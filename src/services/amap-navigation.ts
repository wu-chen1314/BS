import type {
  AmapCoordinateInput,
  AmapDrivingRouteLike,
  AmapDrivingSearchResult,
  AmapNavigationService,
  AmapPluginName,
  AmapStaticApi,
  AmapTransferPlanLike,
  AmapTransferSearchResult,
} from "@/types/amap";
import type {
  TrailMapCoordinate,
  TrailNavigationInstruction,
  TrailNavigationLeg,
  TrailNavigationRoute,
  TrailNavigationSource,
  TrailStopView,
  TransportMode,
} from "@/types/trail";
import { generateCacheKey, requestCache } from "@/utils/cache";
import { loadAmapSdk } from "@/utils/amap";
import {
  ROUTE_CACHE_TTL,
  buildFallbackInstruction,
  buildTrailRouteCacheKey,
  formatDistance,
  formatDuration,
  formatEta,
  hasNavigableCoordinates,
  haversineDistance,
  toTrailCoordinate,
} from "@/utils/trail-navigation";

const ROUTE_STORAGE_PREFIX = "heritage-trail-route:";
const LIVE_ROUTE_CACHE_TTL = 20 * 1000;

const getSessionRouteCache = (key: string) => {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const raw = window.sessionStorage.getItem(`${ROUTE_STORAGE_PREFIX}${key}`);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as {
      timestamp: number;
      data: TrailNavigationRoute;
    };
    if (Date.now() - parsed.timestamp > ROUTE_CACHE_TTL) {
      window.sessionStorage.removeItem(`${ROUTE_STORAGE_PREFIX}${key}`);
      return null;
    }
    return parsed.data;
  } catch (error) {
    console.warn("Failed to parse session route cache", error);
    return null;
  }
};

const setSessionRouteCache = (key: string, data: TrailNavigationRoute) => {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.sessionStorage.setItem(
      `${ROUTE_STORAGE_PREFIX}${key}`,
      JSON.stringify({
        timestamp: Date.now(),
        data,
      })
    );
  } catch (error) {
    console.warn("Failed to write session route cache", error);
  }
};

const getPlannedRouteCacheKey = (stops: TrailStopView[], transportMode: TransportMode, source: TrailNavigationSource) =>
  `${buildTrailRouteCacheKey(stops, transportMode)}|${source}`;

const getPluginName = (transportMode: TransportMode): AmapPluginName => {
  switch (transportMode) {
    case "car":
      return "AMap.Driving";
    case "walk":
      return "AMap.Walking";
    case "public":
      return "AMap.Transfer";
    default:
      return "AMap.Riding";
  }
};

