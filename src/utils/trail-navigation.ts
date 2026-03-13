import type {
  TrailLiveGuidance,
  TrailMapCoordinate,
  TrailNavigationInstruction,
  TrailNavigationLeg,
  TrailNavigationRoute,
  TrailStopView,
  TransportMode,
} from "@/types/trail";

export const ROUTE_CACHE_TTL = 15 * 60 * 1000;
const EARTH_RADIUS_METERS = 6371000;
const isFiniteCoordinate = (value: unknown) =>
  value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value));

export const hasNavigableCoordinates = (stops: TrailStopView[]) =>
  stops.length > 0 &&
  stops.every(
    (stop) =>
      isFiniteCoordinate(stop.project.longitude) &&
      isFiniteCoordinate(stop.project.latitude)
  );

export const toTrailCoordinate = (
  value:
    | TrailStopView
    | TrailMapCoordinate
    | {
        longitude?: number | null;
        latitude?: number | null;
      }
): TrailMapCoordinate => ({
  longitude: Number("project" in value ? value.project.longitude : value.longitude),
  latitude: Number("project" in value ? value.project.latitude : value.latitude),
});

export const formatDistance = (distance: number) =>
  distance >= 1000 ? `${(distance / 1000).toFixed(distance >= 10000 ? 0 : 1)} km` : `${Math.round(distance)} m`;

export const formatDuration = (duration: number) => {
  const minutes = Math.max(1, Math.round(duration / 60));
  if (minutes < 60) {
    return `${minutes} 分钟`;
  }

  const hours = Math.floor(minutes / 60);
  const remainMinutes = minutes % 60;
  return remainMinutes ? `${hours} 小时 ${remainMinutes} 分钟` : `${hours} 小时`;
};

export const formatEta = (duration: number, base = new Date()) => {
  const eta = new Date(base.getTime() + duration * 1000);
  return eta.toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
  });
};

export const buildTrailRouteCacheKey = (
  stops: TrailStopView[],
  transportMode: TransportMode,
  origin?: TrailMapCoordinate | null
) =>
  [
    "trail-route",
    transportMode,
    origin ? `${origin.longitude.toFixed(6)},${origin.latitude.toFixed(6)}` : "planned",
    ...stops.map((stop) =>
      [
        Number(stop.project.id || 0),
        Number(stop.project.longitude || 0).toFixed(6),
        Number(stop.project.latitude || 0).toFixed(6),
      ].join(":")
    ),
  ].join("|");

export const haversineDistance = (from: TrailMapCoordinate, to: TrailMapCoordinate) => {
  const toRadians = (value: number) => (value * Math.PI) / 180;
  const deltaLatitude = toRadians(to.latitude - from.latitude);
  const deltaLongitude = toRadians(to.longitude - from.longitude);
  const latitudeOne = toRadians(from.latitude);
  const latitudeTwo = toRadians(to.latitude);
  const a =
    Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2) +
    Math.cos(latitudeOne) * Math.cos(latitudeTwo) * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
};

export const getRouteBounds = (coordinates: TrailMapCoordinate[]) => {
  const longitudes = coordinates.map((item) => item.longitude);
  const latitudes = coordinates.map((item) => item.latitude);
  return {
    southWest: [Math.min(...longitudes), Math.min(...latitudes)] as [number, number],
    northEast: [Math.max(...longitudes), Math.max(...latitudes)] as [number, number],
  };
};

export const resolveNextLegIndex = (
  route: TrailNavigationRoute,
  currentPosition: TrailMapCoordinate,
  currentLegIndex: number,
  arrivalThreshold = 200
) => {
  const safeIndex = Math.max(0, Math.min(currentLegIndex, route.legs.length - 1));
  const leg = route.legs[safeIndex];
  if (!leg) {
    return 0;
  }

  const distanceToDestination = haversineDistance(currentPosition, leg.to);
  if (distanceToDestination <= arrivalThreshold && safeIndex < route.legs.length - 1) {
    return safeIndex + 1;
  }
  return safeIndex;
};

export const resolveNextStopIndex = (
  stops: TrailStopView[],
  currentPosition: TrailMapCoordinate,
  currentStopIndex: number,
  arrivalThreshold = 200
) => {
  const safeIndex = Math.max(0, Math.min(currentStopIndex, stops.length - 1));
  const targetStop = stops[safeIndex];
  if (!targetStop) {
    return 0;
  }

  const distanceToTarget = haversineDistance(currentPosition, toTrailCoordinate(targetStop));
  if (distanceToTarget <= arrivalThreshold && safeIndex < stops.length - 1) {
    return safeIndex + 1;
  }
  return safeIndex;
};

export const buildLiveGuidance = (
  route: TrailNavigationRoute,
  currentLegIndex: number,
  liveLeg?: TrailNavigationLeg | null
): TrailLiveGuidance => {
  const safeIndex = Math.max(0, Math.min(currentLegIndex, route.legs.length - 1));
  const activeLeg = liveLeg || route.legs[safeIndex];
  const trailingLegs = route.legs.slice(safeIndex + 1);
  const remainingDistance =
    activeLeg.distance + trailingLegs.reduce((sum, item) => sum + Number(item.distance || 0), 0);
  const remainingDuration =
    activeLeg.duration + trailingLegs.reduce((sum, item) => sum + Number(item.duration || 0), 0);

  return {
    activeLegIndex: safeIndex,
    destinationName: activeLeg.toName,
    distance: remainingDistance,
    duration: remainingDuration,
    distanceText: formatDistance(remainingDistance),
    durationText: formatDuration(remainingDuration),
    etaText: formatEta(remainingDuration),
    instructions: activeLeg.instructions,
  };
};

export const buildFallbackInstruction = (
  fromName: string,
  toName: string,
  transportMode: TransportMode,
  distance = 0,
  duration = 0
): TrailNavigationInstruction => ({
  id: `${fromName}-${toName}-${transportMode}`,
  text: `从 ${fromName} 前往 ${toName}，建议使用${transportMode === "car" ? "驾车" : transportMode === "walk" ? "步行" : transportMode === "public" ? "公共交通" : "骑行"}模式导航。`,
  distance,
  duration,
  distanceText: formatDistance(distance),
  durationText: formatDuration(duration),
});
