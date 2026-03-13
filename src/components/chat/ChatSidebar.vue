<template>
  <aside class="chat-sidebar">
    <div class="sidebar-header">
      <div class="header-left">
        <el-icon class="sidebar-icon"><ChatDotRound /></el-icon>
        <h3 class="sidebar-title">会话记录</h3>
      </div>

      <div class="header-actions">
        <el-button
          v-if="!isBatchMode"
          type="success"
          size="small"
          :icon="Edit"
          :disabled="loading"
          @click="emit('toggle-batch-mode')"
        />
        <template v-else>
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            :disabled="selectedIds.length === 0 || loading"
            @click="emit('batch-delete')"
          >
            删除 ({{ selectedIds.length }})
          </el-button>
          <el-button size="small" :disabled="loading" @click="emit('toggle-batch-mode')">取消</el-button>
        </template>
      </div>

      <el-button
        v-if="!isBatchMode"
        type="primary"
        size="small"
        :icon="Plus"
        circle
        :disabled="loading"
        @click="emit('create-chat')"
      />
    </div>

    <div class="chat-list">
      <div v-if="errorMessage" class="sidebar-state sidebar-error">{{ errorMessage }}</div>
      <div v-else-if="loading" class="sidebar-state">正在加载会话列表...</div>
      <div v-else-if="sessions.length === 0" class="empty-chat">
        <el-empty description="当前还没有会话记录，点击右上角开始第一段对话" :image-size="60" />
      </div>

      <button
        v-for="session in sessions"
        v-else
        :key="session.id"
        type="button"
        class="chat-item heritage-float-card"
        :class="{ active: activeId === session.id, selected: selectedIds.includes(session.id) }"
        @click="handleSelect(session.id)"
      >
        <div class="chat-item-left">
          <div v-if="isBatchMode" class="chat-item-checkbox">
            <input
              type="checkbox"
              :checked="selectedIds.includes(session.id)"
              @click.stop="emit('toggle-select', session.id)"
            />
          </div>
          <div v-else class="chat-item-icon">
            <el-icon><ChatLineRound /></el-icon>
          </div>

          <div class="chat-item-content">
            <div class="chat-item-title">{{ session.title || "新对话" }}</div>
            <div class="chat-item-preview">{{ session.preview || "点击继续查看历史消息" }}</div>
          </div>
        </div>

        <div class="chat-item-right">
          <div class="chat-item-time">{{ session.updatedLabel }}</div>
          <el-button
            v-if="!isBatchMode"
            type="danger"
            size="small"
            text
            :icon="Delete"
            @click.stop="emit('delete-chat', session.id)"
          />
        </div>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ChatDotRound, ChatLineRound, Delete, Edit, Plus } from "@element-plus/icons-vue";

export interface ChatSidebarSession {
  id: number;
  title?: string;
  preview?: string;
  updatedLabel?: string;
}

const props = defineProps<{
  sessions: ChatSidebarSession[];
  activeId: number | null;
  isBatchMode: boolean;
  selectedIds: number[];
  loading?: boolean;
  errorMessage?: string;
}>();

const emit = defineEmits<{
  (e: "create-chat"): void;
  (e: "delete-chat", id: number): void;
  (e: "toggle-batch-mode"): void;
  (e: "toggle-select", id: number): void;
  (e: "batch-delete"): void;
  (e: "select-chat", id: number): void;
}>();

const handleSelect = (id: number) => {
  if (props.isBatchMode) {
    emit("toggle-select", id);
    return;
  }
  emit("select-chat", id);
};
</script>

<style scoped>
.chat-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  background: var(--heritage-surface);
  border-radius: 16px;
  box-shadow: 0 8px 32px var(--heritage-shadow);
  overflow: hidden;
  border: 1px solid var(--heritage-border);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--heritage-border);
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, var(--heritage-paper-soft) 0%, rgba(234, 220, 196, 0.7) 100%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.sidebar-icon {
  font-size: 20px;
  color: var(--heritage-primary);
}

.sidebar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--heritage-ink);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sidebar-state {
  padding: 18px 14px;
  border-radius: 12px;
  background: rgba(192, 138, 63, 0.08);
  color: var(--heritage-ink-soft);
  text-align: center;
}

.sidebar-error {
  background: rgba(164, 59, 47, 0.08);
  color: var(--heritage-primary);
}

.empty-chat {
  text-align: center;
  padding: 40px 20px;
}

.chat-item {
  width: 100%;
  border: 1px solid transparent;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.chat-item:hover {
  background: linear-gradient(135deg, rgba(192, 138, 63, 0.12) 0%, rgba(255, 250, 241, 0.95) 100%);
  border-color: rgba(192, 138, 63, 0.24);
}

.chat-item.active {
  background: linear-gradient(135deg, rgba(164, 59, 47, 0.14) 0%, rgba(223, 188, 123, 0.18) 100%);
  border-color: rgba(164, 59, 47, 0.28);
}

.chat-item.selected {
  border-left: 3px solid var(--heritage-primary);
}

.chat-item-left {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.chat-item-icon,
.chat-item-checkbox {
  width: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-item-icon {
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(164, 59, 47, 0.14) 0%, rgba(223, 188, 123, 0.2) 100%);
  color: var(--heritage-primary);
  font-size: 20px;
}

.chat-item-content {
  flex: 1;
  min-width: 0;
}

.chat-item-title {
  font-weight: 600;
  color: var(--heritage-ink);
  margin-bottom: 4px;
}

.chat-item-preview {
  font-size: 13px;
  color: var(--heritage-ink-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  margin-left: 12px;
}

.chat-item-time {
  font-size: 12px;
  color: var(--heritage-muted);
}

@media (max-width: 768px) {
  .chat-sidebar {
    width: 100%;
  }
}
</style>