const createService = (AMap: AmapStaticApi, transportMode: TransportMode): AmapNavigationService => {
  switch (transportMode) {
    case "car":
      return new AMap.Driving({
        policy: 0,
        hideMarkers: true,
      });
    case "walk":
      return new AMap.Walking({
        hideMarkers: true,
      });
    case "public":
      return new AMap.Transfer({
        city: "全国",
        policy: 0,
        nightflag: true,
      });
    default:
      return new AMap.Riding({
        hideMarkers: true,
      });
  }
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null;

const toTuple = (value: unknown): [number, number] | null => {
  if (!value) {
    return null;
  }

  if (Array.isArray(value) && value.length >= 2) {
    const longitude = Number(value[0]);
    const latitude = Number(value[1]);
    return Number.isFinite(longitude) && Number.isFinite(latitude) ? [longitude, latitude] : null;
  }

  if (!isRecord(value)) {
    return null;
  }

  if (typeof value.getLng === "function" && typeof value.getLat === "function") {
    const longitude = Number(value.getLng());
    const latitude = Number(value.getLat());
    return Number.isFinite(longitude) && Number.isFinite(latitude) ? [longitude, latitude] : null;
  }

  if ("lng" in value && "lat" in value) {
    const longitude = Number(value.lng);
    const latitude = Number(value.lat);
    return Number.isFinite(longitude) && Number.isFinite(latitude) ? [longitude, latitude] : null;
  }

  if ("longitude" in value && "latitude" in value) {
    const longitude = Number(value.longitude);
    const latitude = Number(value.latitude);
    return Number.isFinite(longitude) && Number.isFinite(latitude) ? [longitude, latitude] : null;
  }

  return null;
};

const flattenPathInput = (value: unknown): unknown[] => {
  if (!Array.isArray(value)) {
    return [value];
  }
  const looksLikeCoordinateTuple =
    value.length >= 2 &&
    !Array.isArray(value[0]) &&
    !Array.isArray(value[1]) &&
    !isRecord(value[0]) &&
    !isRecord(value[1]);
  if (looksLikeCoordinateTuple) {
    return [value];
  }
  return value.flatMap((item) => flattenPathInput(item));
};

const normalizePolyline = (...paths: unknown[]) => {
  const tuples: Array<[number, number]> = [];
  for (const item of paths.flatMap((value) => flattenPathInput(value))) {
    const tuple = toTuple(item as AmapCoordinateInput);
    if (tuple) {
      const lastTuple = tuples[tuples.length - 1];
      if (!lastTuple || lastTuple[0] !== tuple[0] || lastTuple[1] !== tuple[1]) {
        tuples.push(tuple);
      }
    }
  }
  return tuples;
};

const toNumber = (value: unknown) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const normalizeText = (value: unknown, fallback = "") => String(value || fallback).trim();

const normalizeInstruction = (
  id: string,
  text: string,
  distance = 0,
  duration = 0,
  action?: string,
  road?: string
): TrailNavigationInstruction => ({
  id,
  text,
  distance,
  duration,
  distanceText: formatDistance(distance),
  durationText: formatDuration(duration),
  action,
  road,
});

const parseDrivingLikeResult = (
  fromName: string,
  toName: string,
  route: AmapDrivingRouteLike | AmapDrivingSearchResult | null | undefined,
  transportMode: TransportMode
) => {
  const rawSteps =
    Array.isArray(route?.steps) && route.steps.length
      ? route.steps
      : Array.isArray(route?.rides)
        ? route.rides
        : [];

  const instructions = rawSteps.length
    ? rawSteps.map((step, index) =>
        normalizeInstruction(
          `${fromName}-${toName}-${index}`,
          normalizeText(step.instruction, `从 ${fromName} 前往 ${toName}`),
          toNumber(step.distance ?? step.dist),
          toNumber(step.time ?? step.duration),
          normalizeText(step.action),
          normalizeText(step.road)
        )
      )
    : [
        buildFallbackInstruction(
          fromName,
          toName,
          transportMode,
          toNumber(route?.distance),
          toNumber(route?.time ?? route?.duration)
        ),
      ];

  const polyline = normalizePolyline(rawSteps.map((step) => step.path));

  return {
    distance: toNumber(route?.distance),
    duration: toNumber(route?.time ?? route?.duration),
    instructions,
    polyline,
  };
};

const parseTransferResult = (fromName: string, toName: string, plan: AmapTransferPlanLike | null | undefined) => {
  const instructions: TrailNavigationInstruction[] = [];
  const polyline: Array<[number, number]> = [];
  const segments = Array.isArray(plan?.segments) ? plan.segments : [];

  for (const [segmentIndex, segment] of segments.entries()) {
    const walkingSteps = Array.isArray(segment.walking?.steps) ? segment.walking.steps : [];
    for (const [stepIndex, step] of walkingSteps.entries()) {
      instructions.push(
        normalizeInstruction(
          `${fromName}-${toName}-walk-${segmentIndex}-${stepIndex}`,
          normalizeText(step.instruction, `步行前往 ${toName}`),
          toNumber(step.distance),
          toNumber(step.time),
          normalizeText(step.action),
          normalizeText(step.road)
        )
      );
      polyline.push(...normalizePolyline(step.path));
    }

    const busLines = Array.isArray(segment.bus?.buslines) ? segment.bus.buslines : [];
    for (const [lineIndex, line] of busLines.entries()) {
      instructions.push(
        normalizeInstruction(
          `${fromName}-${toName}-bus-${segmentIndex}-${lineIndex}`,
          `乘坐 ${normalizeText(line.name, "公共交通")} 至 ${normalizeText(line.end_stop, toName)}`,
          toNumber(line.distance),
          toNumber(line.duration ?? line.time),
          "transit",
          normalizeText(line.name)
        )
      );
    }

    if (segment.railway) {
      instructions.push(
        normalizeInstruction(
          `${fromName}-${toName}-rail-${segmentIndex}`,
          `乘坐 ${normalizeText(segment.railway.name, "铁路")} 前往 ${toName}`,
          toNumber(segment.railway.distance),
          toNumber(segment.railway.time),
          "railway",
          normalizeText(segment.railway.name)
        )
      );
    }
  }

  if (!instructions.length) {
    instructions.push(buildFallbackInstruction(fromName, toName, "public", toNumber(plan?.distance), toNumber(plan?.time)));
  }

  return {
    distance: toNumber(plan?.distance),
    duration: toNumber(plan?.time),
    instructions,
    polyline,
  };
};

const pickDrivingResult = (result: unknown): AmapDrivingRouteLike | AmapDrivingSearchResult | null => {
  if (!isRecord(result)) {
    return null;
  }
  if (Array.isArray(result.routes) && result.routes.length && isRecord(result.routes[0])) {
    return result.routes[0] as AmapDrivingRouteLike;
  }
  return result as AmapDrivingSearchResult;
};

const pickTransferPlan = (result: unknown): AmapTransferPlanLike | null => {
  if (!isRecord(result)) {
    return null;
  }
  if (Array.isArray(result.plans) && result.plans.length && isRecord(result.plans[0])) {
    return result.plans[0] as AmapTransferPlanLike;
  }
  return result as AmapTransferSearchResult;
};

const normalizeLegIndexes = (legs: TrailNavigationLeg[]) =>
  legs.map((item, index) => ({
    ...item,
    index,
  }));

const buildRouteFromLegs = (legs: TrailNavigationLeg[], transportMode: TransportMode, source: TrailNavigationSource) => {
  const normalizedLegs = normalizeLegIndexes(legs);
  const distance = normalizedLegs.reduce((sum, item) => sum + item.distance, 0);
  const duration = normalizedLegs.reduce((sum, item) => sum + item.duration, 0);

  return {
    source,
    transportMode,
    distance,
    duration,
    distanceText: formatDistance(distance),
    durationText: formatDuration(duration),
    etaText: formatEta(duration),
    legs: normalizedLegs,
    polyline: normalizedLegs.flatMap((item, index) => (index === 0 ? item.polyline : item.polyline.slice(1))),
    plannedAt: Date.now(),
  } satisfies TrailNavigationRoute;
};

const buildApproximateLeg = (
  origin: TrailMapCoordinate,
  destination: TrailMapCoordinate,
  transportMode: TransportMode,
  fromName: string,
  toName: string
): TrailNavigationLeg => {
  const averageSpeedMap: Record<TransportMode, number> = {
    walk: 1.3,
    public: 6.5,
    car: 14,
  };
  const distance = haversineDistance(origin, destination);
  const duration = Math.max(120, Math.round(distance / (averageSpeedMap[transportMode] || 4.5)));
  const instruction = buildFallbackInstruction(fromName, toName, transportMode, distance, duration);

  return {
    id: `${fromName}-${toName}-${transportMode}-fallback`,
    index: 0,
    source: "approximate",
    fromName,
    toName,
    from: origin,
    to: destination,
    distance,
    duration,
    distanceText: formatDistance(distance),
    durationText: formatDuration(duration),
    instructionPreview: instruction.text,
    instructions: [instruction],
    polyline: [
      [origin.longitude, origin.latitude],
      [destination.longitude, destination.latitude],
    ],
  };
};

const cacheRoute = (key: string, route: TrailNavigationRoute) => {
  requestCache.set(key, route, ROUTE_CACHE_TTL);
  setSessionRouteCache(key, route);
  return route;
};

const buildLiveLegCacheKey = (
  source: TrailNavigationSource,
  options: {
    origin: TrailMapCoordinate;
    destination: TrailStopView;
    transportMode: TransportMode;
  }
) =>
  generateCacheKey(`trail-live-leg:${source}`, {
    transportMode: options.transportMode,
    origin: `${options.origin.longitude.toFixed(6)},${options.origin.latitude.toFixed(6)}`,
    destination: `${Number(options.destination.project.id || 0)}:${Number(options.destination.project.longitude || 0).toFixed(
      6
    )},${Number(options.destination.project.latitude || 0).toFixed(6)}`,
  });

const searchLeg = async (
  AMap: AmapStaticApi,
  origin: TrailMapCoordinate,
  destination: TrailMapCoordinate,
  transportMode: TransportMode,
  fromName: string,
  toName: string
) => {
  const service = createService(AMap, transportMode);
  return new Promise<TrailNavigationLeg>((resolve) => {
    service.search([origin.longitude, origin.latitude], [destination.longitude, destination.latitude], (status, result) => {
      if (status !== "complete") {
        resolve(buildApproximateLeg(origin, destination, transportMode, fromName, toName));
        return;
      }

      const parsed =
        transportMode === "public"
          ? parseTransferResult(fromName, toName, pickTransferPlan(result))
          : parseDrivingLikeResult(fromName, toName, pickDrivingResult(result), transportMode);

      resolve({
        id: `${fromName}-${toName}-${transportMode}`,
        index: 0,
        source: "amap",
        fromName,
        toName,
        from: origin,
        to: destination,
        distance: parsed.distance,
        duration: parsed.duration,
        distanceText: formatDistance(parsed.distance),
        durationText: formatDuration(parsed.duration),
        instructionPreview: parsed.instructions[0]?.text || `从 ${fromName} 前往 ${toName}`,
        instructions: parsed.instructions,
        polyline: parsed.polyline.length
          ? parsed.polyline
          : [
              [origin.longitude, origin.latitude],
              [destination.longitude, destination.latitude],
            ],
      });
    });
  });
};

export const planApproximateTrailNavigation = (stops: TrailStopView[], transportMode: TransportMode) => {
  if (!hasNavigableCoordinates(stops) || stops.length < 2) {
    throw new Error("Stops do not have enough coordinates for navigation");
  }

  const cacheKey = getPlannedRouteCacheKey(stops, transportMode, "approximate");
  const memoized = requestCache.get<TrailNavigationRoute>(cacheKey, ROUTE_CACHE_TTL) || getSessionRouteCache(cacheKey);
  if (memoized) {
    return memoized;
  }

  const legs = stops.slice(1).map((stop, index) =>
    buildApproximateLeg(
      toTrailCoordinate(stops[index]),
      toTrailCoordinate(stop),
      transportMode,
      stops[index].project.name,
      stop.project.name
    )
  );

  return cacheRoute(cacheKey, buildRouteFromLegs(legs, transportMode, "approximate"));
};

export const planTrailNavigation = async (stops: TrailStopView[], transportMode: TransportMode) => {
  if (!hasNavigableCoordinates(stops) || stops.length < 2) {
    throw new Error("Stops do not have enough coordinates for navigation");
  }

  const cacheKey = getPlannedRouteCacheKey(stops, transportMode, "amap");
  const memoized = requestCache.get<TrailNavigationRoute>(cacheKey, ROUTE_CACHE_TTL) || getSessionRouteCache(cacheKey);
  if (memoized) {
    return memoized;
  }

  const AMap = await loadAmapSdk([getPluginName(transportMode)]);
  const legs = await Promise.all(
    stops.slice(1).map((stop, index) =>
      searchLeg(
        AMap,
        toTrailCoordinate(stops[index]),
        toTrailCoordinate(stop),
        transportMode,
        stops[index].project.name,
        stop.project.name
      )
    )
  );

  return cacheRoute(cacheKey, buildRouteFromLegs(legs, transportMode, "amap"));
};

export const planApproximateLiveLegNavigation = (options: {
  origin: TrailMapCoordinate;
  destination: TrailStopView;
  transportMode: TransportMode;
  fromName?: string;
}) => {
  const cacheKey = buildLiveLegCacheKey("approximate", options);
  const memoized = requestCache.get<TrailNavigationLeg>(cacheKey, LIVE_ROUTE_CACHE_TTL);
  if (memoized) {
    return memoized;
  }

  const leg = buildApproximateLeg(
    options.origin,
    toTrailCoordinate(options.destination),
    options.transportMode,
    options.fromName || "当前位置",
    options.destination.project.name
  );
  requestCache.set(cacheKey, leg, LIVE_ROUTE_CACHE_TTL);
  return leg;
};

export const planLiveLegNavigation = async (options: {
  origin: TrailMapCoordinate;
  destination: TrailStopView;
  transportMode: TransportMode;
  fromName?: string;
}) => {
  const cacheKey = buildLiveLegCacheKey("amap", options);
  const memoized = requestCache.get<TrailNavigationLeg>(cacheKey, LIVE_ROUTE_CACHE_TTL);
  if (memoized) {
    return memoized;
  }

  const AMap = await loadAmapSdk([getPluginName(options.transportMode)]);
  const leg = await searchLeg(
    AMap,
    options.origin,
    toTrailCoordinate(options.destination),
    options.transportMode,
    options.fromName || "当前位置",
    options.destination.project.name
  );
  requestCache.set(cacheKey, leg, LIVE_ROUTE_CACHE_TTL);
  return leg;
};
