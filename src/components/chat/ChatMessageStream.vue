<template>
  <div ref="historyRef" class="chat-history">
    <ChatWelcome
      v-if="messages.length === 0"
      :suggestions="welcomeSuggestions"
      @select-suggestion="$emit('select-suggestion', $event)"
    />

    <div
      v-for="(message, index) in messages"
      :key="message.id || index"
      :class="[
        'message-item',
        message.role === 'user' ? 'user-message' : message.role === 'system' ? 'system-message' : 'ai-message',
      ]"
    >
      <div class="message-avatar">
        <el-avatar
          :size="44"
          :src="message.role === 'user' ? userAvatar : message.role === 'assistant' ? aiAvatar : ''"
        />
        <div class="avatar-badge">{{ message.role === "user" ? "我" : message.role === "system" ? "系统" : "AI" }}</div>
      </div>

      <div class="message-content">
        <div v-if="message.isThinking" class="thinking-bubble">
          <div class="wave-dots">
            <span></span><span></span><span></span><span></span><span></span>
          </div>
          <span class="thinking-text">AI 正在整理回答，请稍候...</span>
        </div>

        <template v-else>
          <div class="message-bubble">
            <div class="message-text" v-html="renderMarkdown(message.content)"></div>
          </div>

          <div v-if="message.role === 'assistant'" class="message-actions">
            <el-button size="small" text @click="$emit('copy', message.content)">复制</el-button>
            <el-button size="small" text @click="$emit('regenerate', index)">重新生成</el-button>
          </div>

          <div class="message-meta">
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            <span v-if="message.role === 'user' && message.status" class="message-status">
              {{ userStatusLabel(message.status) }}
            </span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownIt from "markdown-it";
import { computed, nextTick, ref, watch } from "vue";
import type { ChatMessage } from "@/types/chat";
import ChatWelcome from "./ChatWelcome.vue";

const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
});

const props = defineProps<{
  messages: ChatMessage[];
  aiAvatar: string;
  userAvatar: string;
  suggestions: Array<{
    question: string;
    emoji: string;
    description: string;
  }>;
}>();

defineEmits<{
  (e: "copy", content: string): void;
  (e: "regenerate", index: number): void;
  (e: "select-suggestion", question: string): void;
}>();

const historyRef = ref<HTMLElement>();
const welcomeSuggestions = computed(() => props.suggestions);

const renderMarkdown = (content: string) => markdown.render(content || "");

const formatTime = (timestamp: string | Date) => {
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
};

const userStatusLabel = (status: string) => {
  if (status === "sending") return "发送中...";
  if (status === "error") return "发送失败";
  return "已发送";
};

const scrollToBottom = async () => {
  await nextTick();
  const el = historyRef.value;
  if (!el) {
    return;
  }
  el.scrollTop = el.scrollHeight;
};

watch(
  () => props.messages.length,
  () => {
    scrollToBottom();
  }
);

defineExpose({
  scrollToBottom,
});
</script>

<style scoped>
.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, var(--heritage-paper-soft) 0%, rgba(255, 251, 244, 0.92) 100%);
}

.message-item {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  position: relative;
  flex-shrink: 0;
}

.avatar-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, var(--heritage-primary) 0%, var(--heritage-gold) 100%);
  color: var(--heritage-paper-soft);
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 600;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.6;
  box-shadow: 0 2px 12px rgba(72, 41, 28, 0.08);
  word-wrap: break-word;
}

.ai-message .message-bubble {
  background: linear-gradient(135deg, var(--heritage-surface) 0%, rgba(234, 220, 196, 0.5) 100%);
  border: 1px solid var(--heritage-border);
  color: var(--heritage-ink);
}

.user-message .message-bubble {
  background: linear-gradient(135deg, var(--heritage-primary) 0%, #be6951 100%);
  color: var(--heritage-paper-soft);
}

.system-message .message-bubble {
  background: linear-gradient(135deg, rgba(223, 188, 123, 0.24) 0%, rgba(255, 250, 241, 0.95) 100%);
  border: 1px solid rgba(192, 138, 63, 0.35);
  color: #78542d;
}

.message-text {
  font-size: 15px;
  line-height: 1.7;
}

.message-text :deep(p) {
  margin: 0 0 12px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(pre) {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(34, 49, 63, 0.08);
}

.message-text :deep(code) {
  font-family: "Cascadia Code", "JetBrains Mono", Consolas, monospace;
}

.message-actions,
.message-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.message-meta {
  font-size: 12px;
  color: var(--heritage-muted);
}

.message-status {
  font-weight: 500;
}

.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.9) 0%, rgba(234, 220, 196, 0.55) 100%);
  border-radius: 16px;
  border: 1px solid var(--heritage-border);
}

.wave-dots {
  display: flex;
  gap: 6px;
}

.wave-dots span {
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, var(--heritage-primary) 0%, var(--heritage-gold) 100%);
  border-radius: 50%;
  animation: wave 1.5s ease-in-out infinite;
}

.wave-dots span:nth-child(2) {
  animation-delay: 0.1s;
}

.wave-dots span:nth-child(3) {
  animation-delay: 0.2s;
}

.wave-dots span:nth-child(4) {
  animation-delay: 0.3s;
}

.wave-dots span:nth-child(5) {
  animation-delay: 0.4s;
}

@keyframes wave {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

@media (max-width: 768px) {
  .chat-history {
    padding: 16px;
  }

  .message-content {
    max-width: 80%;
  }
}
</style>
