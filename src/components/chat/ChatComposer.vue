<template>
  <div class="chat-input-container">
    <div class="quick-questions">
      <div class="questions-scroll">
        <button
          v-for="item in quickQuestions"
          :key="item.id"
          type="button"
          class="question-chip"
          :disabled="disabled"
          @click="$emit('select-suggestion', item.question)"
        >
          <span class="chip-emoji">{{ item.emoji }}</span>
          <span class="chip-text">{{ item.question }}</span>
        </button>
      </div>
    </div>

    <div class="input-wrapper">
      <div class="input-header">
        <span class="input-label">输入你的问题</span>
        <span class="input-hint">按 Enter 发送，Shift + Enter 换行</span>
      </div>
      <div class="input-body">
        <el-input
          :model-value="modelValue"
          placeholder="比如：什么是非物质文化遗产？"
          :disabled="disabled"
          resize="none"
          type="textarea"
          :rows="3"
          class="message-input"
          :class="{ 'has-content': modelValue.trim() }"
          @update:model-value="$emit('update:modelValue', String($event || ''))"
          @keyup.enter.exact.prevent="$emit('submit')"
        />
        <div class="input-actions">
          <div class="action-buttons">
            <el-button :disabled="!modelValue.trim() || disabled" @click="$emit('clear')">清空</el-button>
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!modelValue.trim() || disabled"
              @click="$emit('submit')"
            >
              发送消息
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string;
  disabled: boolean;
  loading: boolean;
  quickQuestions: Array<{
    id: number;
    question: string;
    emoji: string;
  }>;
}>();

defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "submit"): void;
  (e: "clear"): void;
  (e: "select-suggestion", value: string): void;
}>();
</script>

<style scoped>
.chat-input-container {
  padding: 16px 24px 24px;
  background: linear-gradient(180deg, var(--heritage-paper-soft) 0%, rgba(234, 220, 196, 0.56) 100%);
  border-top: 1px solid var(--heritage-border);
}

.quick-questions {
  margin-bottom: 16px;
  overflow: hidden;
}

.questions-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 8px 0;
}

.question-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.95) 0%, rgba(223, 188, 123, 0.16) 100%);
  border: 1px solid rgba(192, 138, 63, 0.24);
  border-radius: 20px;
  cursor: pointer;
  white-space: nowrap;
}

.question-chip:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.input-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--heritage-ink);
}

.input-hint {
  font-size: 12px;
  color: var(--heritage-muted);
}

.input-body {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  padding: 16px;
  border-radius: 12px;
  border: 2px solid rgba(192, 138, 63, 0.22);
  font-size: 15px;
  line-height: 1.6;
  resize: none;
}

.message-input.has-content :deep(.el-textarea__inner),
.message-input :deep(.el-textarea__inner):focus {
  border-color: var(--heritage-primary);
  box-shadow: 0 0 0 3px rgba(164, 59, 47, 0.12);
}

.action-buttons {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .chat-input-container {
    padding: 16px;
  }

  .input-body {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
