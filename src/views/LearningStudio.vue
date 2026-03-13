<template>
  <div class="learning-page">
    <section class="hero-section" v-loading="loading" element-loading-text="正在加载研学工作台...">
      <div class="hero-copy heritage-float-card">
        <p class="hero-kicker">HERITAGE LEARNING STUDIO</p>
        <h1>研学工坊</h1>
        <p class="hero-desc">
          把当前非遗项目、地区热度和主题策展联动成可收藏、可导出、可带入路线的研学方案。
          页面会根据不同使用场景自动组织活动模块、教师活动单与路线预设。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="goToRoute">
            一键带入路线
          </el-button>
          <el-button size="large" round @click="goLinkedCuration">联动主题策展</el-button>
          <el-button size="large" round @click="goRegionCategory">联动地区分类</el-button>
          <el-button size="large" round @click="goHome">返回项目总览</el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <article class="metric-card heritage-float-card">
          <span>研学模板</span>
          <strong>{{ LEARNING_TRACKS.length }}</strong>
          <p>按课堂、社区、亲子三类场景配置</p>
        </article>
        <article class="metric-card heritage-float-card">
          <span>方案项目</span>
          <strong>{{ activePlan.projectCount }}</strong>
          <p>当前方案中被优先纳入的重点项目</p>
        </article>
        <article class="metric-card heritage-float-card">
          <span>方案收藏</span>
          <strong>{{ analyticsSummary.favoriteCount }}</strong>
          <p>研学方案累计收藏量</p>
        </article>
        <article class="metric-card heritage-float-card">
          <span>路线带入</span>
          <strong>{{ analyticsSummary.routeCarryCount }}</strong>
          <p>由研学工坊跳转到路线规划的次数</p>
        </article>
      </div>
    </section>

    <section class="track-switcher heritage-float-card">
      <div class="track-switcher-copy">
        <p class="section-kicker">TRACK SWITCHER</p>
        <h2>选择研学场景</h2>
        <p>切换后会保留联动关系，但不再重复刷整页数据和统计面板。</p>
      </div>
      <div class="track-chip-grid">
        <button
          v-for="track in LEARNING_TRACKS"
          :key="track.id"
          type="button"
          class="track-chip heritage-float-card"
          :class="{ active: track.id === selectedTrackId }"
          @click="changeTrack(track.id)"
        >
          <span>{{ track.title }}</span>
          <small>{{ track.subtitle }}</small>
        </button>
      </div>
    </section>

    <section class="content-grid">
      <div class="main-column">
        <el-card class="plan-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head">
              <div>
                <p class="section-kicker">LEARNING BLUEPRINT</p>
                <h2>{{ activeTrack.title }}</h2>
                <p class="section-desc">{{ activeTrack.subtitle }}</p>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :type="isFavorited ? 'warning' : 'primary'"
                  plain
                  :loading="favoriteBusy"
                  @click="toggleFavorite"
                >
                  <el-icon><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
                  {{ isFavorited ? "已收藏方案" : "收藏方案" }}
                </el-button>
                <el-button plain @click="exportPlan">
                  <el-icon><Document /></el-icon>
                  导出方案
                </el-button>
                <el-button plain @click="downloadPlanFile">
                  <el-icon><Download /></el-icon>
                  下载策划稿
                </el-button>
                <el-button plain @click="exportTeacherSheet">
                  <el-icon><Files /></el-icon>
                  教师活动单
                </el-button>
                <el-button plain @click="goToRoute">
                  <el-icon><Position /></el-icon>
                  一键带入路线
                </el-button>
              </div>
            </div>
          </template>

          <div class="plan-summary">
            <p>{{ activeTrack.goal }}</p>
            <div class="plan-tags">
              <span class="plan-tag">适用对象：{{ activeTrack.audience }}</span>
              <span class="plan-tag">建议时长：{{ activeTrack.duration }}</span>
              <span class="plan-tag">重点地区：{{ activePlan.highlightedRegion }}</span>
              <span class="plan-tag">关键词：{{ activeTrack.keywords.join(" / ") }}</span>
            </div>
          </div>

          <div class="integration-grid">
            <article class="integration-item heritage-float-card">
              <span>联动主题策展</span>
              <strong>{{ activePlan.linkedThemeTitle }}</strong>
              <p>{{ activePlan.linkedThemeSubtitle }}</p>
            </article>
            <article class="integration-item heritage-float-card">
              <span>路线预设</span>
              <strong>{{ linkedTrailLabel }}</strong>
              <p>{{ linkedTrailHint }}</p>
            </article>
            <article class="integration-item heritage-float-card">
              <span>方案摘要</span>
              <strong>{{ activePlan.title }}</strong>
              <p>{{ planSummary }}</p>
            </article>
          </div>

          <div class="module-grid">
            <article v-for="module in activePlan.modules" :key="module.title" class="module-card heritage-float-card">
              <p class="module-duration">{{ module.duration }}</p>
              <h3>{{ module.title }}</h3>
              <p class="module-objective">{{ module.objective }}</p>
              <div class="module-columns">
                <div>
                  <strong>活动安排</strong>
                  <ul>
                    <li v-for="item in module.activities" :key="item">{{ item }}</li>
                  </ul>
                </div>
                <div>
                  <strong>产出要求</strong>
                  <ul>
                    <li v-for="item in module.outputs" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </article>
          </div>
        </el-card>

        <el-card class="plan-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head">
              <div>
                <p class="section-kicker">PROJECT FOCUS</p>
                <h2>推荐项目</h2>
                <p class="section-desc">按当前研学模板筛出的优先项目，可直接进入详情或带入路线。</p>
              </div>
              <el-tag effect="dark" type="danger">{{ activeProjects.length }} 个重点项目</el-tag>
            </div>
          </template>

          <div class="project-grid">
            <article
              v-for="project in activeProjects"
              :key="project.id"
              class="project-card heritage-float-card"
              @click="goDetail(Number(project.id))"
            >
              <img :src="buildStaticUrl(project.coverUrl) || fallbackCover" :alt="project.name" class="project-cover" />
              <div class="project-body">
                <div class="project-meta">
                  <el-tag size="small" effect="plain">{{ project.protectLevel || "未定级" }}</el-tag>
                  <span>{{ project.regionName || "地区待补充" }}</span>
                </div>
                <h3>{{ project.name }}</h3>
                <p>{{ summarizeRichText(project.history || project.features, 90) }}</p>
                <div class="project-footer">
                  <strong>{{ getCategoryName(project.categoryId) }}</strong>
                  <span>{{ project.inheritorNames || "传承人信息待补充" }}</span>
                </div>
              </div>
            </article>
          </div>

          <el-empty
            v-if="!activeProjects.length"
            description="当前研学模板下还没有推荐项目"
            :image-size="120"
          />
        </el-card>
      </div>

      <div class="side-column">
        <el-card class="action-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">DELIVERABLES</p>
                <h2>方案成果</h2>
              </div>
            </div>
          </template>

          <div class="deliverable-list">
            <div v-for="item in activeTrack.deliverables" :key="item" class="deliverable-item heritage-float-card">
              <strong>{{ item }}</strong>
              <p>可直接结合当前项目与路线预设组织内容产出。</p>
            </div>
          </div>
        </el-card>

        <el-card class="action-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">TEACHER SHEET</p>
                <h2>教师活动单</h2>
              </div>
            </div>
          </template>

          <div class="sheet-summary">
            <p>{{ activePlan.teacherSheet.summary }}</p>
            <div class="plan-tags">
              <span class="plan-tag">对象：{{ activePlan.teacherSheet.targetAudience }}</span>
              <span class="plan-tag">时长：{{ activePlan.teacherSheet.suggestedDuration }}</span>
            </div>
            <div class="sheet-actions">
              <el-button type="primary" plain @click="exportTeacherSheet">打印活动单</el-button>
              <el-button plain @click="downloadTeacherSheetFile">下载活动单</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="action-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">ANALYTICS</p>
                <h2>研学统计</h2>
              </div>
              <el-icon><DataAnalysis /></el-icon>
            </div>
          </template>

          <div class="stats-grid">
            <div class="stat-chip heritage-float-card"><span>访问量</span><strong>{{ analyticsSummary.viewCount }}</strong></div>
            <div class="stat-chip heritage-float-card"><span>导出量</span><strong>{{ analyticsSummary.exportCount }}</strong></div>
            <div class="stat-chip heritage-float-card"><span>活动单导出</span><strong>{{ analyticsSummary.sheetExportCount }}</strong></div>
            <div class="stat-chip heritage-float-card"><span>路线带入</span><strong>{{ analyticsSummary.routeCarryCount }}</strong></div>
          </div>

          <div class="sidebar-group">
            <strong>热门方案</strong>
            <button
              v-for="item in analyticsSummary.topPlans.slice(0, 5)"
              :key="`${item.planId}-${item.trackId}`"
              type="button"
              class="list-item heritage-float-card"
              @click="restoreTopPlan(item)"
            >
              <span>{{ item.planTitle || item.planId || "未命名方案" }}</span>
              <strong>{{ item.value }}</strong>
            </button>
          </div>

          <div class="sidebar-group">
            <strong>偏好关键词</strong>
            <div class="keyword-list">
              <span
                v-for="item in analyticsSummary.keywordPreferences.slice(0, 6)"
                :key="item.name"
                class="keyword-pill"
              >
                {{ item.name }} / {{ item.value }}
              </span>
            </div>
          </div>
        </el-card>

        <el-card class="action-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">MY FAVORITES</p>
                <h2>我的方案收藏</h2>
              </div>
              <el-icon><CollectionTag /></el-icon>
            </div>
          </template>

          <el-empty
            v-if="favoritePlans.length === 0"
            description="当前还没有收藏研学方案"
            :image-size="90"
          />
          <div v-else class="sidebar-group">
            <button
              v-for="item in favoritePlans.slice(0, 6)"
              :key="item.id"
              type="button"
              class="list-item heritage-float-card"
              @click="restoreFavorite(item)"
            >
              <div>
                <strong>{{ item.planTitle || item.planId }}</strong>
                <p>{{ item.trackId || "未绑定模板" }}</p>
              </div>
              <span>{{ formatFavoriteDate(item.createdAt) }}</span>
            </button>
          </div>
        </el-card>

        <el-card class="action-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">HOT REGIONS</p>
                <h2>推荐热区</h2>
              </div>
              <el-icon><Location /></el-icon>
            </div>
          </template>

          <div class="sidebar-group">
            <button
              v-for="item in topRegions"
              :key="item.name"
              type="button"
              class="list-item heritage-float-card"
              @click="goRegionTrail(item.name)"
            >
              <span>{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </button>
          </div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";
import {
  CollectionTag,
  DataAnalysis,
  Download,
  Document,
  Files,
  Location,
  Position,
  Star,
  StarFilled,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { CURATION_THEMES, LEARNING_TRACKS } from "@/constants/heritage";
import { useHeritageOverview } from "@/composables/useHeritageOverview";
import { useHeritageHubStore } from "@/stores/heritageHub";
import {
  checkLearningPlanFavorite,
  recordLearningPlanAnalytics,
  toggleLearningPlanFavorite,
} from "@/services/learning";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import type { LearningPlanFavoriteRecord, LearningPlanStatisticItem } from "@/types/learning";
import { exportLearningPlanPdf, exportTeacherSheetPdf, downloadLearningPlan, downloadTeacherSheet } from "@/utils/learning-export";
import { buildLearningRouteQuery, createLearningPlan, parseLearningPlanId, summarizeLearningPlan } from "@/utils/learning";
import { showSuccess, successText } from "@/utils/uiFeedback";
import { buildStaticUrl } from "@/utils/url";
import { filterProjectsByCategories, getCategoryName, sortRegionsByValue, summarizeRichText } from "@/utils/heritage";

const router = useRouter();
const route = useRoute();
const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const selectedTrackId = ref(LEARNING_TRACKS[0].id);
const favoriteBusy = ref(false);
const isFavorited = ref(false);
const bootLoading = ref(true);
const hasRecordedInitialView = ref(false);
const hubStore = useHeritageHubStore();
const { learningFavorites: favoritePlans, learningAnalyticsSummary: analyticsSummary } = storeToRefs(hubStore);

const { projects, regionStats, loading: overviewLoading, load } = useHeritageOverview();
const loading = computed(() => bootLoading.value || overviewLoading.value);
const activeTrack = computed(
  () => LEARNING_TRACKS.find((item) => item.id === selectedTrackId.value) || LEARNING_TRACKS[0]
);
const activeProjects = computed(() =>
  filterProjectsByCategories(projects.value, activeTrack.value.categoryIds).slice(0, 6)
);
const topRegions = computed(() => sortRegionsByValue(regionStats.value).slice(0, 5));
const activePlan = computed(() => createLearningPlan(activeTrack.value, activeProjects.value, topRegions.value));
const planSummary = computed(() => summarizeLearningPlan(activePlan.value));
const linkedTheme = computed(
  () => CURATION_THEMES.find((item) => item.id === activeTrack.value.linkedThemeId) || CURATION_THEMES[0]
);
const linkedTrailQuery = computed(() => buildLearningRouteQuery(activeTrack.value, activePlan.value.highlightedRegion));
const linkedTrailLabel = computed(() => {
  const transportLabel =
    activeTrack.value.trailPreset.transportMode === "walk"
      ? "步行"
      : activeTrack.value.trailPreset.transportMode === "car"
        ? "自驾"
        : "公共交通";
  return `${activeTrack.value.trailPreset.maxStops} 个点位 / ${transportLabel}`;
});
const linkedTrailHint = computed(
  () =>
    `建议 ${activeTrack.value.trailPreset.durationKey} 内完成，预算 ${activeTrack.value.trailPreset.budgetLevel} 档，${activeTrack.value.trailPreset.preferHot ? "优先热点项目" : "均衡探索项目"}`
);

const applyTrackQuery = (value: unknown) => {
  if (typeof value === "string" && LEARNING_TRACKS.some((item) => item.id === value)) {
    selectedTrackId.value = value;
    return;
  }

  selectedTrackId.value = LEARNING_TRACKS[0].id;
};

const syncTrackQuery = async (trackId: string) => {
  if (route.query.track === trackId) {
    return;
  }
  await router.replace({ path: route.path, query: { ...route.query, track: trackId } });
};

const loadSidebarData = async (force = false) => hubStore.syncLearningState(force);

const refreshFavoriteState = async () => {
  try {
    const res = await checkLearningPlanFavorite(activePlan.value.id);
    isFavorited.value = Boolean(res.data.data);
  } catch (error) {
    console.error("Failed to refresh learning favorite state", error);
  }
};

const recordCurrentPlan = async (
  actionType: "view" | "routeCarry" | "export" | "teacherSheet",
  payload: Record<string, unknown> = {}
) => {
  try {
    await recordLearningPlanAnalytics({
      planId: activePlan.value.id,
      planTitle: activePlan.value.title,
      trackId: activeTrack.value.id,
      actionType,
      audienceTag: activeTrack.value.audience,
      durationLabel: activeTrack.value.duration,
      linkedThemeId: linkedTheme.value.id,
      regionKeyword: activePlan.value.highlightedRegion,
      projectCount: activePlan.value.projectCount,
      keywords: activePlan.value.keywords,
      payload,
    });
  } catch (error) {
    console.error("Failed to record learning plan analytics", error);
  }
};

const changeTrack = async (trackId: string) => {
  if (selectedTrackId.value === trackId) {
    return;
  }
  selectedTrackId.value = trackId;
  await syncTrackQuery(trackId);
};

const toggleFavorite = async () => {
  favoriteBusy.value = true;
  try {
    const res = await toggleLearningPlanFavorite({
      planId: activePlan.value.id,
      planTitle: activePlan.value.title,
      trackId: activeTrack.value.id,
    });
    isFavorited.value = Boolean(res.data.data);
    await loadSidebarData(true);
    showSuccess(isFavorited.value ? "已收藏当前研学方案" : "已取消收藏当前研学方案");
  } catch (error) {
    console.error("Failed to toggle learning plan favorite", error);
  } finally {
    favoriteBusy.value = false;
  }
};

const exportPlan = async () => {
  try {
    exportLearningPlanPdf(activePlan.value);
    await recordCurrentPlan("export", { format: "pdf-print" });
    await loadSidebarData(true);
    showSuccess("研学方案打印页已打开，可直接另存为 PDF");
  } catch (error) {
    console.error("Failed to export learning plan", error);
    ElMessage.error(error instanceof Error ? error.message : "导出研学方案失败");
  }
};

const downloadPlanFile = async () => {
  try {
    downloadLearningPlan(activePlan.value);
    await recordCurrentPlan("export", { format: "markdown" });
    await loadSidebarData(true);
    showSuccess(successText.downloaded("研学策划稿"));
  } catch (error) {
    console.error("Failed to download learning plan", error);
    ElMessage.error("下载研学策划稿失败");
  }
};

const exportTeacherSheet = async () => {
  try {
    exportTeacherSheetPdf(activePlan.value.teacherSheet);
    await recordCurrentPlan("teacherSheet", { format: "pdf-print" });
    await loadSidebarData(true);
    showSuccess("教师活动单打印页已打开");
  } catch (error) {
    console.error("Failed to export teacher sheet", error);
    ElMessage.error(error instanceof Error ? error.message : "导出教师活动单失败");
  }
};

const downloadTeacherSheetFile = async () => {
  try {
    downloadTeacherSheet(activePlan.value.teacherSheet);
    await recordCurrentPlan("teacherSheet", { format: "markdown" });
    await loadSidebarData(true);
    showSuccess(successText.downloaded("教师活动单"));
  } catch (error) {
    console.error("Failed to download teacher sheet", error);
    ElMessage.error("下载教师活动单失败");
  }
};

const goToRoute = async () => {
  await recordCurrentPlan("routeCarry", { query: linkedTrailQuery.value });
  await loadSidebarData(true);
  router.push({ path: "/heritage-trail", query: linkedTrailQuery.value });
};

const goRegionTrail = async (regionName: string) => {
  const query = buildLearningRouteQuery(activeTrack.value, regionName);
  await recordCurrentPlan("routeCarry", { regionName, query });
  await loadSidebarData(true);
  router.push({ path: "/heritage-trail", query });
};

const goLinkedCuration = () => {
  router.push({ path: "/curation", query: { theme: activeTrack.value.linkedThemeId } });
};

const goRegionCategory = () => {
  router.push("/region-category");
};

const goHome = () => {
  router.push("/home");
};

const goDetail = (id: number) => {
  router.push({ path: "/home", query: { id } });
};

const restoreFavorite = async (item: LearningPlanFavoriteRecord) => {
  const parsed = item.planId ? parseLearningPlanId(item.planId) : null;
  const trackId = item.trackId || parsed?.trackId;
  if (trackId && LEARNING_TRACKS.some((track) => track.id === trackId)) {
    await changeTrack(trackId);
  }
};

const restoreTopPlan = async (item: LearningPlanStatisticItem) => {
  const parsed = item.planId ? parseLearningPlanId(item.planId) : null;
  const trackId = item.trackId || parsed?.trackId;
  if (trackId && LEARNING_TRACKS.some((track) => track.id === trackId)) {
    await changeTrack(trackId);
  }
};

const formatFavoriteDate = (value?: string | null) => {
  if (!value) {
    return "最近收藏";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "最近收藏"
    : date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
};

onMounted(async () => {
  try {
    applyTrackQuery(route.query.track);
    await load();
    await Promise.allSettled([loadSidebarData(), refreshFavoriteState()]);
    if (!hasRecordedInitialView.value) {
      await recordCurrentPlan("view", { source: "page-enter" });
      hasRecordedInitialView.value = true;
      await loadSidebarData(true);
    }
  } catch (error) {
    console.error("Failed to load learning studio overview", error);
    ElMessage.error("研学工坊数据加载失败，请稍后重试");
  } finally {
    bootLoading.value = false;
  }
});

watch(
  () => route.query.track,
  async (value) => {
    const previousTrackId = selectedTrackId.value;
    applyTrackQuery(value);
    if (bootLoading.value) {
      return;
    }
    if (previousTrackId === selectedTrackId.value && value === route.query.track) {
      return;
    }
    try {
      await refreshFavoriteState();
      await recordCurrentPlan("view", {
        source: previousTrackId === selectedTrackId.value ? "route-track-sync" : "route-track-change",
      });
      await loadSidebarData(true);
    } catch (error) {
      console.error("Failed to synchronize learning studio track state", error);
    }
  }
);

watch(
  () => activePlan.value.id,
  async () => {
    if (bootLoading.value) {
      return;
    }
    try {
      await refreshFavoriteState();
    } catch (error) {
      console.error("Failed to refresh learning favorite state", error);
    }
  }
);
</script>

<style scoped>
.learning-page {
  min-height: calc(100vh - 110px);
  padding: 12px 6px 32px;
  color: #243246;
}

.hero-section,
.content-grid,
.integration-grid,
.module-grid,
.project-grid,
.deliverable-list,
.stats-grid,
.sidebar-group,
.keyword-list {
  display: grid;
  gap: 18px;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 18px;
  margin-bottom: 18px;
}

.hero-copy,
.hero-metrics,
.track-switcher,
.plan-card,
.action-card {
  border-radius: 24px;
  border: 1px solid rgba(196, 30, 58, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero-copy {
  padding: 30px;
  background:
    radial-gradient(circle at top right, rgba(196, 30, 58, 0.12), transparent 34%),
    linear-gradient(135deg, rgba(255, 248, 238, 0.96), rgba(255, 255, 255, 0.98));
}

.hero-kicker,
.section-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: #a66a3f;
}

.hero-copy h1,
.section-head h2,
.module-card h3,
.project-body h3,
.deliverable-item strong,
.integration-item strong,
.stat-chip strong {
  margin: 0;
  color: #243246;
}

.hero-copy h1 {
  font-size: 40px;
  line-height: 1.08;
}

.hero-desc,
.section-desc,
.plan-summary p,
.integration-item p,
.deliverable-item p,
.module-objective,
.project-body p,
.sheet-summary p,
.list-item p {
  color: #5c6a7d;
  line-height: 1.8;
}

.hero-actions,
.toolbar-actions,
.plan-tags,
.sheet-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-actions {
  margin-top: 24px;
}

.hero-metrics {
  padding: 18px;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.integration-item,
.module-card,
.project-card,
.deliverable-item,
.stat-chip,
.list-item {
  border-radius: 18px;
  background: linear-gradient(135deg, #fffaf1, #fff);
  border: 1px solid rgba(166, 106, 63, 0.08);
}

.metric-card {
  padding: 18px;
  min-height: 132px;
}

.metric-card span,
.metric-card p,
.integration-item span,
.stat-chip span,
.list-item span {
  color: #775e50;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 34px;
  color: #b4362f;
}

.track-switcher {
  display: grid;
  grid-template-columns: minmax(220px, 0.7fr) minmax(0, 1.3fr);
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(255, 255, 255, 0.98)),
    rgba(255, 255, 255, 0.92);
}

.track-switcher-copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.track-switcher-copy h2 {
  margin: 0;
  color: #243246;
}

.track-switcher-copy p {
  margin: 0;
  line-height: 1.8;
  color: #677384;
}

.track-chip-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.track-chip {
  border: none;
  cursor: pointer;
  min-width: 0;
  min-height: 96px;
  padding: 16px 18px;
  border-radius: 18px;
  text-align: left;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 10px 28px rgba(34, 45, 66, 0.06);
}

.track-chip span {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #243246;
}

.track-chip small {
  display: block;
  margin-top: 6px;
  color: #8a96a3;
}

.track-chip.active,
.track-chip:hover {
  background: linear-gradient(135deg, #c41e3a, #d8863d);
  box-shadow: 0 18px 34px rgba(196, 30, 58, 0.2);
}

.track-chip.active span,
.track-chip.active small,
.track-chip:hover span,
.track-chip:hover small {
  color: #fffaf1;
}

.content-grid {
  grid-template-columns: minmax(0, 1.62fr) minmax(300px, 0.88fr);
  align-items: start;
}

.main-column,
.side-column {
  display: grid;
  gap: 18px;
}

.plan-card :deep(.el-card__body),
.action-card :deep(.el-card__body) {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.section-head.compact h2 {
  font-size: 22px;
}

.plan-summary {
  margin-top: 6px;
}

.plan-tags {
  margin-top: 18px;
}

.plan-tag,
.keyword-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(196, 30, 58, 0.08);
  color: #8d3d34;
  font-size: 13px;
}

.integration-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 20px;
}

.integration-item,
.module-card,
.deliverable-item,
.sheet-summary,
.stat-chip,
.list-item {
  padding: 18px;
}

.module-grid {
  margin-top: 22px;
}

.module-card h3,
.project-body h3 {
  margin-bottom: 8px;
}

.module-duration {
  margin: 0 0 8px;
  color: #b4362f;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.module-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.module-columns strong {
  display: block;
  margin-bottom: 8px;
}

.module-columns ul {
  margin: 0;
  padding-left: 18px;
  color: #5c6a7d;
  line-height: 1.8;
}

.project-grid {
  margin-top: 12px;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 14px;
}

.project-card {
  overflow: hidden;
  cursor: pointer;
}

.project-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.project-body {
  padding: 16px;
}

.project-meta,
.project-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #8a96a3;
}

.project-body p {
  margin: 0 0 14px;
}

.sheet-summary {
  display: grid;
  gap: 16px;
}

.stats-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.sidebar-group {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.sidebar-group strong {
  color: #243246;
}

.list-item {
  width: 100%;
  border: none;
  cursor: pointer;
  text-align: left;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: inherit;
}

.list-item strong {
  color: #243246;
}

.keyword-list {
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
}

@media (max-width: 1100px) {
  .hero-section,
  .content-grid,
  .track-switcher {
    grid-template-columns: 1fr;
  }

  .hero-metrics,
  .track-chip-grid,
  .integration-grid,
  .module-columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .learning-page {
    padding: 8px 0 18px;
  }

  .hero-copy,
  .track-switcher,
  .plan-card :deep(.el-card__body),
  .action-card :deep(.el-card__body) {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
