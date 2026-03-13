import request from "@/utils/request";
import type { LearningPlanAnalyticsSummary, LearningPlanFavoriteRecord } from "@/types/learning";

export interface LearningPlanFavoritePayload {
  planId: string;
  planTitle: string;
  trackId: string;
}

export interface LearningPlanAnalyticsPayload {
  planId: string;
  planTitle: string;
  trackId: string;
  actionType: "view" | "routeCarry" | "export" | "teacherSheet";
  audienceTag: string;
  durationLabel: string;
  linkedThemeId: string;
  regionKeyword: string;
  projectCount: number;
  keywords: string[];
  payload?: Record<string, unknown>;
}

export const toggleLearningPlanFavorite = (payload: LearningPlanFavoritePayload) =>
  request.post("/learning-studio/favorites/toggle", payload);

export const checkLearningPlanFavorite = (planId: string) =>
  request.get<{ code: number; data: boolean }>("/learning-studio/favorites/check", {
    params: { planId },
  });

export const fetchLearningPlanFavorites = () =>
  request.get<{ code: number; data: LearningPlanFavoriteRecord[] }>("/learning-studio/favorites/list");

export const recordLearningPlanAnalytics = (payload: LearningPlanAnalyticsPayload) =>
  request.post("/learning-studio/analytics/record", payload);

export const fetchLearningPlanAnalyticsSummary = (days = 30) =>
  request.get<{ code: number; data: LearningPlanAnalyticsSummary }>("/learning-studio/analytics/summary", {
    params: { days },
  });
