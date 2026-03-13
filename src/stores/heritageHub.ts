import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchLearningPlanAnalyticsSummary, fetchLearningPlanFavorites } from "@/services/learning";
import {
  fetchFavoriteList,
  fetchHotProjects,
  fetchProjectPage,
  fetchSearchHistory,
  fetchStatisticsMap,
} from "@/services/project";
import { fetchTrailAnalyticsSummary, fetchTrailFavorites } from "@/services/trail";
import type {
  LearningPlanAnalyticsSummary,
  LearningPlanFavoriteRecord,
} from "@/types/learning";
import type { HeritageProject, HotProjectItem, StatisticItem } from "@/types/project";
import type { TrailAnalyticsSummary, TrailFavoriteRecord } from "@/types/trail";
import { requestCache } from "@/utils/cache";

interface OverviewSnapshot {
  projects: HeritageProject[];
  hotProjects: HotProjectItem[];
  regionStats: StatisticItem[];
  fetchedAt: number;
}

interface OverviewOptions {
  hotLimit?: number;
  pageSize?: number;
  force?: boolean;
}

const OVERVIEW_CACHE_KEY = "heritage-overview:snapshot";
const OVERVIEW_STORAGE_KEY = "heritage-overview:snapshot";
const OVERVIEW_TTL = 5 * 60 * 1000;
const MODULE_TTL = 60 * 1000;
const HOT_PROJECT_LIMIT = 20;
const hasWindow = typeof window !== "undefined";

let inflightOverviewPromise: Promise<OverviewSnapshot> | null = null;

const createEmptyTrailAnalytics = (): TrailAnalyticsSummary => ({
  favoriteCount: 0,
  viewCount: 0,
  customizationCount: 0,
  shareCount: 0,
  exportCount: 0,
  topTrails: [],
  transportPreferences: [],
  budgetPreferences: [],
  durationPreferences: [],
  interestPreferences: [],
  actionSummary: [],
});

const createEmptyLearningAnalytics = (): LearningPlanAnalyticsSummary => ({
  favoriteCount: 0,
  viewCount: 0,
  routeCarryCount: 0,
  exportCount: 0,
  sheetExportCount: 0,
  topPlans: [],
  trackPreferences: [],
  audiencePreferences: [],
  themePreferences: [],
  keywordPreferences: [],
  actionSummary: [],
});

