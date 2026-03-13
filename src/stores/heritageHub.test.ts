import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useHeritageHubStore } from "./heritageHub";
import { requestCache } from "@/utils/cache";

const projectApiMocks = vi.hoisted(() => ({
  fetchFavoriteList: vi.fn(),
  fetchHotProjects: vi.fn(),
  fetchProjectPage: vi.fn(),
  fetchSearchHistory: vi.fn(),
  fetchStatisticsMap: vi.fn(),
}));

const trailApiMocks = vi.hoisted(() => ({
  fetchTrailAnalyticsSummary: vi.fn(),
  fetchTrailFavorites: vi.fn(),
}));

const learningApiMocks = vi.hoisted(() => ({
  fetchLearningPlanAnalyticsSummary: vi.fn(),
  fetchLearningPlanFavorites: vi.fn(),
}));

vi.mock("@/services/project", () => ({
  fetchFavoriteList: projectApiMocks.fetchFavoriteList,
  fetchHotProjects: projectApiMocks.fetchHotProjects,
  fetchProjectPage: projectApiMocks.fetchProjectPage,
  fetchSearchHistory: projectApiMocks.fetchSearchHistory,
  fetchStatisticsMap: projectApiMocks.fetchStatisticsMap,
}));

vi.mock("@/services/trail", () => ({
  fetchTrailAnalyticsSummary: trailApiMocks.fetchTrailAnalyticsSummary,
  fetchTrailFavorites: trailApiMocks.fetchTrailFavorites,
}));

vi.mock("@/services/learning", () => ({
  fetchLearningPlanAnalyticsSummary: learningApiMocks.fetchLearningPlanAnalyticsSummary,
  fetchLearningPlanFavorites: learningApiMocks.fetchLearningPlanFavorites,
}));

const createProjectRecord = (id: number) => ({
  id,
  name: `project-${id}`,
});

