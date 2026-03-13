import request from "@/utils/request";
import type { NewsArticleEditorPayload } from "@/types/news";

export const fetchNewsArticles = (params: {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  tag?: string;
}) =>
  request.get("/news/articles", {
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 4,
      keyword: params.keyword || undefined,
      tag: params.tag || undefined,
    },
  });

export const fetchNewsArticleDetail = (id: number) => request.get(`/news/articles/${id}`);

export const recordNewsArticleRead = (id: number) => request.post(`/news/articles/${id}/read`);

export const fetchNewsDashboard = (recentLimit = 5) =>
  request.get("/news/dashboard", {
    params: {
      recentLimit,
    },
  });

export const createNewsArticle = (payload: NewsArticleEditorPayload) =>
  request.post("/news/articles", payload);

export const updateNewsArticle = (id: number, payload: NewsArticleEditorPayload) =>
  request.put(`/news/articles/${id}`, payload);

export const deleteNewsArticle = (id: number) => request.delete(`/news/articles/${id}`);
