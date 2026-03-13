import { CATEGORY_LABELS } from "@/constants/heritage";

export const getCategoryName = (value?: number | string | null) => {
  const id = Number(value);
  return CATEGORY_LABELS[id] || "其他类别";
};

export const getProtectLevelType = (level?: string | null) => {
  const mapping: Record<string, string> = {
    国家级: "danger",
    省级: "warning",
    市级: "success",
    县级: "info",
  };
  return mapping[level || ""] || "info";
};

export const stripHtml = (html?: string | null) => {
  if (!html) {
    return "";
  }
  return html.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
};

export const summarizeRichText = (
  html?: string | null,
  maxLength = 88,
  fallback = "暂无内容简介，可进入详情页查看完整内容。"
) => {
  const plainText = stripHtml(html);
  if (!plainText) {
    return fallback;
  }
  return plainText.length > maxLength ? `${plainText.slice(0, maxLength)}...` : plainText;
};

export const filterProjectsByCategories = <T extends { categoryId?: number | string | null }>(
  projects: T[],
  categoryIds: number[]
) => {
  const categorySet = new Set(categoryIds.map(Number));
  return projects.filter((item) => categorySet.has(Number(item.categoryId)));
};

export const sortRegionsByValue = <T extends { value?: number | string | null }>(regions: T[]) =>
  [...regions].sort((left, right) => Number(right.value || 0) - Number(left.value || 0));

export const getAverageViewCount = <T extends { viewCount?: number | string | null }>(projects: T[]) => {
  if (!projects.length) {
    return 0;
  }
  const total = projects.reduce((sum, item) => sum + Number(item.viewCount || 0), 0);
  return Math.round(total / projects.length);
};
