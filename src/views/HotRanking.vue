<template>
  <div class="ranking-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">HOT HERITAGE RANKING</p>
        <h1>非遗热度排行</h1>
        <p class="page-desc">
          热度页现在优先复用首页概览缓存，只在你主动刷新时强制拉新，切页时不会再整块闪烁。浏览量与首页项目详情、热榜和防刷冷却策略保持同一口径。
        </p>
        <div class="page-actions">
          <el-tag effect="plain" type="warning">{{ rankingSourceLabel }}</el-tag>
          <el-button round :loading="loading" @click="loadRanking(true)">刷新排行</el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <article class="metric-card heritage-float-card">
          <span>入榜项目</span>
          <strong>{{ rankingList.length }}</strong>
          <p>当前榜单收录的项目数量</p>
        </article>
        <article class="metric-card heritage-float-card">
          <span>总热度</span>
          <strong>{{ totalRankViews }}</strong>
          <p>榜单项目累计浏览总量</p>
        </article>
        <article class="metric-card heritage-float-card">
          <span>平均热度</span>
          <strong>{{ averageRankViews }}</strong>
          <p>用于快速判断整体关注度水平</p>
        </article>
      </div>
    </section>

    <section class="content-grid">
    <article class="podium-panel heritage-float-card" v-loading="loading" element-loading-text="正在加载热度排行...">
        <div class="section-head">
          <div>
            <p class="section-kicker">TOP 3</p>
            <h2>热度前三</h2>
          </div>
          <span class="section-note">点击项目可直接回到首页详情</span>
        </div>

        <el-alert
          v-if="errorMessage"
          class="section-alert"
          type="warning"
          :closable="false"
          show-icon
          :title="errorMessage"
        />

      <el-empty v-if="!loading && rankingList.length === 0" description="当前还没有热度排行数据" :image-size="120" />

        <div v-else class="podium-grid">
          <button
            v-for="(item, index) in topRanking"
            :key="item.id"
            type="button"
            class="podium-card heritage-float-card"
            :class="`place-${index + 1}`"
            @click="handleView(item.id)"
          >
            <div class="podium-badge">TOP {{ index + 1 }}</div>
            <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" class="podium-cover" />
            <div class="podium-copy">
              <h3>{{ item.name }}</h3>
              <p>{{ item.categoryName || "非遗项目" }}</p>
              <strong>{{ item.viewCount || 0 }} 次浏览</strong>
            </div>
          </button>
        </div>
      </article>

      <aside class="stream-panel heritage-float-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">RANK STREAM</p>
            <h2>榜单走势</h2>
          </div>
          <span class="section-note">{{ rankingSourceLabel }}</span>
        </div>

        <div v-if="remainingRanking.length > 0" class="stream-list">
          <button
            v-for="(item, index) in remainingRanking"
            :key="item.id"
            type="button"
            class="stream-item heritage-float-card"
            @click="handleView(item.id)"
          >
            <div class="stream-top">
              <span class="stream-rank">{{ index + 4 }}</span>
              <strong>{{ item.name }}</strong>
              <span class="stream-value">{{ item.viewCount || 0 }} 次</span>
            </div>
            <div class="stream-track">
              <div class="stream-fill" :style="{ width: calcWidth(Number(item.viewCount || 0), maxRankViews) }"></div>
            </div>
            <p>{{ item.categoryName || "非遗项目" }}</p>
          </button>
        </div>
          <el-empty v-else description="前三之外暂时没有更多项目" :image-size="86" />

        <div class="hint-card">
          <strong>热度说明</strong>
          <p>榜单与项目详情共用同一套浏览统计；登录用户每次主动浏览都会即时更新，首页和热度排行会同步重排。</p>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { useHeritageHubStore } from "@/stores/heritageHub";
import type { HeritageProject } from "@/types/project";
import { buildStaticUrl } from "@/utils/url";

interface RankingProject {
  id: number;
  name: string;
  coverUrl?: string | null;
  viewCount: number;
  categoryName?: string | null;
}

const router = useRouter();
const hubStore = useHeritageHubStore();
const { hotProjects, projects } = storeToRefs(hubStore);

const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const loading = ref(false);
const errorMessage = ref("");

const projectLookup = computed(
  () =>
    new Map(
      projects.value
        .filter((item): item is HeritageProject & { id: number } => Number.isFinite(Number(item.id)))
        .map((item) => [Number(item.id), item])
    )
);

const resolvedHotRanking = computed<RankingProject[]>(() =>
  hotProjects.value
    .map((item) => {
      const projectId = Number(item.id);
      if (!Number.isFinite(projectId) || projectId <= 0) {
        return null;
      }

      const projectDetail = projectLookup.value.get(projectId);
      return {
        id: projectId,
        name: projectDetail?.name || item.name || "未命名项目",
        coverUrl: projectDetail?.coverUrl || item.coverUrl || null,
        viewCount: Number(projectDetail?.viewCount ?? item.viewCount ?? 0),
        categoryName: projectDetail?.categoryName ?? null,
      };
    })
    .filter((item): item is RankingProject => Boolean(item))
    .slice(0, 20)
);

const fallbackRanking = computed<RankingProject[]>(() =>
  [...projects.value]
    .filter((item): item is HeritageProject & { id: number } => Number.isFinite(Number(item.id)))
    .sort((left, right) => Number(right.viewCount || 0) - Number(left.viewCount || 0))
    .slice(0, 20)
    .map((item) => ({
      id: Number(item.id),
      name: item.name,
      coverUrl: item.coverUrl,
      viewCount: Number(item.viewCount ?? 0),
      categoryName: item.categoryName ?? null,
    }))
);

