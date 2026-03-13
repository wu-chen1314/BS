import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { deleteChatSession, fetchChatHistory, fetchChatSessions, sendChatMessage } from "@/services/chat";
import type { ChatHistoryRecord, ChatMessage, ChatSessionRecord } from "@/types/chat";
import { getCurrentUser } from "@/utils/session";

interface BatchDeleteResult {
  failedIds: number[];
  succeededIds: number[];
}

const nextMessageId = () => Date.now() + Math.floor(Math.random() * 1000);

const resolveSessionScopeKey = () => {
  const currentUser = getCurrentUser();
  if (!currentUser?.id && !currentUser?.token) {
    return "";
  }

  return `${currentUser.id ?? "anonymous"}:${currentUser.token ?? ""}`;
};

const formatChatTime = (timestamp?: string | null) => {
  if (!timestamp) {
    return "";
  }

  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleDateString("zh-CN", {
    day: "2-digit",
    month: "2-digit",
  });
};

const formatPreview = (text?: string | null) => {
  if (!text) {
    return "点击继续查看历史消息";
  }

  return text.length > 30 ? `${text.slice(0, 30)}...` : text;
};

const toChatMessages = (records: ChatHistoryRecord[]): ChatMessage[] =>
  records.map((item, index) => ({
    id: nextMessageId() + index,
    role: item.role,
    content: item.content,
    status: item.role === "user" ? "sent" : undefined,
    timestamp: item.timestamp || new Date().toISOString(),
  }));

