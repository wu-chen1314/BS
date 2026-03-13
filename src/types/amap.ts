export type AmapPluginName =
  | "AMap.Driving"
  | "AMap.Walking"
  | "AMap.Transfer"
  | "AMap.Riding"
  | "AMap.ToolBar"
  | "AMap.Scale";

export interface AmapConfig {
  key: string;
  securityJsCode: string;
  version: string;
}

export type AmapAvailabilityStatus = "ready" | "missing-key" | "missing-window";

export interface AmapAvailability {
  available: boolean;
  status: AmapAvailabilityStatus;
  message: string;
  config: AmapConfig;
}

export interface AmapLngLatGetter {
  getLng(): unknown;
  getLat(): unknown;
}

export interface AmapLngLatRecord {
  lng?: unknown;
  lat?: unknown;
  longitude?: unknown;
  latitude?: unknown;
}

export type AmapCoordinateInput = [unknown, unknown] | AmapLngLatGetter | AmapLngLatRecord;

export interface AmapNavigationStepLike {
  instruction?: unknown;
  distance?: unknown;
  dist?: unknown;
  time?: unknown;
  duration?: unknown;
  action?: unknown;
  road?: unknown;
  path?: AmapCoordinateInput[];
}

export interface AmapDrivingRouteLike {
  distance?: unknown;
  time?: unknown;
  duration?: unknown;
  steps?: AmapNavigationStepLike[];
  rides?: AmapNavigationStepLike[];
}

export interface AmapDrivingSearchResult extends AmapDrivingRouteLike {
  routes?: AmapDrivingRouteLike[];
}

export interface AmapTransferBusLineLike {
  name?: unknown;
  end_stop?: unknown;
  distance?: unknown;
  duration?: unknown;
  time?: unknown;
}

export interface AmapTransferRailwayLike {
  name?: unknown;
  distance?: unknown;
  time?: unknown;
}

export interface AmapTransferSegmentLike {
  walking?: {
    steps?: AmapNavigationStepLike[];
  };
  bus?: {
    buslines?: AmapTransferBusLineLike[];
  };
  railway?: AmapTransferRailwayLike;
}

export interface AmapTransferPlanLike {
  distance?: unknown;
  time?: unknown;
  segments?: AmapTransferSegmentLike[];
}

export interface AmapTransferSearchResult extends AmapTransferPlanLike {
  plans?: AmapTransferPlanLike[];
}

export interface AmapNavigationService {
  search(
    origin: [number, number],
    destination: [number, number],
    callback: (status: string, result: unknown) => void
  ): void;
}

export interface AmapPixelInstance {}

export interface AmapMarkerInstance {
  setLabel(options: {
    direction: string;
    offset: AmapPixelInstance;
    content: string;
  }): void;
  setPosition(position: [number, number]): void;
}

export interface AmapPolylineInstance {}

export interface AmapMapInstance {
  addControl(control: unknown): void;
  add(overlays: unknown): void;
  remove(overlays: unknown): void;
  setFitView(overlays: unknown[], immediately?: boolean, padding?: [number, number, number, number]): void;
  destroy(): void;
}

export interface AmapStaticApi {
  plugin(plugins: string[], callback: () => void): void;
  Driving: new (options: Record<string, unknown>) => AmapNavigationService;
  Walking: new (options: Record<string, unknown>) => AmapNavigationService;
  Transfer: new (options: Record<string, unknown>) => AmapNavigationService;
  Riding: new (options: Record<string, unknown>) => AmapNavigationService;
  Map: new (container: HTMLElement, options: Record<string, unknown>) => AmapMapInstance;
  Marker: new (options: Record<string, unknown>) => AmapMarkerInstance;
  Polyline: new (options: Record<string, unknown>) => AmapPolylineInstance;
  Pixel: new (x: number, y: number) => AmapPixelInstance;
  ToolBar: new () => unknown;
  Scale: new () => unknown;
}
