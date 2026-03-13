import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useChatWorkspaceStore } from "./chatWorkspace";
import { clearCurrentSession, saveCurrentSession } from "@/utils/session";

const chatApiMocks = vi.hoisted(() => ({
  createChatSession: vi.fn(),
  deleteChatSession: vi.fn(),
  fetchChatHistory: vi.fn(),
  fetchChatSessions: vi.fn(),
  sendChatMessage: vi.fn(),
}));

vi.mock("@/services/chat", () => ({
  createChatSession: chatApiMocks.createChatSession,
  deleteChatSession: chatApiMocks.deleteChatSession,
  fetchChatHistory: chatApiMocks.fetchChatHistory,
  fetchChatSessions: chatApiMocks.fetchChatSessions,
  sendChatMessage: chatApiMocks.sendChatMessage,
}));

describe("chatWorkspace store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    clearCurrentSession();
    saveCurrentSession(
      {
        id: 1,
        username: "tester",
      },
      "chat-token",
      86400
    );

    chatApiMocks.fetchChatSessions.mockReset();
    chatApiMocks.fetchChatHistory.mockReset();
    chatApiMocks.createChatSession.mockReset();
    chatApiMocks.deleteChatSession.mockReset();
    chatApiMocks.sendChatMessage.mockReset();
  });

  it("bootstraps the session list once for concurrent callers", async () => {
    const store = useChatWorkspaceStore();

    chatApiMocks.fetchChatSessions.mockResolvedValue({
      data: {
        code: 200,
        data: [
          {
            id: 101,
            title: "非遗概览",
            lastMessage: "继续看看推荐内容",
            updatedAt: "2026-03-11T08:00:00.000Z",
          },
        ],
      },
    });

    const [first, second] = await Promise.all([store.ensureBootstrapped(), store.ensureBootstrapped()]);

    expect(first).toBe(true);
    expect(second).toBe(true);
    expect(chatApiMocks.fetchChatSessions).toHaveBeenCalledTimes(1);
    expect(store.chatSessions).toHaveLength(1);
    expect(store.chatSessions[0]).toMatchObject({
      id: 101,
      title: "非遗概览",
    });
  });

  it("starts a fresh draft conversation without creating an empty remote session", async () => {
    const store = useChatWorkspaceStore();
    store.activeChatId = 88;
    store.messages = [
      {
        id: 1,
        role: "user",
        content: "existing draft",
        timestamp: "2026-03-12T00:00:00.000Z",
      },
    ];

    await store.createNewChat();

    expect(chatApiMocks.createChatSession).not.toHaveBeenCalled();
    expect(store.activeChatId).toBeNull();
    expect(store.messages).toEqual([]);
  });

  it("reuses one shared message pipeline for send and session refresh", async () => {
    const store = useChatWorkspaceStore();

    chatApiMocks.sendChatMessage.mockResolvedValue({
      data: {
        code: 200,
        data: {
          chatId: 7,
          reply: "可以先从项目背景和传承脉络开始了解。",
        },
      },
    });
    chatApiMocks.fetchChatSessions.mockResolvedValue({
      data: {
        code: 200,
        data: [
          {
            id: 7,
            title: "什么是非遗？",
            lastMessage: "可以先从项目背景和传承脉络开始了解。",
            updatedAt: "2026-03-11T08:05:00.000Z",
          },
        ],
      },
    });

    await store.sendMessage("什么是非遗？");

    expect(chatApiMocks.sendChatMessage).toHaveBeenCalledWith({
      chatId: undefined,
      message: "什么是非遗？",
      title: "什么是非遗？",
    });
    expect(chatApiMocks.fetchChatSessions).toHaveBeenCalledTimes(1);
    expect(store.activeChatId).toBe(7);
    expect(store.messages).toHaveLength(2);
    expect(store.messages[0]).toMatchObject({
      content: "什么是非遗？",
      role: "user",
      status: "sent",
    });
    expect(store.messages[1]).toMatchObject({
      content: "可以先从项目背景和传承脉络开始了解。",
      role: "assistant",
    });
    expect(store.chatSessions[0]).toMatchObject({
      id: 7,
      title: "什么是非遗？",
    });
  });

  it("treats fulfilled chat deletions as success without rechecking nested codes", async () => {
    const store = useChatWorkspaceStore();
    store.rawSessions = [
      {
        id: 10,
        title: "路线规划",
        lastMessage: "保留",
        updatedAt: "2026-03-11T08:00:00.000Z",
      },
      {
        id: 11,
        title: "研学方案",
        lastMessage: "删除",
        updatedAt: "2026-03-11T08:05:00.000Z",
      },
    ];
    store.activeChatId = 11;
    store.messages = [
      {
        id: 1,
        role: "assistant",
        content: "existing history",
        timestamp: "2026-03-12T00:00:00.000Z",
      },
    ];

    chatApiMocks.deleteChatSession.mockResolvedValue({
      data: {},
    });

    const result = await store.removeChats([11]);

    expect(result.succeededIds).toEqual([11]);
    expect(result.failedIds).toEqual([]);
    expect(store.chatSessions).toHaveLength(1);
    expect(store.chatSessions[0]?.id).toBe(10);
    expect(store.activeChatId).toBeNull();
    expect(store.messages).toEqual([]);
  });
});
