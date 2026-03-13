import { storeToRefs } from "pinia";
import { useHeritageHubStore } from "@/stores/heritageHub";
import type { HeritageProject } from "@/types/project";

export interface HeritageRegionStat {
  name: string;
  value: number;
}

export interface HeritageOverviewSnapshot {
  projects: HeritageProject[];
  hotProjects: HeritageProject[];
  regionStats: HeritageRegionStat[];
  fetchedAt: number;
}

interface LoadOptions {
  hotLimit?: number;
  pageSize?: number;
  force?: boolean;
}

export const useHeritageOverview = () => {
  const store = useHeritageHubStore();
  const { projects, hotProjects, regionStats, overviewLoading, overviewErrorMessage } = storeToRefs(store);

  const load = async (options: LoadOptions = {}) => store.ensureOverview(options);

  return {
    projects,
    hotProjects,
    regionStats,
    loading: overviewLoading,
    errorMessage: overviewErrorMessage,
    load,
  };
};
