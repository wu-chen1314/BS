import type { TrailStopView } from "@/types/trail";

interface MapPoint {
  x: number;
  y: number;
}

const isFiniteCoordinate = (value: unknown) =>
  value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value));

const REGION_ANCHORS: Array<{ keywords: string[]; point: MapPoint }> = [
  { keywords: ["北京"], point: { x: 78, y: 22 } },
  { keywords: ["天津"], point: { x: 80, y: 25 } },
  { keywords: ["河北"], point: { x: 74, y: 28 } },
  { keywords: ["山西"], point: { x: 68, y: 29 } },
  { keywords: ["内蒙古"], point: { x: 63, y: 15 } },
  { keywords: ["辽宁"], point: { x: 86, y: 20 } },
  { keywords: ["吉林"], point: { x: 91, y: 16 } },
  { keywords: ["黑龙江"], point: { x: 95, y: 9 } },
  { keywords: ["上海"], point: { x: 85, y: 47 } },
  { keywords: ["江苏"], point: { x: 82, y: 41 } },
  { keywords: ["浙江"], point: { x: 87, y: 49 } },
  { keywords: ["安徽"], point: { x: 77, y: 44 } },
  { keywords: ["福建"], point: { x: 85, y: 58 } },
  { keywords: ["江西"], point: { x: 77, y: 53 } },
  { keywords: ["山东"], point: { x: 82, y: 31 } },
  { keywords: ["河南"], point: { x: 72, y: 37 } },
  { keywords: ["湖北"], point: { x: 68, y: 47 } },
  { keywords: ["湖南"], point: { x: 66, y: 54 } },
  { keywords: ["广东"], point: { x: 74, y: 66 } },
  { keywords: ["广西"], point: { x: 65, y: 66 } },
  { keywords: ["海南"], point: { x: 68, y: 82 } },
  { keywords: ["重庆"], point: { x: 57, y: 48 } },
  { keywords: ["四川"], point: { x: 49, y: 46 } },
  { keywords: ["贵州"], point: { x: 53, y: 57 } },
  { keywords: ["云南"], point: { x: 42, y: 63 } },
  { keywords: ["西藏"], point: { x: 20, y: 42 } },
  { keywords: ["陕西"], point: { x: 58, y: 36 } },
  { keywords: ["甘肃"], point: { x: 44, y: 28 } },
  { keywords: ["青海"], point: { x: 34, y: 31 } },
  { keywords: ["宁夏"], point: { x: 52, y: 26 } },
  { keywords: ["新疆"], point: { x: 10, y: 20 } },
  { keywords: ["香港"], point: { x: 77, y: 72 } },
  { keywords: ["澳门"], point: { x: 75, y: 73 } },
  { keywords: ["台湾"], point: { x: 92, y: 62 } },
];

export interface TrailMapNode {
  id: number;
  x: number;
  y: number;
  name: string;
  regionName: string;
  categoryName?: string | null;
  durationLabel: string;
  longitude?: number | null;
  latitude?: number | null;
}

export interface TrailMapSegment {
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
}

export interface TrailMapData {
  mode: "geographic" | "schematic";
  nodes: TrailMapNode[];
  segments: TrailMapSegment[];
}

const fallbackPoint = (index: number): MapPoint => ({
  x: 18 + (index % 5) * 15,
  y: 20 + Math.floor(index / 5) * 12,
});

const resolveAnchor = (regionName?: string | null, index = 0): MapPoint => {
  const target = regionName || "";
  const matched = REGION_ANCHORS.find((item) => item.keywords.some((keyword) => target.includes(keyword)));
  return matched?.point || fallbackPoint(index);
};

const hasCompleteCoordinates = (stops: TrailStopView[]) =>
  stops.length > 0 &&
  stops.every((stop) => isFiniteCoordinate(stop.project.longitude) && isFiniteCoordinate(stop.project.latitude));

const buildSegments = (nodes: TrailMapNode[]) => {
  const segments: TrailMapSegment[] = [];
  for (let index = 1; index < nodes.length; index += 1) {
    segments.push({
      fromX: nodes[index - 1].x,
      fromY: nodes[index - 1].y,
      toX: nodes[index].x,
      toY: nodes[index].y,
    });
  }
  return segments;
};

const buildSchematicNodes = (stops: TrailStopView[]) =>
  stops.map((stop, index) => {
    const point = resolveAnchor(stop.project.regionName, index);
    return {
      id: Number(stop.project.id),
      x: point.x,
      y: point.y,
      name: stop.project.name,
      regionName: stop.project.regionName || "地区待补充",
      categoryName: stop.project.categoryName || null,
      durationLabel: stop.visitDurationLabel,
      longitude: isFiniteCoordinate(stop.project.longitude) ? Number(stop.project.longitude) : null,
      latitude: isFiniteCoordinate(stop.project.latitude) ? Number(stop.project.latitude) : null,
    } satisfies TrailMapNode;
  });

const buildGeographicNodes = (stops: TrailStopView[]) => {
  const longitudes = stops.map((stop) => Number(stop.project.longitude));
  const latitudes = stops.map((stop) => Number(stop.project.latitude));
  const minLongitude = Math.min(...longitudes);
  const maxLongitude = Math.max(...longitudes);
  const minLatitude = Math.min(...latitudes);
  const maxLatitude = Math.max(...latitudes);

  const xPadding = 14;
  const yPadding = 16;
  const width = 72;
  const height = 62;
  const longitudeRange = Math.max(maxLongitude - minLongitude, 0.0001);
  const latitudeRange = Math.max(maxLatitude - minLatitude, 0.0001);

  return stops.map((stop) => {
    const longitude = Number(stop.project.longitude);
    const latitude = Number(stop.project.latitude);
    return {
      id: Number(stop.project.id),
      x: xPadding + ((longitude - minLongitude) / longitudeRange) * width,
      y: yPadding + (1 - (latitude - minLatitude) / latitudeRange) * height,
      name: stop.project.name,
      regionName: stop.project.regionName || "地区待补充",
      categoryName: stop.project.categoryName || null,
      durationLabel: stop.visitDurationLabel,
      longitude,
      latitude,
    } satisfies TrailMapNode;
  });
};

export const buildTrailMap = (stops: TrailStopView[]): TrailMapData => {
  const nodes = hasCompleteCoordinates(stops) ? buildGeographicNodes(stops) : buildSchematicNodes(stops);
  return {
    mode: hasCompleteCoordinates(stops) ? "geographic" : "schematic",
    nodes,
    segments: buildSegments(nodes),
  };
};
