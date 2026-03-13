import request from "@/utils/request";

export const fetchProjectPage = (params: Record<string, unknown>) =>
  request.get("/projects/page", { params });

export const fetchProjectDetail = (id: number) => request.get(`/projects/${id}`);

export const fetchStatisticsByLevel = () => request.get("/statistics/level");

export const fetchStatisticsByStatus = () => request.get("/statistics/status");

export const fetchStatisticsMap = () => request.get("/statistics/map");

export const fetchHotProjects = (limit = 5) =>
  request.get("/view/hot", {
    params: { limit },
  });

export const increaseProjectViewCount = (projectId: number) =>
  request.get("/view/count", {
    params: { projectId },
  });

export const fetchComments = (projectId: number) =>
  request.get("/comments/list", {
    params: { projectId },
  });

export const addComment = (payload: { projectId: number; content: string; parentId?: number | null }) =>
  request.post("/comments/add", payload);

export const deleteComment = (id: number) => request.delete(`/comments/delete/${id}`);

export const toggleFavorite = (projectId: number) =>
  request.post("/favorites/toggle", {
    projectId,
  });

export const checkFavorite = (projectId: number) =>
  request.get("/favorites/check", {
    params: { projectId },
  });

export const fetchFavoriteList = (params: { pageNum?: number; pageSize?: number } = {}) =>
  request.get("/favorites/list", {
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 100,
    },
  });

export const recordSearchKeyword = (keyword: string) =>
  request.post("/search/record", {
    keyword,
  });

export const fetchSearchHistory = () => request.get("/search/history");

export const clearSearchHistory = () => request.delete("/search/history");

export const saveProject = (payload: Record<string, unknown>) =>
  payload.id ? request.put("/projects/update", payload) : request.post("/projects/add", payload);

export const deleteProject = (id: number) => request.delete(`/projects/delete/${id}`);

export const deleteProjectsBatch = (ids: number[]) => request.delete("/projects/delete/batch", { data: ids });

export const fetchInheritorPage = (params: Record<string, unknown>) =>
  request.get("/inheritors/page", {
    params,
  });
