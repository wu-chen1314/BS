<script setup lang="ts">
import { defineAsyncComponent } from "vue";
import { RouterView } from "vue-router";

const ChatAssistant = defineAsyncComponent(() => import("./components/ChatAssistant.vue"));
</script>

<template>
  <RouterView />
  <ChatAssistant />
</template>

<style>
:root {
  color-scheme: light;
  --heritage-space-1: 8px;
  --heritage-space-2: 12px;
  --heritage-space-3: 16px;
  --heritage-space-4: 18px;
  --heritage-space-5: 24px;
  --heritage-space-6: 28px;
  --heritage-primary: #c0392b;
  /* 故宫红 deeper more vibrant red */
  --heritage-primary-deep: #922b21;
  --heritage-primary-soft: #e67e22;
  /* warm accent */
  --heritage-gold: #d4af37;
  /* classic gold */
  --heritage-gold-soft: #fcf3cf;
  --heritage-ink: #1c2833;
  /* darker elegant ink black */
  --heritage-ink-soft: #566573;
  --heritage-paper: #f8f6f0;
  --heritage-paper-deep: #eaeceb;
  --heritage-paper-soft: #ffffff;
  --heritage-surface: rgba(255, 255, 255, 0.95);
  --heritage-glass-bg: rgba(255, 255, 255, 0.75);
  --heritage-glass-border: rgba(255, 255, 255, 0.4);
  --heritage-glass-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.07);
  --heritage-border: rgba(113, 72, 44, 0.08);
  --heritage-shadow: rgba(0, 0, 0, 0.05);
  --heritage-card-shadow-rest: 0 10px 30px rgba(0, 0, 0, 0.04);
  --heritage-card-shadow-hover: 0 20px 40px rgba(0, 0, 0, 0.12);
  --heritage-card-shadow-active: 0 10px 20px rgba(0, 0, 0, 0.08);
  --heritage-card-translate-hover: -6px;
  --heritage-card-translate-active: -2px;
  --heritage-jade: #52be80;
  --heritage-jade-soft: #a9dfbf;
  --heritage-warning: #f39c12;
  --heritage-muted: #85929e;
  --el-color-primary: var(--heritage-primary);
  --el-color-primary-light-3: #d98880;
  --el-color-primary-light-5: #e6b0aa;
  --el-color-primary-light-7: #f2d7d5;
  --el-color-primary-light-8: #f9ebea;
  --el-color-primary-light-9: #fdf2e9;
  --el-color-primary-dark-2: var(--heritage-primary-deep);
  --el-color-success: var(--heritage-jade);
  --el-color-warning: var(--heritage-warning);
  --el-color-danger: var(--heritage-primary);
  --el-color-info: var(--heritage-ink-soft);
  --el-border-radius-base: 16px;
  --el-border-radius-round: 999px;
}

html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

body {
  background:
    radial-gradient(circle at top left, rgba(212, 175, 55, 0.1), transparent 30%),
    radial-gradient(circle at right 20%, rgba(192, 57, 43, 0.08), transparent 25%),
    linear-gradient(135deg, var(--heritage-paper-soft) 0%, var(--heritage-paper) 100%);
  background-attachment: fixed;
  color: var(--heritage-ink);
  font-family: "Inter", "Outfit", "Noto Serif SC", "Microsoft YaHei", "PingFang SC", sans-serif;
  letter-spacing: 0.015em;
  -webkit-font-smoothing: antialiased;
}

a {
  color: var(--heritage-primary);
}

button,
input,
textarea,
select {
  font: inherit;
}

::selection {
  background: rgba(164, 59, 47, 0.18);
  color: var(--heritage-ink);
}

.heritage-float-card {
  position: relative;
  transform: translate3d(0, 0, 0);
  transition:
    transform 260ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 260ms cubic-bezier(0.22, 1, 0.36, 1),
    border-color 220ms ease,
    background-color 220ms ease,
    filter 220ms ease;
}

.heritage-float-card::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(255, 250, 241, 0);
  transition: box-shadow 220ms ease;
}

.section-head,
.toolbar-actions,
.section-meta,
.hero-tags,
.workspace-meta,
.card-tags,
.summary-list,
.guide-list,
.filter-row,
.action-row {
  row-gap: var(--heritage-space-2);
}

.pagination-row {
  margin-top: var(--heritage-space-5);
}

