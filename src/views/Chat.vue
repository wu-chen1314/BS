<template>
  <div class="chat-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">AI HERITAGE ASSISTANT</p>
        <h1>AI 非遗知识助手</h1>
        <p class="page-desc">
          把会话管理、问答流和快捷入口放回同一个更轻的工作区里，减少装饰性背景和无效层级，让切换会话、继续提问和回看历史更顺。
        </p>
        <div class="hero-tags">
          <span class="pill">{{ stateLabel }}</span>
          <span class="pill">{{ activeSessionTitle }}</span>
          <span class="pill">{{ isBatchMode ? `批量模式 ${selectedChats.length} 条` : "单会话浏览" }}</span>
        </div>
      </div>

      <div class="hero-metrics">
        <article v-for="item in heroMetrics" :key="item.label" class="metric-card heritage-float-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.help }}</p>
        </article>
      </div>
    </section>

    <section class="workspace-grid">
      <ChatSidebar
        :sessions="chatSessions"
        :active-id="activeChatId"
        :is-batch-mode="isBatchMode"
        :selected-ids="selectedChats"
        :loading="sessionLoading"
        :error-message="sidebarErrorMessage"
        @create-chat="createNewChat"
        @delete-chat="deleteChat"
        @toggle-batch-mode="toggleBatchMode"
        @toggle-select="toggleChatSelection"
        @batch-delete="batchDelete"
        @select-chat="selectChat"
      />

      <div class="chat-main heritage-float-card">
        <div class="workspace-head">
          <div>
            <p class="section-kicker">CONVERSATION WORKSPACE</p>
            <h2>{{ activeSessionTitle }}</h2>
            <p class="section-desc">通过连续对话了解非遗项目、历史脉络、传播方法和文旅策划思路。</p>
          </div>
          <div class="workspace-user">
            <el-avatar :size="48" :src="userAvatar" />
            <div class="user-details">
              <span class="user-name">{{ userInfo.nickname || userInfo.username || "游客" }}</span>
              <span class="user-role">{{ userInfo.role === "admin" ? "管理员" : "文化探索者" }}</span>
            </div>
          </div>
        </div>

        <div class="workspace-meta">
          <span class="meta-chip">状态：{{ stateLabel }}</span>
          <span class="meta-chip">会话数：{{ chatSessions.length }}</span>
          <span class="meta-chip">消息数：{{ messages.length }}</span>
        </div>

        <div v-if="historyErrorMessage" class="state-banner error-state">{{ historyErrorMessage }}</div>
        <div v-else-if="historyLoading" class="state-banner">正在加载历史消息...</div>

        <div class="chat-content">
          <ChatMessageStream
            ref="messageStreamRef"
            :messages="messages"
            :ai-avatar="aiAvatar"
            :user-avatar="userAvatar"
            :suggestions="welcomeSuggestions"
            @copy="copyMessage"
            @regenerate="regenerateResponse"
            @select-suggestion="sendSuggestion"
          />

          <ChatComposer
            v-model="inputMessage"
            :disabled="isBusy"
            :loading="isThinking"
            :quick-questions="quickQuestions"
            @submit="sendMessage"
            @clear="clearInput"
            @select-suggestion="sendSuggestion"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import ChatComposer from "@/components/chat/ChatComposer.vue";
import ChatMessageStream from "@/components/chat/ChatMessageStream.vue";
import ChatSidebar from "@/components/chat/ChatSidebar.vue";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { useChatWorkspaceStore } from "@/stores/chatWorkspace";
import type { ChatMessage } from "@/types/chat";
import { confirmDangerAction, selectionRequiredText, showSuccess, showWarning, successText } from "@/utils/uiFeedback";
import { getCurrentUser, SESSION_CHANGED_EVENT, type SessionUser } from "@/utils/session";
import { buildStaticUrl } from "@/utils/url";

const aiAvatar = MATERIAL_PLACEHOLDERS.aiAssistant;

const quickQuestions = [
  { id: 1, emoji: "问", question: "什么是非物质文化遗产？" },
  { id: 2, emoji: "览", question: "中国有哪些代表性的非遗项目？" },
  { id: 3, emoji: "技", question: "传统技艺类非遗有哪些典型案例？" },
  { id: 4, emoji: "路", question: "怎样策划一条非遗主题体验路线？" },
];