const readOverviewStorage = () => {
  if (!hasWindow) {
    return null;
  }

  try {
    const raw = window.sessionStorage.getItem(OVERVIEW_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as OverviewSnapshot;
    if (Date.now() - parsed.fetchedAt > OVERVIEW_TTL) {
      window.sessionStorage.removeItem(OVERVIEW_STORAGE_KEY);
      return null;
    }

    return parsed;
  } catch (error) {
    console.error("Failed to restore overview cache", error);
    return null;
  }
};

const writeOverviewStorage = (snapshot: OverviewSnapshot) => {
  if (!hasWindow) {
    return;
  }

  try {
    window.sessionStorage.setItem(OVERVIEW_STORAGE_KEY, JSON.stringify(snapshot));
  } catch (error) {
    console.error("Failed to persist overview cache", error);
  }
};

const updateProjectList = <T extends { id?: number | null }>(
  source: T[],
  projectId: number,
  patch: Partial<T>
) => source.map((item) => (Number(item.id) === projectId ? { ...item, ...patch } : item));

const sortHotProjectItems = (items: HotProjectItem[]) =>
  [...items].sort((left, right) => Number(right.viewCount || 0) - Number(left.viewCount || 0));

const fetchOverviewProjects = async (pageSize: number) => {
  const records: HeritageProject[] = [];
  let pageNum = 1;
  let total = 0;

  do {
    const res = await fetchProjectPage({
      pageNum,
      pageSize,
    });
    const page = res.data.data || {};
    const pageRecords = (page.records || []) as HeritageProject[];
    records.push(...pageRecords);
    total = Number(page.total || records.length);
    pageNum += 1;
  } while (records.length < total && pageNum <= 50);

  return Array.from(
    new Map(records.map((item) => [Number(item.id || 0), item] as const)).values()
  ).filter((item) => Number(item.id || 0) > 0);
};

export const useHeritageHubStore = defineStore("heritageHub", () => {
  const overviewLoading = ref(false);
  const overviewErrorMessage = ref("");
  const projects = ref<HeritageProject[]>([]);
  const hotProjects = ref<HotProjectItem[]>([]);
  const regionStats = ref<StatisticItem[]>([]);

  const projectFavoriteIds = ref<number[]>([]);
  const favoriteProjects = ref<HeritageProject[]>([]);
  const favoriteProjectsTotal = ref(0);
  const favoriteProjectsPage = ref(1);
  const favoriteProjectsPageSize = ref(8);
  const favoriteProjectsLoaded = ref(false);

  const searchHistory = ref<string[]>([]);
  const searchHistoryLoaded = ref(false);

  const trailFavorites = ref<TrailFavoriteRecord[]>([]);
  const trailAnalyticsSummary = ref<TrailAnalyticsSummary>(createEmptyTrailAnalytics());
  const trailStateLoaded = ref(false);

  const learningFavorites = ref<LearningPlanFavoriteRecord[]>([]);
  const learningAnalyticsSummary = ref<LearningPlanAnalyticsSummary>(createEmptyLearningAnalytics());
  const learningStateLoaded = ref(false);

  const applyOverviewSnapshot = (snapshot: OverviewSnapshot) => {
    projects.value = snapshot.projects || [];
    hotProjects.value = snapshot.hotProjects || [];
    regionStats.value = snapshot.regionStats || [];
  };

  const fetchOverviewSnapshot = async ({
    hotLimit = 8,
    pageSize = 60,
  }: OverviewOptions = {}): Promise<OverviewSnapshot> => {
    const [projectRes, hotRes, mapRes] = await Promise.allSettled([
      fetchOverviewProjects(pageSize),
      fetchHotProjects(hotLimit),
      fetchStatisticsMap(),
    ]);

    const projectPayload = projectRes.status === "fulfilled" ? projectRes.value : null;

    if (!projectPayload) {
      throw new Error("项目概览加载失败");
    }

    const snapshot: OverviewSnapshot = {
      projects: projectPayload,
      hotProjects:
        hotRes.status === "fulfilled"
          ? hotRes.value.data.data || []
          : [],
      regionStats:
        mapRes.status === "fulfilled"
          ? mapRes.value.data.data || []
          : [],
      fetchedAt: Date.now(),
    };

    requestCache.set(OVERVIEW_CACHE_KEY, snapshot, OVERVIEW_TTL);
    writeOverviewStorage(snapshot);
    return snapshot;
  };

  const ensureOverview = async (options: OverviewOptions = {}) => {
    const { force = false } = options;

    if (!force) {
      const memoryCache = requestCache.get<OverviewSnapshot>(OVERVIEW_CACHE_KEY, OVERVIEW_TTL);
      const storageCache = memoryCache || readOverviewStorage();
      if (storageCache) {
        applyOverviewSnapshot(storageCache);
        return storageCache;
      }
    }

    if (!inflightOverviewPromise) {
      inflightOverviewPromise = fetchOverviewSnapshot(options).finally(() => {
        inflightOverviewPromise = null;
      });
    }

    overviewLoading.value = true;
    overviewErrorMessage.value = "";
    try {
      const snapshot = await inflightOverviewPromise;
      applyOverviewSnapshot(snapshot);
      return snapshot;
    } catch (error: any) {
      overviewErrorMessage.value = error?.message || "项目概览加载失败";
      throw error;
    } finally {
      overviewLoading.value = false;
    }
  };

  const refreshOverview = async (options: Omit<OverviewOptions, "force"> = {}) =>
    ensureOverview({ ...options, force: true });

  const invalidateOverviewCache = () => {
    requestCache.delete(OVERVIEW_CACHE_KEY);
    if (hasWindow) {
      window.sessionStorage.removeItem(OVERVIEW_STORAGE_KEY);
    }
  };

  const mergeProject = (project: HeritageProject) => {
    if (!project?.id) {
      return;
    }

    const projectId = Number(project.id);
    projects.value = updateProjectList(projects.value, projectId, project);
    favoriteProjects.value = updateProjectList(favoriteProjects.value, projectId, project);
    hotProjects.value = updateProjectList(
      hotProjects.value,
      projectId,
      project as Partial<HotProjectItem>
    );
  };

  const removeProject = (projectId: number) => {
    const existedInFavorites = favoriteProjects.value.some((item) => Number(item.id) === projectId);
    projects.value = projects.value.filter((item) => Number(item.id) !== projectId);
    favoriteProjects.value = favoriteProjects.value.filter((item) => Number(item.id) !== projectId);
    hotProjects.value = hotProjects.value.filter((item) => Number(item.id) !== projectId);
    projectFavoriteIds.value = projectFavoriteIds.value.filter((item) => item !== projectId);
    if (favoriteProjectsLoaded.value && existedInFavorites) {
      favoriteProjectsTotal.value = Math.max(0, favoriteProjectsTotal.value - 1);
    }
    invalidateOverviewCache();
  };

  const setProjectFavorite = (projectId: number, favorited: boolean, project?: HeritageProject | null) => {
    const favoriteSet = new Set(projectFavoriteIds.value);
    if (favorited) {
      favoriteSet.add(projectId);
    } else {
      favoriteSet.delete(projectId);
    }
    projectFavoriteIds.value = Array.from(favoriteSet);

    if (!favoriteProjectsLoaded.value) {
      return;
    }

    if (!favorited) {
      const existed = favoriteProjects.value.some((item) => Number(item.id) === projectId);
      favoriteProjects.value = favoriteProjects.value.filter((item) => Number(item.id) !== projectId);
      if (existed) {
        favoriteProjectsTotal.value = Math.max(0, favoriteProjectsTotal.value - 1);
      }
      return;
    }

    if (!project || favoriteProjects.value.some((item) => Number(item.id) === projectId)) {
      return;
    }

    favoriteProjects.value = [project, ...favoriteProjects.value].slice(0, favoriteProjectsPageSize.value);
    favoriteProjectsTotal.value += 1;
  };

  const syncProjectFavoriteIds = async () => {
    const pageSize = 100;
    let pageNum = 1;
    let total = 0;
    const collected: number[] = [];

    do {
      const res = await fetchFavoriteList({ pageNum, pageSize });
      const data = res.data.data || {};
      total = Number(data.total || 0);
      for (const item of data.records || []) {
        const projectId = Number(item.id);
        if (Number.isFinite(projectId)) {
          collected.push(projectId);
        }
      }
      pageNum += 1;
    } while (collected.length < total && pageNum <= 20);

    projectFavoriteIds.value = Array.from(new Set(collected));
    return projectFavoriteIds.value;
  };

  const syncFavoriteProjects = async (pageNum = 1, pageSize = favoriteProjectsPageSize.value) => {
    favoriteProjectsPage.value = pageNum;
    favoriteProjectsPageSize.value = pageSize;

    const res = await fetchFavoriteList({ pageNum, pageSize });
    const data = res.data.data || {};
    favoriteProjects.value = data.records || [];
    favoriteProjectsTotal.value = Number(data.total || 0);
    favoriteProjectsLoaded.value = true;
    return data;
  };

  const syncSearchHistory = async () => {
    const res = await fetchSearchHistory();
    searchHistory.value = res.data.data || [];
    searchHistoryLoaded.value = true;
    return searchHistory.value;
  };

  const setSearchHistory = (keywords: string[]) => {
    searchHistory.value = [...keywords];
    searchHistoryLoaded.value = true;
  };

  const prependSearchKeyword = (keyword: string) => {
    const normalized = keyword.trim();
    if (!normalized) {
      return;
    }

    searchHistory.value = [normalized, ...searchHistory.value.filter((item) => item !== normalized)].slice(
      0,
      12
    );
    searchHistoryLoaded.value = true;
  };

  const clearSearchHistoryState = () => {
    searchHistory.value = [];
    searchHistoryLoaded.value = true;
  };

  const syncTrailState = async (force = false) => {
    if (!force) {
      const cached = requestCache.get<{
        favorites: TrailFavoriteRecord[];
        summary: TrailAnalyticsSummary;
      }>("trail-module:state", MODULE_TTL);
      if (cached) {
        trailFavorites.value = cached.favorites;
        trailAnalyticsSummary.value = cached.summary;
        trailStateLoaded.value = true;
        return cached;
      }
    }

    const [favoritesRes, analyticsRes] = await Promise.allSettled([
      fetchTrailFavorites(),
      fetchTrailAnalyticsSummary(),
    ]);

    const nextState = {
      favorites:
        favoritesRes.status === "fulfilled"
          ? favoritesRes.value.data.data || []
          : trailFavorites.value,
      summary:
        analyticsRes.status === "fulfilled"
          ? { ...createEmptyTrailAnalytics(), ...analyticsRes.value.data.data }
          : trailAnalyticsSummary.value,
    };

    trailFavorites.value = nextState.favorites;
    trailAnalyticsSummary.value = nextState.summary;
    trailStateLoaded.value = true;
    requestCache.set("trail-module:state", nextState, MODULE_TTL);
    return nextState;
  };

  const syncLearningState = async (force = false) => {
    if (!force) {
      const cached = requestCache.get<{
        favorites: LearningPlanFavoriteRecord[];
        summary: LearningPlanAnalyticsSummary;
      }>("learning-module:state", MODULE_TTL);
      if (cached) {
        learningFavorites.value = cached.favorites;
        learningAnalyticsSummary.value = cached.summary;
        learningStateLoaded.value = true;
        return cached;
      }
    }

    const [favoritesRes, analyticsRes] = await Promise.allSettled([
      fetchLearningPlanFavorites(),
      fetchLearningPlanAnalyticsSummary(),
    ]);

    const nextState = {
      favorites:
        favoritesRes.status === "fulfilled"
          ? favoritesRes.value.data.data || []
          : learningFavorites.value,
      summary:
        analyticsRes.status === "fulfilled"
          ? { ...createEmptyLearningAnalytics(), ...analyticsRes.value.data.data }
          : learningAnalyticsSummary.value,
    };

    learningFavorites.value = nextState.favorites;
    learningAnalyticsSummary.value = nextState.summary;
    learningStateLoaded.value = true;
    requestCache.set("learning-module:state", nextState, MODULE_TTL);
    return nextState;
  };

  const clearAllCaches = () => {
    inflightOverviewPromise = null;
    overviewLoading.value = false;
    overviewErrorMessage.value = "";
    projects.value = [];
    hotProjects.value = [];
    regionStats.value = [];
    projectFavoriteIds.value = [];
    favoriteProjects.value = [];
    favoriteProjectsTotal.value = 0;
    favoriteProjectsPage.value = 1;
    favoriteProjectsPageSize.value = 8;
    favoriteProjectsLoaded.value = false;
    searchHistory.value = [];
    searchHistoryLoaded.value = false;
    trailFavorites.value = [];
    trailAnalyticsSummary.value = createEmptyTrailAnalytics();
    trailStateLoaded.value = false;
    learningFavorites.value = [];
    learningAnalyticsSummary.value = createEmptyLearningAnalytics();
    learningStateLoaded.value = false;
    invalidateOverviewCache();
    requestCache.clear();
  };

  const isProjectFavorited = (projectId?: number | null) =>
    Number.isFinite(Number(projectId)) && projectFavoriteIds.value.includes(Number(projectId));

  const bumpProjectView = (projectId: number) => {
    const increaseViewCount = <T extends { id?: number | null; viewCount?: number | null }>(items: T[]) =>
      items.map((item) =>
        Number(item.id) === projectId
          ? {
            ...item,
            viewCount: Number(item.viewCount || 0) + 1,
          }
          : item
      );

    projects.value = increaseViewCount(projects.value);
    favoriteProjects.value = increaseViewCount(favoriteProjects.value);
    hotProjects.value = increaseViewCount(hotProjects.value).sort(
      (left, right) => Number(right.viewCount || 0) - Number(left.viewCount || 0)
    );
  };

  const setProjectViewCount = (
    projectId: number,
    viewCount: number,
    project?: Pick<HeritageProject, "name" | "coverUrl"> | null
  ) => {
    const hotProjectDetail =
      project ||
      projects.value.find((item) => Number(item.id) === projectId) ||
      favoriteProjects.value.find((item) => Number(item.id) === projectId) ||
      hotProjects.value.find((item) => Number(item.id) === projectId);

    const normalizeViewCount = <T extends { id?: number | null; viewCount?: number | null }>(items: T[]) =>
      items.map((item) =>
        Number(item.id) === projectId
          ? {
            ...item,
            viewCount,
          }
          : item
      );

    projects.value = normalizeViewCount(projects.value);
    favoriteProjects.value = normalizeViewCount(favoriteProjects.value);

    const nextHotProjects = hotProjects.value.some((item) => Number(item.id) === projectId)
      ? normalizeViewCount(hotProjects.value)
      : [
        {
          id: projectId,
          name: hotProjectDetail?.name || `项目 ${projectId}`,
          coverUrl: hotProjectDetail?.coverUrl || null,
          viewCount,
        },
        ...hotProjects.value,
      ];

    hotProjects.value = sortHotProjectItems(nextHotProjects).slice(0, HOT_PROJECT_LIMIT);
  };

  return {
    overviewLoading,
    overviewErrorMessage,
    projects,
    hotProjects,
    regionStats,
    projectFavoriteIds,
    favoriteProjects,
    favoriteProjectsTotal,
    favoriteProjectsPage,
    favoriteProjectsPageSize,
    favoriteProjectsLoaded,
    searchHistory,
    searchHistoryLoaded,
    trailFavorites,
    trailAnalyticsSummary,
    trailStateLoaded,
    learningFavorites,
    learningAnalyticsSummary,
    learningStateLoaded,
    ensureOverview,
    refreshOverview,
    invalidateOverviewCache,
    mergeProject,
    removeProject,
    setProjectFavorite,
    syncProjectFavoriteIds,
    syncFavoriteProjects,
    syncSearchHistory,
    setSearchHistory,
    prependSearchKeyword,
    clearSearchHistoryState,
    syncTrailState,
    syncLearningState,
    clearAllCaches,
    isProjectFavorited,
    bumpProjectView,
    setProjectViewCount,
  };
});