const rankingList = computed(() =>
  resolvedHotRanking.value.length > 0 ? resolvedHotRanking.value : fallbackRanking.value
);
const topRanking = computed(() => rankingList.value.slice(0, 3));
const remainingRanking = computed(() => rankingList.value.slice(3));
const rankingSourceLabel = computed(() =>
  resolvedHotRanking.value.length > 0 ? "实时热榜" : "浏览量回退排序"
);
const totalRankViews = computed(() =>
  rankingList.value.reduce((sum, item) => sum + Number(item.viewCount || 0), 0)
);
const averageRankViews = computed(() =>
  rankingList.value.length > 0 ? Math.round(totalRankViews.value / rankingList.value.length) : 0
);
const maxRankViews = computed(() =>
  Math.max(...rankingList.value.map((item) => Number(item.viewCount || 0)), 1)
);

const calcWidth = (value: number, max: number) => `${Math.max((Number(value || 0) / max) * 100, 8)}%`;

const loadRanking = async (force = false) => {
  loading.value = true;
  errorMessage.value = "";
  try {
    await hubStore.ensureOverview({ force, hotLimit: 20, pageSize: 80 });
  } catch (error: any) {
    console.error("Failed to load hot ranking", error);
    errorMessage.value = error?.message || "热度排行加载失败，当前结果可能不是最新数据。";
    ElMessage.error("热度排行加载失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};

const handleView = (id?: number | null) => {
  if (!Number.isFinite(Number(id)) || Number(id) <= 0) {
    return;
  }
  router.push({ path: "/home", query: { id } });
};

onMounted(async () => {
  await loadRanking();
});
</script>

<style scoped>
.ranking-page {
  min-height: calc(100vh - 88px);
  display: grid;
  gap: 18px;
  padding: 12px 6px 28px;
}

.hero,
.podium-panel,
.stream-panel {
  border-radius: 26px;
  border: 1px solid var(--heritage-border);
  background: rgba(255, 251, 244, 0.92);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(192, 57, 43, 0.12), transparent 28%),
    linear-gradient(135deg, rgba(255, 249, 241, 0.96), rgba(255, 255, 255, 0.98));
}

.hero-copy,
.hero-metrics,
.podium-grid,
.stream-list {
  display: grid;
  gap: 14px;
}

.page-kicker,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--heritage-gold);
}

.hero-copy h1,
.section-head h2,
.podium-copy h3,
.stream-top strong {
  margin: 0;
  color: var(--heritage-ink);
}

.page-desc,
.podium-copy p,
.stream-item p,
.hint-card p {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.page-actions,
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.hero-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.metric-card {
  border-radius: 20px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.metric-card span,
.section-note,
.podium-copy p,
.stream-item p {
  color: var(--heritage-ink-soft);
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 34px;
  color: var(--heritage-primary);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(300px, 0.9fr);
  gap: 18px;
}

.podium-panel,
.stream-panel {
  padding: 24px;
}

.section-alert {
  margin: 16px 0;
}

.podium-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.podium-card {
  border: none;
  cursor: pointer;
  text-align: left;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(180deg, #fff, #fbf6ef);
  box-shadow: 0 18px 36px rgba(72, 41, 28, 0.08);
}

.podium-card.place-1 {
  background: linear-gradient(180deg, rgba(192, 57, 43, 0.14), rgba(255, 255, 255, 0.98));
}

.podium-card.place-2 {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.14), rgba(255, 255, 255, 0.98));
}

.podium-card.place-3 {
  background: linear-gradient(180deg, rgba(82, 190, 128, 0.14), rgba(255, 255, 255, 0.98));
}

.podium-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 16px 16px 0;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(28, 40, 51, 0.08);
  color: var(--heritage-primary);
  font-size: 12px;
  font-weight: 700;
}

.podium-cover {
  width: calc(100% - 32px);
  height: 180px;
  margin: 14px 16px 0;
  border-radius: 18px;
  object-fit: cover;
}

.podium-copy {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.podium-copy strong {
  color: var(--heritage-primary);
  font-size: 18px;
}

.stream-list {
  margin-top: 18px;
}

.stream-item {
  border: none;
  cursor: pointer;
  text-align: left;
  border-radius: 20px;
  padding: 16px;
  background: linear-gradient(180deg, #fff, #fbf6ef);
}

.stream-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stream-rank {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(192, 138, 63, 0.12);
  color: var(--heritage-primary);
  font-weight: 700;
}

.stream-value {
  margin-left: auto;
  color: var(--heritage-primary);
  white-space: nowrap;
}

.stream-track {
  height: 10px;
  margin: 12px 0 10px;
  border-radius: 999px;
  background: rgba(28, 40, 51, 0.08);
  overflow: hidden;
}

.stream-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--heritage-primary), var(--heritage-gold));
}

.hint-card {
  margin-top: 18px;
  border-radius: 20px;
  padding: 18px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.hint-card strong {
  display: block;
  margin-bottom: 8px;
  color: var(--heritage-ink);
}

@media (max-width: 1100px) {
  .hero,
  .content-grid,
  .hero-metrics,
  .podium-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .ranking-page {
    padding: 10px 0 20px;
  }

  .hero,
  .podium-panel,
  .stream-panel {
    padding: 18px;
  }

  .page-actions,
  .section-head,
  .stream-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .podium-cover {
    width: 100%;
    margin: 14px 0 0;
  }

  .podium-badge {
    margin-left: 0;
  }

  .stream-value {
    margin-left: 0;
  }
}
</style>
