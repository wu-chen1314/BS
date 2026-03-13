import request from "@/utils/request";
import type {
  RegionCategoryBootstrapData,
  RegionCategoryCategoryStats,
  RegionCategoryOption,
  RegionCategoryProjectPage,
  RegionCategoryProjectQuery,
  RegionCategoryRegionStats,
} from "@/types/region-category";

export const fetchRegionCategoryBootstrap = () =>
  request.get<{ code: number; data: RegionCategoryBootstrapData }>("/region-category/all");

export const fetchRegionCategoryCities = (provinceId: number) =>
  request.get<{ code: number; data: RegionCategoryOption[] }>("/region-category/cities", {
    params: { provinceId },
  });

export const fetchRegionCategoryProjects = (params: RegionCategoryProjectQuery) =>
  request.get<{ code: number; data: RegionCategoryProjectPage }>("/region-category/projects", {
    params,
  });

export const fetchRegionCategoryRegionStats = (regionId?: number) =>
  request.get<{ code: number; data: RegionCategoryRegionStats }>("/region-category/statistics/by-region", {
    params: { regionId },
  });

export const fetchRegionCategoryCategoryStats = (categoryId?: number) =>
  request.get<{ code: number; data: RegionCategoryCategoryStats }>("/region-category/statistics/by-category", {
    params: { categoryId },
  });
