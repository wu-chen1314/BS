<template>
  <div v-if="showAssistant" class="chat-container">
    <el-button class="chat-fab" type="primary" circle size="large" @click="toggleChat">
      <el-icon size="24"><Service /></el-icon>
    </el-button>

    <transition name="el-zoom-in-bottom">
      <el-card v-show="isOpen" class="chat-window" shadow="always">
        <template #header>
          <div class="chat-header">
            <span class="chat-title">
              <el-icon><ChatDotRound /></el-icon>
              非遗智能助手
            </span>
            <div class="header-actions">
              <el-tooltip content="开始新对话">
                <el-icon class="action-icon" @click="clearHistory"><Delete /></el-icon>
              </el-tooltip>
              <el-icon class="action-icon" @click="isOpen = false"><Close /></el-icon>
            </div>
          </div>
        </template>

        <div ref="messageListRef" class="message-list">
          <div
            v-for="msg in displayMessages"
            :key="msg.id"
            class="message-item"
            :class="
              msg.role === 'user'
                ? 'message-user'
                : msg.role === 'system'
                  ? 'message-system'
                  : 'message-ai'
            "
          >
            <el-avatar
              :size="30"
              :src="msg.role === 'user' ? userAvatar : aiAvatar"
              class="msg-avatar"
            />

            <div class="msg-bubble">
              <div v-if="msg.role !== 'user'" class="ai-name">
                {{ msg.role === "system" ? "系统提醒" : "非遗小助手" }}
              </div>
              <div class="msg-content" v-html="renderContent(msg.content)"></div>
            </div>
          </div>

          <div v-if="isThinking" class="message-item message-ai">
            <el-avatar :size="30" :src="aiAvatar" class="msg-avatar" />
            <div class="msg-bubble">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="2"
            placeholder="请输入你的问题，Enter 发送，Shift + Enter 换行"
            resize="none"
            :disabled="isBusy"
            @keydown.enter.prevent="handleEnter"
          />
          <el-button class="send-btn" type="primary" size="small" :loading="isBusy" @click="sendMessage">
            发送
          </el-button>
        </div>
      </el-card>
    </transition>
  </div>
</template>

<script setup lang="ts">
import MarkdownIt from "markdown-it";
import { storeToRefs } from "pinia";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ChatDotRound, Close, Delete, Service } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { useChatWorkspaceStore } from "@/stores/chatWorkspace";
import type { ChatMessage } from "@/types/chat";
import { getCurrentUser, SESSION_CHANGED_EVENT, type SessionUser } from "@/utils/session";
import { buildStaticUrl } from "@/utils/url";

const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
});

const aiAvatar = MATERIAL_PLACEHOLDERS.aiAssistant;

const welcomeMessage: ChatMessage = {
  id: 0,
  role: "assistant",
  content:
    "你好，我是非遗智能助手。你可以问我项目背景、历史故事、代表技艺，或让我推荐值得继续浏览的非遗内容。",
  timestamp: new Date(0).toISOString(),
};

const route = useRoute();
const chatStore = useChatWorkspaceStore();
const { isBusy, isThinking, messages } = storeToRefs(chatStore);

const isOpen = ref(false);
const inputContent = ref("");
const messageListRef = ref<HTMLElement | null>(null);
const currentUser = ref<SessionUser | null>(getCurrentUser());

const showAssistant = computed(() => route.path !== "/login");
const displayMessages = computed(() => (messages.value.length > 0 ? messages.value : [welcomeMessage]));
const userAvatar = computed(
  () => buildStaticUrl(currentUser.value?.avatarUrl) || MATERIAL_PLACEHOLDERS.avatar
);

const refreshCurrentUser = () => {
  currentUser.value = getCurrentUser();
  chatStore.syncSessionScope();
};

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    scrollToBottom();
  }
};

const clearHistory = () => {
  chatStore.resetActiveConversation();
  inputContent.value = "";
  scrollToBottom();
};

const handleEnter = (event: KeyboardEvent) => {
  if (!event.shiftKey) {
    sendMessage();
  }
};

const sendMessage = async () => {
  const text = inputContent.value.trim();
  if (!text || isBusy.value) {
    return;
  }

  inputContent.value = "";
  scrollToBottom();

  try {
    await chatStore.sendMessage(text);
  } catch (error) {
    console.error("AI assistant error", error);
    const message =
      error instanceof Error && error.message ? error.message : "当前网络或模型服务暂时不可用，请稍后重试。";
    ElMessage.error(message);
  } finally {
    scrollToBottom();
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

const renderContent = (text: string) => markdown.render(text || "");

watch(
  () => [displayMessages.value.length, isOpen.value, isThinking.value],
  ([, open]) => {
    if (open) {
      scrollToBottom();
    }
  }
);

watch(showAssistant, (visible) => {
  if (!visible) {
    isOpen.value = false;
  }
});

onMounted(() => {
  refreshCurrentUser();
  window.addEventListener(SESSION_CHANGED_EVENT, refreshCurrentUser);
});

onBeforeUnmount(() => {
  window.removeEventListener(SESSION_CHANGED_EVENT, refreshCurrentUser);
});
</script>

<style scoped>
.chat-fab {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 2000;
  box-shadow: 0 4px 12px var(--heritage-shadow);
  transition: transform 0.3s;
}

.chat-fab:hover {
  transform: scale(1.1) rotate(15deg);
}

.chat-window {
  position: fixed;
  bottom: 90px;
  right: 30px;
  width: 380px;
  height: 550px;
  z-index: 2001;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  color: var(--heritage-ink);
}

.chat-title {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-icon {
  cursor: pointer;
  color: var(--heritage-muted);
  transition: color 0.3s;
}

.action-icon:hover {
  color: var(--heritage-primary);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  background-color: var(--heritage-paper-soft);
  display: flex;
  flex-direction: column;
  gap: 15px;
  height: 380px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-all;
  position: relative;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.msg-content :deep(p) {
  margin: 0 0 8px;
}

.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-ai,
.message-system {
  align-self: flex-start;
}

.message-ai .msg-bubble {
  background-color: var(--heritage-surface);
  color: var(--heritage-ink);
  border-top-left-radius: 0;
}

.message-system .msg-bubble {
  background: rgba(164, 59, 47, 0.08);
  border: 1px solid rgba(164, 59, 47, 0.16);
  color: var(--heritage-primary);
  border-top-left-radius: 0;
}

.ai-name {
  font-size: 12px;
  color: var(--heritage-muted);
  margin-bottom: 4px;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-user .msg-bubble {
  background: linear-gradient(135deg, var(--heritage-primary), #be6951);
  color: var(--heritage-paper-soft);
  border-top-right-radius: 0;
}

.chat-input-area {
  padding: 10px;
  border-top: 1px solid var(--heritage-border);
  background: var(--heritage-surface);
  position: relative;
}

.send-btn {
  position: absolute;
  bottom: 15px;
  right: 15px;
}

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: var(--heritage-gold);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
  margin: 0 2px;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0);
  }

  40% {
    transform: scale(1);
  }
}

.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background: rgba(164, 59, 47, 0.22);
  border-radius: 3px;
}
</style>
