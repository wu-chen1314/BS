<template>
  <section class="toolbar-card">
    <div class="toolbar-main">
      <div class="search-column">
        <div class="search-field" @focusin="$emit('toggle-history', true)" @focusout="handleFocusOut">
          <el-input
            :model-value="keyword"
            placeholder="搜索非遗项目、传承人或关键词"
            clearable
            size="large"
            @update:model-value="$emit('update:keyword', String($event || ''))"
            @clear="$emit('search')"
            @keyup.enter="$emit('search')"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div v-if="showHistory && history.length > 0" class="history-panel">
            <div class="history-header">
              <span>搜索历史</span>
              <el-button link type="primary" @click="$emit('clear-history')">清空</el-button>
            </div>
            <button
              v-for="item in history"
              :key="item"
              type="button"
              class="history-item"
              @mousedown.prevent="$emit('select-history', item)"
            >
              <el-icon><Clock /></el-icon>
              <span>{{ item }}</span>
            </button>
          </div>
        </div>

        <el-select
          :model-value="protectLevel"
          placeholder="按保护级别筛选"
          clearable
          size="large"
          class="level-select"
          @update:model-value="$emit('update:protect-level', String($event || ''))"
        >
          <el-option v-for="item in protectLevels" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>

      <div class="toolbar-actions">
        <el-button type="primary" size="large" @click="$emit('search')">开始搜索</el-button>
        <el-button size="large" @click="$emit('reset')">重置筛选</el-button>
      </div>
    </div>

    <div class="toolbar-side">
      <div class="toolbar-copy">
        <span class="copy-label">主题浏览</span>
        <h2>从项目、热度和传承线索快速进入非遗内容</h2>
        <p>首页采用轻量卡片流布局，同时保留搜索、收藏、评论、详情查看和后台维护能力。</p>
      </div>

      <div v-if="isAdmin" class="admin-actions">
        <el-button type="success" plain @click="$emit('open-add')">新增项目</el-button>
        <el-button :disabled="selectedCount === 0" type="danger" plain @click="$emit('batch-delete')">
          批量删除 ({{ selectedCount }})
        </el-button>
        <el-button type="warning" plain @click="$emit('export')">导出当前列表</el-button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Clock, Search } from "@element-plus/icons-vue";

defineProps<{
  keyword: string;
  protectLevel: string;
  history: string[];
  showHistory: boolean;
  isAdmin: boolean;
  selectedCount: number;
  protectLevels: Array<{ value: string; label: string }>;
}>();

const emit = defineEmits<{
  (e: "update:keyword", value: string): void;
  (e: "update:protect-level", value: string): void;
  (e: "toggle-history", value: boolean): void;
  (e: "search"): void;
  (e: "reset"): void;
  (e: "clear-history"): void;
  (e: "select-history", value: string): void;
  (e: "open-add"): void;
  (e: "batch-delete"): void;
  (e: "export"): void;
}>();

const handleFocusOut = () => {
  window.setTimeout(() => emit("toggle-history", false), 120);
};
</script>

<style scoped>
.toolbar-card {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  padding: 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(127, 38, 31, 0.98), rgba(34, 49, 63, 0.94));
  color: var(--heritage-paper-soft);
  box-shadow: 0 20px 40px rgba(72, 41, 28, 0.18);
}

.toolbar-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.search-column {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 14px;
}

.search-field {
  position: relative;
}

.history-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 10;
  border-radius: 18px;
  background: var(--heritage-paper-soft);
  color: var(--heritage-ink);
  border: 1px solid var(--heritage-border);
  box-shadow: 0 16px 32px var(--heritage-shadow);
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 14px;
  font-weight: 600;
}

.history-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease;
}

.history-item:hover {
  background: rgba(192, 138, 63, 0.12);
}

.toolbar-actions,
.admin-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
}

.copy-label {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(223, 188, 123, 0.18);
  font-size: 12px;
  letter-spacing: 1px;
}

.toolbar-copy h2 {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.25;
}

.toolbar-copy p {
  margin: 0;
  line-height: 1.7;
  color: rgba(255, 250, 241, 0.82);
}

.level-select :deep(.el-select__wrapper),
.search-field :deep(.el-input__wrapper) {
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.96);
  box-shadow: none;
}

@media (max-width: 1100px) {
  .toolbar-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .toolbar-card {
    padding: 18px;
  }

  .search-column {
    grid-template-columns: 1fr;
  }
}
</style>
