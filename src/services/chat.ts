import request from "@/utils/request";

export interface ChatSessionPayload {
  title?: string;
}

export const fetchChatSessions = (page = 1, limit = 20) =>
  request.get("/chat/sessions", {
    params: { page, limit },
  });

export const fetchChatHistory = (chatId?: number, limit = 50) =>
  request.get("/chat/history", {
    params: {
      chatId,
      limit,
    },
  });

export const createChatSession = (payload: ChatSessionPayload = {}) => request.post("/chat/sessions", payload);

export const deleteChatSession = (id: number) => request.delete(`/chat/sessions/${id}`);

export const sendChatMessage = (payload: { message: string; chatId?: number; title?: string }) =>
  request.post("/chat/send", payload);