const welcomeSuggestions = [
  {
    question: "什么是非物质文化遗产？",
    emoji: "问",
    description: "先了解非遗的基本概念、类型和保护价值。",
  },
  {
    question: "中国有哪些代表性的非遗项目？",
    emoji: "览",
    description: "快速浏览典型的国家级和地方特色非遗案例。",
  },
  {
    question: "非遗如何保护和传播？",
    emoji: "答",
    description: "一起梳理传承、教育、文旅和数字化传播思路。",
  },
];

const chatStore = useChatWorkspaceStore();
const {
  activeChatId,
  chatSessions,
  historyErrorMessage,
  historyLoading,
  isBusy,
  isThinking,
  messages,
  sessionLoading,
  sidebarErrorMessage,
} = storeToRefs(chatStore);

const userInfo = ref<SessionUser>(getCurrentUser() || {});
const inputMessage = ref("");
const isBatchMode = ref(false);
const selectedChats = ref<number[]>([]);
const messageStreamRef = ref<InstanceType<typeof ChatMessageStream>>();

const userAvatar = computed(
  () => buildStaticUrl(userInfo.value.avatarUrl) || MATERIAL_PLACEHOLDERS.avatar
);
const activeSessionTitle = computed(() => {
  const activeSession = chatSessions.value.find((item) => item.id === activeChatId.value);
  return activeSession?.title || (messages.value.length ? "当前对话" : "新对话");
});
const stateLabel = computed(() => {
  if (historyErrorMessage.value) {
    return "会话异常";
  }
  if (isThinking.value) {
    return "AI 思考中";
  }
  if (historyLoading.value) {
    return "加载历史";
  }
  if (sessionLoading.value) {
    return "同步会话";
  }
  if (activeChatId.value) {
    return "对话进行中";
  }
  return "等待提问";
});
const heroMetrics = computed(() => [
  {
    help: "当前账号下可继续访问的对话数",
    label: "会话数量",
    value: chatSessions.value.length,
  },
  {
    help: "当前工作区已展示的消息条数",
    label: "当前消息",
    value: messages.value.length,
  },
  {
    help: "输入区可直接调用的快速问题",
    label: "快捷问题",
    value: quickQuestions.length,
  },
  {
    help: "当前登录账号的助手使用身份",
    label: "当前身份",
    value: userInfo.value.role === "admin" ? "管理员" : "探索者",
  },
]);

const refreshCurrentUser = () => {
  userInfo.value = getCurrentUser() || {};
  chatStore.syncSessionScope();
};

const createNewChat = async () => {
  if (isBusy.value) {
    return;
  }

  try {
    await chatStore.createNewChat();
    isBatchMode.value = false;
    selectedChats.value = [];
  } catch (error) {
    console.error("Failed to create chat", error);
    ElMessage.error("创建会话失败");
  }
};

const deleteChat = async (chatId: number) => {
  try {
    await confirmDangerAction({ subject: "这条会话记录" });
    await chatStore.removeChat(chatId);
    selectedChats.value = selectedChats.value.filter((id) => id !== chatId);
    showSuccess(successText.deleted("会话"));
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to delete chat", error);
      ElMessage.error("删除会话失败");
    }
  }
};

const toggleBatchMode = () => {
  isBatchMode.value = !isBatchMode.value;
  if (!isBatchMode.value) {
    selectedChats.value = [];
  }
};

const toggleChatSelection = (chatId: number) => {
  if (selectedChats.value.includes(chatId)) {
    selectedChats.value = selectedChats.value.filter((id) => id !== chatId);
    return;
  }

  selectedChats.value = [...selectedChats.value, chatId];
};

const batchDelete = async () => {
  if (selectedChats.value.length === 0) {
    showWarning(selectionRequiredText("会话", "删除"));
    return;
  }

  try {
    await confirmDangerAction({
      subject: `选中的 ${selectedChats.value.length} 条会话记录`,
    });

    const { failedIds, succeededIds } = await chatStore.removeChats([...selectedChats.value]);
    selectedChats.value = failedIds;

    if (failedIds.length === 0) {
      isBatchMode.value = false;
      showSuccess(successText.batchDeleted("会话"));
      return;
    }

    showWarning(`已删除 ${succeededIds.length} 条，仍有 ${failedIds.length} 条删除失败，请稍后重试。`);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to batch delete sessions", error);
      ElMessage.error("批量删除失败");
    }
  }
};

const selectChat = async (chatId: number) => {
  await chatStore.selectChat(chatId);
  await messageStreamRef.value?.scrollToBottom();
};