.table-shell,
.collection-shell,
.news-column,
.chat-content,
.content-grid,
.workspace-grid {
  min-width: 0;
}

.el-button {
  min-height: 40px;
  padding-inline: 18px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.el-button [class*="el-icon"]+span,
.el-button span+[class*="el-icon"] {
  margin-left: 6px;
}

.el-button:not(.is-disabled):not(.is-circle):not(.el-button--text):not(.is-text):not(.is-link):hover {
  transform: translateY(-1px);
}

.el-button--primary:not(.is-plain) {
  border: none;
  box-shadow: 0 12px 24px rgba(164, 59, 47, 0.16);
}

.el-button--primary.is-plain,
.el-button.is-plain {
  background: rgba(255, 250, 241, 0.92);
  border-color: rgba(192, 138, 63, 0.28);
  color: var(--heritage-primary);
}

.el-button.is-link,
.el-button--text,
.el-button.is-text {
  padding-inline: 6px;
  min-height: auto;
  border-radius: 10px;
  color: var(--heritage-primary);
}

.el-empty {
  padding: 10px 0;
}

.el-empty__description {
  margin-top: 10px;
}

.el-empty__description p {
  color: var(--heritage-ink-soft);
  line-height: 1.7;
}

.el-empty__bottom {
  margin-top: 14px;
}

.el-table {
  --el-table-header-bg-color: rgba(248, 246, 240, 0.95);
  --el-table-row-hover-bg-color: rgba(255, 248, 238, 0.9);
  --el-table-border-color: rgba(113, 72, 44, 0.08);
  border-radius: 22px;
  overflow: hidden;
}

.el-table th.el-table__cell {
  padding-block: 14px;
  color: var(--heritage-ink);
  font-weight: 700;
}

.el-table td.el-table__cell {
  padding-block: 14px;
}

.el-table .cell {
  line-height: 1.7;
}

.el-card {
  border-radius: 24px;
}

.el-card__header {
  padding: 20px 24px 0;
}

.el-card__body {
  padding: 20px 24px 24px;
}

.el-dialog {
  border-radius: 24px;
}

.el-dialog__header {
  padding: 22px 24px 10px;
}

.el-dialog__body {
  padding: 12px 24px 22px;
}

.el-dialog__footer {
  padding: 0 24px 24px;
}

/* 退出登录确认弹窗样式 - 遵循行业通用 UI/UX 规范 */
.heritage-confirm-dialog {
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.heritage-confirm-dialog .el-message-box__header {
  padding: 20px 24px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.heritage-confirm-dialog .el-message-box__title {
  font-size: 17px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  letter-spacing: -0.01em;
}

.heritage-confirm-dialog .el-message-box__content {
  padding: 24px;
  background: #ffffff;
}

.heritage-confirm-dialog .el-message-box__message {
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  margin: 0;
}

.heritage-confirm-dialog .el-message-box__message p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.heritage-confirm-dialog .el-message-box__btns {
  padding: 16px 24px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  gap: 12px;
  justify-content: flex-end;
}

.heritage-confirm-dialog .el-button--primary {
  background: #dc2626;
  border: 1px solid #dc2626;
  color: #ffffff;
  font-weight: 500;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.heritage-confirm-dialog .el-button--primary:hover {
  background: #b91c1c;
  border-color: #b91c1c;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.heritage-confirm-dialog .el-button--primary:active {
  background: #991b1b;
}

.heritage-confirm-dialog .el-button--default {
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-weight: 500;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.heritage-confirm-dialog .el-button--default:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  color: #111827;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.heritage-confirm-dialog .el-button--default:active {
  background: #f3f4f6;
}

@media (hover: hover) and (prefers-reduced-motion: no-preference) {
  .heritage-float-card:hover {
    transform: translate3d(0, var(--heritage-card-translate-hover), 0);
    box-shadow: var(--heritage-card-shadow-hover);
    filter: saturate(1.03);
  }

  .heritage-float-card:hover::after {
    box-shadow: inset 0 0 0 1px rgba(255, 250, 241, 0.4);
  }

  .heritage-float-card:active {
    transform: translate3d(0, var(--heritage-card-translate-active), 0) scale(0.995);
    box-shadow: var(--heritage-card-shadow-active);
  }
}

@media (prefers-reduced-motion: reduce) {
  .heritage-float-card {
    transition: none;
  }

  .heritage-float-card:hover,
  .heritage-float-card:active {
    transform: none;
    filter: none;
  }
}
</style>
