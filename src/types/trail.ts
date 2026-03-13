import type { HeritageProject } from "@/types/project";

export type RouteMode = "template" | "custom";
export type TransportMode = "walk" | "public" | "car";
export type BudgetLevel = "low" | "medium" | "high";
export type DurationKey = "2h" | "4h" | "6h" | "day";

export interface TrailNote {
  title: string;
  content: string;
}

export interface TrailTemplateConfig {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  categoryIds: number[];
  durationKey: DurationKey;
  budgetLevel: BudgetLevel;
  transportMode: TransportMode;
  season: string;
  audience: string;
  highlight: string;
  keywords: string[];
  notes: TrailNote[];
}

export interface TrailInheritorProfile {
  id: number;
  name: string;
  level?: string | null;
  description?: string | null;
  avatarUrl?: string | null;
  sex?: string | null;
  projectId?: number | null;
  address?: string | null;
  longitude?: number | null;
  latitude?: number | null;
}

export interface TrailPlannerForm {
  interestIds: number[];
  durationKey: DurationKey;
  budgetLevel: BudgetLevel;
  transportMode: TransportMode;
  regionKeyword: string;
  maxStops: number;
  preferHot: boolean;
}

export interface TrailStopView {
  project: HeritageProject;
  inheritors: TrailInheritorProfile[];
  visitDurationHours: number;
  visitDurationLabel: string;
  transferTip: string;
  estimatedSpend: number;
  facilityTags: string[];
  supportTips: string[];
  historicalContext: string;
}

export interface TrailViewModel {
  id: string;
  routeType: RouteMode;
  title: string;
  subtitle: string;
  description: string;
  durationLabel: string;
  transportLabel: string;
  budgetLabel: string;
  estimatedHours: number;
  estimatedCost: number;
  stopCount: number;
  keywords: string[];
  notes: TrailNote[];
  stops: TrailStopView[];
}

export interface TrailFavoriteRecord {
  id: number;
  trailId: string;
  trailTitle?: string | null;
  routeType: RouteMode | string;
  createdAt?: string | null;
}

export interface TrailStatisticItem {
  name?: string;
  trailId?: string | null;
  trailTitle?: string | null;
  routeType?: string | null;
  value: number;
}

export interface TrailAnalyticsSummary {
  favoriteCount: number;
  viewCount: number;
  customizationCount: number;
  shareCount: number;
  exportCount: number;
  topTrails: TrailStatisticItem[];
  transportPreferences: TrailStatisticItem[];
  budgetPreferences: TrailStatisticItem[];
  durationPreferences: TrailStatisticItem[];
  interestPreferences: TrailStatisticItem[];
  actionSummary: TrailStatisticItem[];
}

export interface TrailMapCoordinate {
  longitude: number;
  latitude: number;
}

export interface TrailNavigationInstruction {
  id: string;
  text: string;
  road?: string;
  action?: string;
  distance: number;
  duration: number;
  distanceText: string;
  durationText: string;
}

export type TrailNavigationSource = "amap" | "approximate";

export interface TrailNavigationLeg {
  id: string;
  index: number;
  source: TrailNavigationSource;
  fromName: string;
  toName: string;
  from: TrailMapCoordinate;
  to: TrailMapCoordinate;
  distance: number;
  duration: number;
  distanceText: string;
  durationText: string;
  instructionPreview: string;
  instructions: TrailNavigationInstruction[];
  polyline: Array<[number, number]>;
}

export interface TrailNavigationRoute {
  source: TrailNavigationSource;
  transportMode: TransportMode;
  distance: number;
  duration: number;
  distanceText: string;
  durationText: string;
  etaText: string;
  legs: TrailNavigationLeg[];
  polyline: Array<[number, number]>;
  plannedAt: number;
}

export interface TrailLiveGuidance {
  activeLegIndex: number;
  destinationName: string;
  distance: number;
  duration: number;
  distanceText: string;
  durationText: string;
  etaText: string;
  instructions: TrailNavigationInstruction[];
}
