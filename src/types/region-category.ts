import type { HeritageProject } from "@/types/project";

export interface RegionCategoryOption {
  id: number;
  name: string;
  parentId?: number | null;
  level?: number | null;
}

export interface RegionCategoryNode {
  id: number;
  name: string;
  parentId?: number | null;
  level?: number | null;
  children?: RegionCategoryNode[];
}

export interface RegionCategoryRegionStats {
  totalProjects: number;
  categoryCount: number;
  inheritorCount: number;
}

export interface RegionCategoryCategoryStats {
  totalProjects: number;
  regionCount: number;
  inheritorCount: number;
}

export interface RegionCategoryBootstrapData {
  provinces: RegionCategoryOption[];
  categoryTree: RegionCategoryNode[];
  regionStatistics: RegionCategoryRegionStats;
  categoryStatistics: RegionCategoryCategoryStats;
}

export interface RegionCategoryProjectPage {
  records: HeritageProject[];
  total: number;
}

export interface RegionCategoryProjectQuery {
  page: number;
  limit: number;
  regionId?: number;
  categoryId?: number;
}
