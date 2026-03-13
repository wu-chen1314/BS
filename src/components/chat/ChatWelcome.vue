<template>
  <div class="welcome-message">
    <div class="welcome-hero">
      <div class="welcome-icon">
        <div class="icon-container">AI</div>
      </div>
      <h1 class="welcome-title">欢迎使用非遗知识助手</h1>
      <p class="welcome-text">可以和我聊非遗项目、历史脉络、保护方式，以及展览、研学和路线策划思路。</p>
    </div>

    <div class="suggestion-section">
      <h3 class="section-title">你可以这样开始对话</h3>
      <div class="suggestion-grid">
        <button
          v-for="item in suggestions"
          :key="item.question"
          type="button"
          class="suggestion-card heritage-float-card"
          @click="$emit('select-suggestion', item.question)"
        >
          <div class="card-icon">{{ item.emoji }}</div>
          <div class="card-content">
            <h4>{{ item.question }}</h4>
            <p>{{ item.description }}</p>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  suggestions: Array<{
    question: string;
    emoji: string;
    description: string;
  }>;
}>();

defineEmits<{
  (e: "select-suggestion", question: string): void;
}>();
</script>

<style scoped>
.welcome-message {
  text-align: center;
  padding: 40px 20px;
}

.welcome-hero {
  margin-bottom: 36px;
}

.welcome-icon {
  margin-bottom: 20px;
}

.icon-container {
  width: 80px;
  height: 80px;
  margin: 0 auto;
  background: linear-gradient(135deg, rgba(164, 59, 47, 0.14) 0%, rgba(223, 188, 123, 0.24) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--heritage-primary);
  font-size: 32px;
  font-weight: 700;
  box-shadow: 0 8px 24px rgba(164, 59, 47, 0.18);
}

.welcome-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--heritage-ink);
  margin: 0 0 12px;
}

.welcome-text {
  font-size: 16px;
  color: var(--heritage-ink-soft);
  margin: 0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--heritage-ink);
  margin: 0 0 20px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.suggestion-card {
  padding: 20px;
  background: linear-gradient(135deg, var(--heritage-surface) 0%, rgba(223, 188, 123, 0.14) 100%);
  border: 1px solid rgba(192, 138, 63, 0.24);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  text-align: left;
}

.suggestion-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(164, 59, 47, 0.15);
  border-color: rgba(164, 59, 47, 0.3);
}

.card-icon {
  font-size: 28px;
}

.card-content h4 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--heritage-ink);
}

.card-content p {
  margin: 0;
  font-size: 13px;
  color: var(--heritage-ink-soft);
}
</style>