describe("heritageHub store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    projectApiMocks.fetchFavoriteList.mockReset();
    projectApiMocks.fetchHotProjects.mockReset();
    projectApiMocks.fetchProjectPage.mockReset();
    projectApiMocks.fetchSearchHistory.mockReset();
    projectApiMocks.fetchStatisticsMap.mockReset();
    trailApiMocks.fetchTrailAnalyticsSummary.mockReset();
    trailApiMocks.fetchTrailFavorites.mockReset();
    learningApiMocks.fetchLearningPlanAnalyticsSummary.mockReset();
    learningApiMocks.fetchLearningPlanFavorites.mockReset();
  });

  it("hydrates overview projects across all pages instead of truncating to the first page", async () => {
    const store = useHeritageHubStore();
    const firstPageRecords = Array.from({ length: 60 }, (_, index) => createProjectRecord(index + 1));
    const secondPageRecords = Array.from({ length: 5 }, (_, index) => createProjectRecord(index + 61));

    projectApiMocks.fetchProjectPage
      .mockResolvedValueOnce({
        data: {
          data: {
            records: firstPageRecords,
            total: 65,
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: {
            records: secondPageRecords,
            total: 65,
          },
        },
      });
    projectApiMocks.fetchHotProjects.mockResolvedValue({
      data: {
        data: [],
      },
    });
    projectApiMocks.fetchStatisticsMap.mockResolvedValue({
      data: {
        data: [],
      },
    });

    await store.ensureOverview({ force: true, pageSize: 60 });

    expect(projectApiMocks.fetchProjectPage).toHaveBeenCalledTimes(2);
    expect(store.projects).toHaveLength(65);
    expect(store.projects.at(-1)?.id).toBe(65);
  });

  it("hydrates trail and learning sidebars from fulfilled requests without rechecking nested codes", async () => {
    const store = useHeritageHubStore();

    trailApiMocks.fetchTrailFavorites.mockResolvedValue({
      data: {
        data: [
          {
            id: 1,
            trailId: "craft-route",
            trailTitle: "工艺路线",
            routeType: "template",
          },
        ],
      },
    });
    trailApiMocks.fetchTrailAnalyticsSummary.mockResolvedValue({
      data: {
        data: {
          favoriteCount: 3,
          viewCount: 9,
        },
      },
    });
    learningApiMocks.fetchLearningPlanFavorites.mockResolvedValue({
      data: {
        data: [
          {
            id: 2,
            planId: "school-plan",
            planTitle: "校园研学",
            trackId: "school",
          },
        ],
      },
    });
    learningApiMocks.fetchLearningPlanAnalyticsSummary.mockResolvedValue({
      data: {
        data: {
          favoriteCount: 4,
          routeCarryCount: 2,
        },
      },
    });

    const trailState = await store.syncTrailState(true);
    const learningState = await store.syncLearningState(true);

    expect(trailState.favorites).toHaveLength(1);
    expect(store.trailAnalyticsSummary.favoriteCount).toBe(3);
    expect(store.trailAnalyticsSummary.viewCount).toBe(9);
    expect(learningState.favorites).toHaveLength(1);
    expect(store.learningAnalyticsSummary.favoriteCount).toBe(4);
    expect(store.learningAnalyticsSummary.routeCarryCount).toBe(2);
  });

  it("clears overview and module caches when the session changes", () => {
    const store = useHeritageHubStore();

    store.projects = [createProjectRecord(1)];
    store.hotProjects = [{ id: 2, name: "hot-project" }];
    store.regionStats = [{ name: "Zhejiang", value: 3 }];
    store.projectFavoriteIds = [1, 2];
    store.favoriteProjects = [createProjectRecord(1)];
    store.favoriteProjectsTotal = 1;
    store.favoriteProjectsLoaded = true;
    store.searchHistory = ["shadow-cache"];
    store.trailFavorites = [
      {
        id: 1,
        trailId: "craft-route",
        trailTitle: "Craft Route",
        routeType: "template",
      },
    ];
    store.learningFavorites = [
      {
        id: 2,
        planId: "school-plan",
        planTitle: "School Plan",
        trackId: "school",
      },
    ];

    requestCache.set("heritage-overview:snapshot", {
      projects: [createProjectRecord(9)],
      hotProjects: [],
      regionStats: [],
      fetchedAt: Date.now(),
    });
    window.sessionStorage.setItem(
      "heritage-overview:snapshot",
      JSON.stringify({
        projects: [createProjectRecord(7)],
        hotProjects: [],
        regionStats: [],
        fetchedAt: Date.now(),
      })
    );

    store.clearAllCaches();

    expect(store.projects).toEqual([]);
    expect(store.hotProjects).toEqual([]);
    expect(store.regionStats).toEqual([]);
    expect(store.projectFavoriteIds).toEqual([]);
    expect(store.favoriteProjects).toEqual([]);
    expect(store.favoriteProjectsTotal).toBe(0);
    expect(store.searchHistory).toEqual([]);
    expect(store.trailFavorites).toEqual([]);
    expect(store.learningFavorites).toEqual([]);
    expect(requestCache.get("heritage-overview:snapshot")).toBeNull();
    expect(window.sessionStorage.getItem("heritage-overview:snapshot")).toBeNull();
  });

  it("does not decrement favorite totals when removing a project that is not in the loaded favorites page", () => {
    const store = useHeritageHubStore();

    store.favoriteProjectsLoaded = true;
    store.favoriteProjects = [createProjectRecord(1)];
    store.favoriteProjectsTotal = 3;

    store.removeProject(9);

    expect(store.favoriteProjectsTotal).toBe(3);
    expect(store.favoriteProjects).toHaveLength(1);
  });

  it("syncs a project view count across overview, favorites and hot ranking state", () => {
    const store = useHeritageHubStore();

    store.projects = [
      { ...createProjectRecord(1), viewCount: 2 },
      { ...createProjectRecord(2), viewCount: 6 },
    ];
    store.favoriteProjects = [{ ...createProjectRecord(1), viewCount: 2 }];
    store.hotProjects = [
      { id: 1, name: "project-1", viewCount: 2 },
      { id: 2, name: "project-2", viewCount: 6 },
    ];

    store.setProjectViewCount(1, 9);

    expect(store.projects[0].viewCount).toBe(9);
    expect(store.favoriteProjects[0].viewCount).toBe(9);
    expect(store.hotProjects[0].id).toBe(1);
    expect(store.hotProjects[0].viewCount).toBe(9);
  });

  it("adds a viewed project into hot ranking state when it was not previously ranked", () => {
    const store = useHeritageHubStore();

    store.projects = [
      { ...createProjectRecord(3), coverUrl: "/covers/3.jpg", viewCount: 11 },
    ];
    store.hotProjects = [
      { id: 1, name: "project-1", viewCount: 18 },
      { id: 2, name: "project-2", viewCount: 12 },
    ];

    store.setProjectViewCount(3, 15, {
      name: "project-3",
      coverUrl: "/covers/3.jpg",
    });

    expect(store.hotProjects.map((item) => item.id)).toEqual([1, 3, 2]);
    expect(store.hotProjects[1]).toMatchObject({
      id: 3,
      name: "project-3",
      coverUrl: "/covers/3.jpg",
      viewCount: 15,
    });
  });
});