const sendMessage = async (preset?: string) => {
  const content = (typeof preset === "string" ? preset : inputMessage.value).trim();
  if (!content || isBusy.value) {
    return;
  }

  inputMessage.value = "";

  try {
    await chatStore.sendMessage(content);
  } catch (error) {
    console.error("Failed to send message", error);
    const message = error instanceof Error && error.message ? error.message : "消息发送失败，请稍后重试。";
    ElMessage.error(message);
  } finally {
    await messageStreamRef.value?.scrollToBottom();
  }
};

const regenerateResponse = async (messageIndex: number) => {
  if (isBusy.value) {
    return;
  }

  const previousUserMessage = [...messages.value.slice(0, messageIndex)]
    .reverse()
    .find((item: ChatMessage) => item.role === "user");

  if (previousUserMessage) {
    await sendMessage(previousUserMessage.content);
  }
};

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content);
    showSuccess(successText.copied("内容"));
  } catch (error) {
    console.error("Failed to copy message", error);
    ElMessage.error("复制失败");
  }
};

const sendSuggestion = async (suggestion: string) => {
  inputMessage.value = suggestion;
  await sendMessage(suggestion);
};

const clearInput = () => {
  inputMessage.value = "";
};

onMounted(async () => {
  refreshCurrentUser();
  window.addEventListener(SESSION_CHANGED_EVENT, refreshCurrentUser);
  await chatStore.ensureBootstrapped();
});

onBeforeUnmount(() => {
  window.removeEventListener(SESSION_CHANGED_EVENT, refreshCurrentUser);
});
</script>

<style scoped>
.chat-page {
  min-height: calc(100vh - 88px);
  display: grid;
  gap: 18px;
  padding: 12px 6px 28px;
}

.hero,
.chat-main {
  border-radius: 26px;
  border: 1px solid var(--heritage-border);
  background: var(--heritage-surface);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(192, 138, 63, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(255, 251, 244, 0.98));
}

.page-kicker,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--heritage-gold);
}

.hero-copy h1,
.workspace-head h2 {
  margin: 0;
  color: var(--heritage-ink);
}

.page-desc,
.section-desc,
.metric-card p,
.user-role {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-tags,
.workspace-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tags {
  margin-top: 20px;
}

.pill,
.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(192, 138, 63, 0.12);
  color: var(--heritage-primary);
  font-size: 12px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.metric-card span {
  color: var(--heritage-muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 30px;
  color: var(--heritage-primary);
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.chat-main {
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.workspace-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  flex-wrap: wrap;
  padding: 24px 24px 16px;
}

.workspace-user,
.user-details {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-details {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.user-name {
  font-weight: 600;
  color: var(--heritage-ink);
}

.workspace-meta {
  padding: 0 24px 18px;
}

.state-banner {
  margin: 0 24px;
  padding: 12px 18px;
  border-radius: 16px;
  background: rgba(192, 138, 63, 0.1);
  color: var(--heritage-ink-soft);
  border: 1px solid rgba(192, 138, 63, 0.18);
}

.error-state {
  background: rgba(164, 59, 47, 0.08);
  color: var(--heritage-primary);
  border-color: rgba(164, 59, 47, 0.18);
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

:deep(.chat-sidebar) {
  width: auto;
  border-radius: 26px;
  background: rgba(255, 251, 244, 0.92);
  box-shadow: var(--heritage-card-shadow-rest);
}

:deep(.sidebar-header) {
  padding: 22px 20px;
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(223, 188, 123, 0.12));
}

:deep(.chat-list) {
  padding: 14px;
}

:deep(.chat-item) {
  border-radius: 18px;
  margin-bottom: 10px;
}

:deep(.chat-history) {
  padding: 22px 24px;
  background: linear-gradient(180deg, rgba(255, 251, 244, 0.9), rgba(255, 249, 241, 0.98));
}

:deep(.chat-input-container) {
  padding: 18px 24px 24px;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.96), rgba(223, 188, 123, 0.08));
}

:deep(.question-chip) {
  border-radius: 999px;
}

:deep(.message-bubble) {
  border-radius: 18px;
}

@media (max-width: 1080px) {
  .hero,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .chat-page {
    padding: 10px 0 20px;
  }

  .hero,
  .workspace-head {
    padding: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .workspace-head {
    flex-direction: column;
  }

  .workspace-meta,
  .state-banner {
    margin-left: 18px;
    margin-right: 18px;
    padding-left: 14px;
    padding-right: 14px;
  }

  :deep(.chat-history),
  :deep(.chat-input-container) {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