export const useChatWorkspaceStore = defineStore("chatWorkspace", () => {
  let bootstrapPromise: Promise<boolean> | null = null;

  const sessionScopeKey = ref(resolveSessionScopeKey());
  const bootstrapped = ref(false);
  const activeChatId = ref<number | null>(null);
  const sessionLoading = ref(false);
  const historyLoading = ref(false);
  const isSending = ref(false);
  const isThinking = ref(false);
  const messages = ref<ChatMessage[]>([]);
  const rawSessions = ref<ChatSessionRecord[]>([]);
  const sidebarErrorMessage = ref("");
  const historyErrorMessage = ref("");

  const isBusy = computed(() => isSending.value || isThinking.value);
  const chatSessions = computed(() =>
    rawSessions.value.map((session) => ({
      id: session.id,
      preview: formatPreview(session.lastMessage),
      title: session.title || "新对话",
      updatedLabel: formatChatTime(session.updatedAt),
    }))
  );

  const resetActiveConversation = () => {
    activeChatId.value = null;
    historyErrorMessage.value = "";
    historyLoading.value = false;
    messages.value = [];
  };

  const resetWorkspace = () => {
    bootstrapped.value = false;
    bootstrapPromise = null;
    isSending.value = false;
    isThinking.value = false;
    rawSessions.value = [];
    sessionLoading.value = false;
    sidebarErrorMessage.value = "";
    resetActiveConversation();
  };

  const syncSessionScope = () => {
    const nextScopeKey = resolveSessionScopeKey();
    if (nextScopeKey === sessionScopeKey.value) {
      return;
    }

    sessionScopeKey.value = nextScopeKey;
    resetWorkspace();
  };

  const isAuthenticated = () => {
    syncSessionScope();
    return Boolean(sessionScopeKey.value);
  };

  const ensureAuthenticated = () => {
    if (isAuthenticated()) {
      return;
    }

    throw new Error("登录已失效，请重新登录后再试");
  };

  const upsertSession = (session: ChatSessionRecord) => {
    rawSessions.value = [session, ...rawSessions.value.filter((item) => item.id !== session.id)];
    bootstrapped.value = true;
  };

  const refreshChatList = async () => {
    if (!isAuthenticated()) {
      resetWorkspace();
      return false;
    }

    sessionLoading.value = true;
    sidebarErrorMessage.value = "";

    try {
      const res = await fetchChatSessions(1, 50);
      rawSessions.value = (res.data.data || []) as ChatSessionRecord[];

      if (activeChatId.value && !rawSessions.value.some((item) => item.id === activeChatId.value)) {
        resetActiveConversation();
      }

      bootstrapped.value = true;
      return true;
    } catch (error) {
      console.error("Failed to load chat list", error);
      sidebarErrorMessage.value = "会话列表加载失败，请稍后重试。";
      return false;
    } finally {
      sessionLoading.value = false;
    }
  };

  const ensureBootstrapped = async () => {
    if (bootstrapped.value) {
      return true;
    }

    if (!bootstrapPromise) {
      bootstrapPromise = refreshChatList().finally(() => {
        bootstrapPromise = null;
      });
    }

    return bootstrapPromise;
  };

  const loadChatHistoryById = async (chatId?: number) => {
    if (!chatId) {
      resetActiveConversation();
      return true;
    }

    if (!isAuthenticated()) {
      resetWorkspace();
      return false;
    }

    historyLoading.value = true;
    historyErrorMessage.value = "";

    try {
      const res = await fetchChatHistory(chatId, 50);
      messages.value = toChatMessages((res.data.data || []) as ChatHistoryRecord[]);
      return true;
    } catch (error) {
      console.error("Failed to load chat history", error);
      messages.value = [];
      historyErrorMessage.value = "历史消息加载失败，请重新选择会话。";
      return false;
    } finally {
      historyLoading.value = false;
    }
  };

  const selectChat = async (chatId: number) => {
    activeChatId.value = chatId;
    return loadChatHistoryById(chatId);
  };

  const createNewChat = async () => {
    ensureAuthenticated();
    resetActiveConversation();
    bootstrapped.value = true;
    return null;
  };

  const removeChat = async (chatId: number) => {
    ensureAuthenticated();
    await deleteChatSession(chatId);

    rawSessions.value = rawSessions.value.filter((item) => item.id !== chatId);
    if (activeChatId.value === chatId) {
      resetActiveConversation();
    }
  };

  const removeChats = async (chatIds: number[]): Promise<BatchDeleteResult> => {
    ensureAuthenticated();

    if (chatIds.length === 0) {
      return {
        failedIds: [],
        succeededIds: [],
      };
    }

    const results = await Promise.allSettled(chatIds.map((id) => deleteChatSession(id)));
    const succeededIds = chatIds.filter((id, index) => {
      const result = results[index];
      return result.status === "fulfilled";
    });

    rawSessions.value = rawSessions.value.filter((item) => !succeededIds.includes(item.id));
    if (activeChatId.value && succeededIds.includes(activeChatId.value)) {
      resetActiveConversation();
    }

    return {
      failedIds: chatIds.filter((id) => !succeededIds.includes(id)),
      succeededIds,
    };
  };

  const sendMessage = async (content: string) => {
    const normalizedContent = content.trim();
    if (!normalizedContent || isBusy.value) {
      return null;
    }

    ensureAuthenticated();
    historyErrorMessage.value = "";

    const userMessage: ChatMessage = {
      id: nextMessageId(),
      role: "user",
      content: normalizedContent,
      status: "sending",
      timestamp: new Date().toISOString(),
    };
    const thinkingMessage: ChatMessage = {
      id: nextMessageId(),
      role: "assistant",
      content: "",
      isThinking: true,
      timestamp: new Date().toISOString(),
    };

    messages.value = [...messages.value, userMessage, thinkingMessage];
    isSending.value = true;
    isThinking.value = true;

    try {
      const title =
        normalizedContent.length > 16 ? `${normalizedContent.slice(0, 16)}...` : normalizedContent;
      const res = await sendChatMessage({
        message: normalizedContent,
        chatId: activeChatId.value || undefined,
        title,
      });

      const nextChatId = Number(res.data.data?.chatId);
      const reply = String(res.data.data?.reply || "").trim();
      if (!Number.isFinite(nextChatId) || !reply) {
        throw new Error("AI 响应异常，请稍后重试。");
      }

      activeChatId.value = nextChatId;
      userMessage.status = "sent";
      messages.value = messages.value
        .filter((item) => item.id !== thinkingMessage.id)
        .concat({
          id: nextMessageId(),
          role: "assistant",
          content: reply,
          timestamp: new Date().toISOString(),
        });
      upsertSession({
        id: nextChatId,
        lastMessage: reply,
        title,
        updatedAt: new Date().toISOString(),
      });
      await refreshChatList();

      return reply;
    } catch (error) {
      console.error("Failed to send message", error);
      const message =
        error instanceof Error && error.message ? error.message : "消息发送失败，请稍后重试。";

      userMessage.status = "error";
      messages.value = messages.value
        .filter((item) => item.id !== thinkingMessage.id)
        .concat({
          id: nextMessageId(),
          role: "system",
          content: message,
          timestamp: new Date().toISOString(),
        });
      throw new Error(message);
    } finally {
      isSending.value = false;
      isThinking.value = false;
    }
  };

  return {
    activeChatId,
    bootstrapped,
    chatSessions,
    historyErrorMessage,
    historyLoading,
    isBusy,
    isSending,
    isThinking,
    messages,
    rawSessions,
    sessionLoading,
    sidebarErrorMessage,
    createNewChat,
    ensureBootstrapped,
    loadChatHistoryById,
    refreshChatList,
    removeChat,
    removeChats,
    resetActiveConversation,
    resetWorkspace,
    selectChat,
    sendMessage,
    syncSessionScope,
  };
});
