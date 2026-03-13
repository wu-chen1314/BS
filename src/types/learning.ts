import type { HeritageProject, StatisticItem } from "@/types/project";

export interface LearningPlanModule {
  title: string;
  duration: string;
  objective: string;
  activities: string[];
  outputs: string[];
}

export interface LearningTeacherAgendaItem {
  title: string;
  duration: string;
  objective: string;
  teacherTasks: string[];
  studentTasks: string[];
}

export interface LearningTeacherSheet {
  title: string;
  summary: string;
  targetAudience: string;
  suggestedDuration: string;
  prepChecklist: string[];
  materials: string[];
  discussionPrompts: string[];
  assessmentPoints: string[];
  agenda: LearningTeacherAgendaItem[];
}

export interface LearningPlanViewModel {
  id: string;
  trackId: string;
  title: string;
  subtitle: string;
  audience: string;
  duration: string;
  goal: string;
  keywords: string[];
  deliverables: string[];
  linkedThemeId: string;
  linkedThemeTitle: string;
  linkedThemeSubtitle: string;
  highlightedRegion: string;
  projectCount: number;
  projects: HeritageProject[];
  modules: LearningPlanModule[];
  teacherSheet: LearningTeacherSheet;
}

export interface LearningPlanFavoriteRecord {
  id: number;
  planId: string;
  planTitle?: string | null;
  trackId?: string | null;
  createdAt?: string | null;
}

export interface LearningPlanStatisticItem extends StatisticItem {
  planId?: string | null;
  planTitle?: string | null;
  trackId?: string | null;
}

export interface LearningPlanAnalyticsSummary {
  favoriteCount: number;
  viewCount: number;
  routeCarryCount: number;
  exportCount: number;
  sheetExportCount: number;
  topPlans: LearningPlanStatisticItem[];
  trackPreferences: LearningPlanStatisticItem[];
  audiencePreferences: LearningPlanStatisticItem[];
  themePreferences: LearningPlanStatisticItem[];
  keywordPreferences: LearningPlanStatisticItem[];
  actionSummary: LearningPlanStatisticItem[];
}
