<template>
  <el-dialog :model-value="visible" width="960px" top="4vh" destroy-on-close @close="$emit('update:visible', false)">
    <template #header>
      <div class="dialog-header">
        <div>
          <span class="dialog-kicker">项目详情</span>
          <h2>{{ project?.name || "非遗项目" }}</h2>
        </div>
        <div class="dialog-actions" v-if="project?.id && !isAdmin">
          <el-button :type="isFavorited ? 'warning' : 'primary'" plain @click="$emit('toggle-favorite')">
            {{ isFavorited ? "取消收藏" : "收藏项目" }}
          </el-button>
        </div>
      </div>
    </template>

    <div v-if="project" class="dialog-layout">
      <section class="hero-panel">
        <img :src="coverUrl" alt="" class="hero-cover" />
        <div class="hero-copy">
          <div class="hero-tags">
            <el-tag :type="levelType" effect="dark" round>{{ project.protectLevel || "未标注" }}</el-tag>
            <el-tag effect="plain">{{ categoryLabel }}</el-tag>
            <el-tag type="info" effect="plain">{{ project.regionName || "地区待补充" }}</el-tag>
          </div>
          <p class="hero-summary">{{ summary }}</p>

          <div class="detail-grid">
            <div class="detail-item">
              <span>传承人</span>
              <strong>{{ project.inheritorNames || "暂无记录" }}</strong>
            </div>
            <div class="detail-item">
              <span>传承状态</span>
              <strong>{{ project.status || "状态待补充" }}</strong>
            </div>
            <div class="detail-item">
              <span>浏览热度</span>
              <strong>{{ project.viewCount || 0 }} 次</strong>
            </div>
            <div class="detail-item">
              <span>开放时间</span>
              <strong>{{ project.openingHours || "待补充" }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section v-if="project.videoUrl" class="content-panel">
        <h3>影像资料</h3>
        <video :src="videoUrl" controls class="video-player"></video>
      </section>

      <section class="content-panel">
        <h3>历史与介绍</h3>
        <div class="rich-text" v-html="richText"></div>
      </section>

      <section class="content-panel comments-panel">
        <div class="comment-header">
          <div>
            <h3>评论互动</h3>
            <p>{{ comments.length }} 条留言</p>
          </div>
        </div>

        <div class="comment-composer">
          <el-input
            :model-value="commentDraft"
            type="textarea"
            :rows="3"
            placeholder="写下你对这个项目的看法或补充"
            @update:model-value="$emit('update:comment-draft', String($event || ''))"
          />
          <div class="comment-submit">
            <el-button type="primary" :loading="submittingComment" @click="$emit('submit-comment')">发布评论</el-button>
          </div>
        </div>

        <div class="comment-list">
            <el-empty v-if="comments.length === 0" description="当前还没有评论，欢迎留下第一条体验反馈" :image-size="80" />

          <article v-for="item in comments" :key="item.id" class="comment-item">
            <el-avatar :src="buildStaticUrl(item.avatarUrl) || fallbackAvatar" :size="42" />
            <div class="comment-content">
              <div class="comment-meta">
                <strong>{{ item.nickname || "匿名用户" }}</strong>
                <span>{{ item.createdAt || "" }}</span>
              </div>
              <p>{{ item.content }}</p>
            </div>
            <el-button v-if="canDeleteComment(item)" type="danger" link @click="$emit('delete-comment', item.id)">
              删除
            </el-button>
          </article>
        </div>
      </section>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import type { HeritageProject, ProjectComment } from "@/types/project";
import { getCategoryName, getProtectLevelType, summarizeRichText } from "@/utils/heritage";
import { sanitizeRichHtml } from "@/utils/html";
import { buildStaticUrl } from "@/utils/url";

const fallbackAvatar = MATERIAL_PLACEHOLDERS.avatar;
const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const props = defineProps<{
  visible: boolean;
  project: HeritageProject | null;
  comments: ProjectComment[];
  commentDraft: string;
  submittingComment: boolean;
  isAdmin: boolean;
  currentUserId?: number | null;
  isFavorited: boolean;
}>();

defineEmits<{
  (e: "update:visible", value: boolean): void;
  (e: "update:comment-draft", value: string): void;
  (e: "submit-comment"): void;
  (e: "delete-comment", id: number): void;
  (e: "toggle-favorite"): void;
}>();

const coverUrl = computed(() => buildStaticUrl(props.project?.coverUrl) || fallbackCover);
const videoUrl = computed(() => buildStaticUrl(props.project?.videoUrl));
const categoryLabel = computed(() => props.project?.categoryName || getCategoryName(props.project?.categoryId));
const levelType = computed(() => getProtectLevelType(props.project?.protectLevel || undefined));
const summary = computed(() => summarizeRichText(props.project?.history || props.project?.features, 160));
const richText = computed(
  () =>
    sanitizeRichHtml(
      props.project?.history,
      `<p>${summarizeRichText(props.project?.features, 180, "暂无详细介绍。")}</p>`
    )
);

const canDeleteComment = (comment: ProjectComment) =>
  props.isAdmin || (props.currentUserId != null && Number(props.currentUserId) === Number(comment.userId));
</script>

<style scoped>
.dialog-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.dialog-kicker {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(164, 59, 47, 0.1);
  color: var(--heritage-primary);
  font-size: 12px;
  margin-bottom: 8px;
}

.dialog-header h2 {
  margin: 0;
  font-size: 30px;
  color: var(--heritage-ink);
}

.dialog-layout {
  display: grid;
  gap: 20px;
}

.hero-panel,
.content-panel {
  border-radius: 24px;
  background: var(--heritage-surface);
}

.hero-panel {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
}

.hero-cover {
  width: 100%;
  height: 100%;
  min-height: 300px;
  object-fit: cover;
  border-radius: 22px;
}

.hero-copy {
  display: grid;
  gap: 18px;
  padding: 8px 8px 8px 0;
}

.hero-tags,
.detail-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-summary {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-muted);
}

.detail-item {
  min-width: 170px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(192, 138, 63, 0.08);
  display: grid;
  gap: 8px;
}

.detail-item span {
  color: var(--heritage-ink-soft);
  font-size: 12px;
}

.detail-item strong {
  color: var(--heritage-ink);
}

.content-panel {
  padding: 22px;
  box-shadow: 0 18px 40px var(--heritage-shadow);
}

.content-panel h3 {
  margin: 0 0 14px;
  font-size: 20px;
}

.video-player {
  width: 100%;
  max-height: 360px;
  border-radius: 18px;
  background: #000;
}

.rich-text {
  color: var(--heritage-ink);
  line-height: 1.85;
}

.rich-text :deep(p) {
  margin: 0 0 1em;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.comment-header p {
  margin: 6px 0 0;
  color: var(--heritage-ink-soft);
}

.comment-composer {
  display: grid;
  gap: 12px;
}

.comment-submit {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.comment-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: flex-start;
  padding: 16px;
  border-radius: 18px;
  background: rgba(192, 138, 63, 0.08);
}

.comment-content {
  min-width: 0;
}

.comment-content p {
  margin: 8px 0 0;
  color: var(--heritage-ink);
  line-height: 1.7;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--heritage-ink-soft);
  font-size: 13px;
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .hero-cover {
    min-height: 240px;
  }
}
</style>
