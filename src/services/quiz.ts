import request from "@/utils/request";
import type { QuizAttemptAnswer, QuizQuestionEditorPayload } from "@/types/quiz";

export const fetchQuizQuestions = (limit = 5) =>
  request.get("/quiz/questions", {
    params: {
      limit,
    },
  });

export const submitQuizAttempt = (payload: {
  durationSeconds: number;
  answers: QuizAttemptAnswer[];
}) => request.post("/quiz/attempts", payload);

export const fetchQuizSummary = () => request.get("/quiz/summary");

export const fetchQuizHistory = (limit = 6) =>
  request.get("/quiz/history", {
    params: {
      limit,
    },
  });

export const fetchQuizQuestionAdminPage = (params: {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  active?: boolean | null;
} = {}) =>
  request.get("/quiz/questions/manage", {
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 8,
      keyword: params.keyword || undefined,
      active: params.active ?? undefined,
    },
  });

export const createQuizQuestion = (payload: QuizQuestionEditorPayload) =>
  request.post("/quiz/questions/manage", payload);

export const updateQuizQuestion = (id: number, payload: QuizQuestionEditorPayload) =>
  request.put(`/quiz/questions/manage/${id}`, payload);

export const deleteQuizQuestion = (id: number) =>
  request.delete(`/quiz/questions/manage/${id}`);
