import request from "@/utils/request";
import type { BudgetLevel, DurationKey, RouteMode, TrailAnalyticsSummary, TrailFavoriteRecord, TransportMode } from "@/types/trail";

export interface TrailFavoritePayload {
  trailId: string;
  trailTitle: string;
  routeType: RouteMode;
}

export interface TrailAnalyticsPayload {
  trailId: string;
  trailTitle: string;
  routeType: RouteMode;
  actionType: "view" | "share" | "customize" | "export";
  transportMode: TransportMode;
  budgetLevel: BudgetLevel;
  durationKey: DurationKey;
  interests: string[];
  stopCount: number;
  estimatedHours: number;
  estimatedCost: number;
  payload?: Record<string, unknown>;
}

export const toggleTrailFavorite = (payload: TrailFavoritePayload) =>
  request.post("/heritage-trails/favorites/toggle", payload);

export const checkTrailFavorite = (trailId: string, routeType: RouteMode) =>
  request.get("/heritage-trails/favorites/check", {
    params: { trailId, routeType },
  });

export const fetchTrailFavorites = () =>
  request.get<{ code: number; data: TrailFavoriteRecord[] }>("/heritage-trails/favorites/list");

export const recordTrailAnalytics = (payload: TrailAnalyticsPayload) =>
  request.post("/heritage-trails/analytics/record", payload);

export const fetchTrailAnalyticsSummary = (days = 30) =>
  request.get<{ code: number; data: TrailAnalyticsSummary }>("/heritage-trails/analytics/summary", {
    params: { days },
  });
