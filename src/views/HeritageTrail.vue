<template>
  <div class="trail-page">
    <section class="hero heritage-float-card" v-loading="loading" element-loading-text="正在加载路线概览...">
      <div class="hero-copy">
        <p class="page-kicker">HERITAGE TRAVEL PLANNER</p>
        <h1>非遗旅游路线</h1>
        <p class="page-desc">
          路线页现在也统一成了更轻的工作台节奏。模板切换、自定义生成、收藏和导出都保留，但概览数据优先复用缓存，不再一进页就整块抖动。
        </p>
        <div class="hero-actions">
          <el-button type="primary" round @click="applyTemplatePreferences">刷新模板路线</el-button>
          <el-button round @click="generateCustomTrail">
            <el-icon><MagicStick /></el-icon>
            生成自定义路线
          </el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <article v-for="item in heroMetrics" :key="item.label" class="metric-card heritage-float-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.help }}</p>
        </article>
      </div>
    </section>

    <section class="planner-panel heritage-float-card" v-loading="loading" element-loading-text="正在同步路线工作台...">
      <div class="section-head">
        <div>
          <p class="section-kicker">ROUTE WORKBENCH</p>
          <h2>{{ routeMode === "template" ? "主题模板路线" : "自定义路线" }}</h2>
          <p class="section-desc">
            当前路线为《{{ activeTrail.title }}》。你可以先切换模板，再按兴趣、时长、预算和交通方式微调，或者直接生成自定义路线。
          </p>
        </div>
        <div class="toolbar-actions">
          <el-button :type="isFavorited ? 'warning' : 'primary'" plain :loading="favoriteBusy" @click="handleToggleFavorite">
            <el-icon><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
            {{ isFavorited ? "已收藏路线" : "收藏当前路线" }}
          </el-button>
          <el-button plain @click="shareCurrentTrail"><el-icon><Share /></el-icon>分享路线</el-button>
          <el-button plain :disabled="activeTrail.stops.length === 0" @click="handleExportPdf">
            <el-icon><Document /></el-icon>导出 PDF
          </el-button>
          <el-button plain :disabled="activeTrail.stops.length === 0" @click="handleDownloadItinerary">
            <el-icon><Download /></el-icon>下载行程单
          </el-button>
          <el-button plain @click="resetPlanner"><el-icon><RefreshRight /></el-icon>重置筛选</el-button>
        </div>
      </div>

      <div class="template-panel">
        <div class="template-copy">
          <p class="section-kicker">TEMPLATE NAVIGATOR</p>
          <h3>切换路线主题</h3>
          <p>模板切换会保留当前页面节奏，只更新必要的路线状态与侧栏统计。</p>
        </div>

        <div class="template-strip">
          <button
            v-for="template in TRAIL_TEMPLATE_LIBRARY"
            :key="template.id"
            type="button"
            class="template-chip heritage-float-card"
            :class="{ active: routeMode === 'template' && selectedTrailId === template.id }"
            @click="switchTemplate(template.id)"
          >
            <span>{{ template.title }}</span>
            <small>{{ template.subtitle }}</small>
          </button>
        </div>
      </div>

      <div class="planner-grid">
        <div class="planner-form">
          <div class="field">
            <label>兴趣偏好</label>
            <el-select
              v-model="plannerForm.interestIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择感兴趣的门类"
            >
              <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>

          <div class="field-row">
            <div class="field">
              <label>游览时长</label>
              <el-select v-model="plannerForm.durationKey">
                <el-option v-for="item in TRAIL_DURATION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="field">
              <label>出行方式</label>
              <el-select v-model="plannerForm.transportMode">
                <el-option
                  v-for="item in TRAIL_TRANSPORT_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="field">
              <label>预算水平</label>
              <el-select v-model="plannerForm.budgetLevel">
                <el-option v-for="item in TRAIL_BUDGET_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
          </div>

          <div class="field-row">
            <div class="field">
              <label>区域关键词</label>
              <el-input v-model="plannerForm.regionKeyword" clearable placeholder="例如：福建、成都、苏州" />
            </div>
            <div class="field">
              <label>最多点位</label>
              <el-input-number v-model="plannerForm.maxStops" :min="2" :max="6" />
            </div>
            <div class="field">
              <label>优先热门项目</label>
              <el-switch v-model="plannerForm.preferHot" />
            </div>
          </div>
        </div>

        <div class="planner-summary">
          <div v-for="item in plannerSummaryCards" :key="item.label" class="summary-card heritage-float-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>

          <div class="summary-note heritage-float-card">
            <strong>当前路线关键词</strong>
            <p>{{ activeTrail.keywords.join(" / ") }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <div class="main-column">
        <article class="route-profile heritage-float-card">
          <div class="section-head">
            <div>
              <p class="section-kicker">ROUTE PROFILE</p>
              <h2>{{ activeTrail.title }}</h2>
              <p class="section-desc">{{ activeTrail.subtitle }}</p>
            </div>
            <div class="badge-row">
              <el-tag round effect="dark">{{ activeTrail.durationLabel }}</el-tag>
              <el-tag round effect="plain">{{ activeTrail.transportLabel }}</el-tag>
              <el-tag round effect="plain" type="warning">{{ activeTrail.budgetLabel }}</el-tag>
            </div>
          </div>

          <p class="route-description">{{ activeTrail.description }}</p>

          <div class="route-metrics">
            <div class="metric-card heritage-float-card">
              <span>推荐停留</span>
              <strong>{{ activeTrail.estimatedHours }} 小时</strong>
            </div>
            <div class="metric-card heritage-float-card">
              <span>预算建议</span>
              <strong>￥{{ activeTrail.estimatedCost }}</strong>
            </div>
            <div class="metric-card heritage-float-card">
              <span>路线站点</span>
              <strong>{{ activeTrail.stopCount }} 个</strong>
            </div>
          </div>

          <div class="note-grid">
            <div v-for="item in activeTrail.notes" :key="item.title" class="note-card heritage-float-card">
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
          </div>
        </article>

        <TrailRouteMap
          v-if="activeTrail.stops.length"
          :stops="activeTrail.stops"
          :transport-mode="activeTransportMode"
        />

        <article class="stops-panel heritage-float-card">
          <div class="section-head">
            <div>
              <p class="section-kicker">ROUTE STOPS</p>
              <h2>路线站点</h2>
              <p class="section-desc">保留地图、传承人和支持提示，让路线阅读与实地规划能连续衔接。</p>
            </div>
            <div class="section-meta">
              <span class="meta-chip">{{ activeTrail.stopCount }} 个推荐站点</span>
              <span class="meta-chip">{{ routeMode === "custom" ? "自定义生成" : "模板推荐" }}</span>
            </div>
          </div>

          <el-empty
            v-if="activeTrail.stops.length === 0"
            description="当前筛选下还没有可串联的路线，建议放宽兴趣或区域范围。"
            :image-size="110"
          />

          <div v-else class="stops-list">
            <article v-for="(stop, index) in activeTrail.stops" :key="stop.project.id" class="stop-card heritage-float-card">
              <div class="stop-cover">
                <img :src="buildStaticUrl(stop.project.coverUrl) || fallbackCover" :alt="stop.project.name" />
                <span class="order">第 {{ index + 1 }} 站</span>
              </div>

              <div class="stop-body">
                <div class="stop-meta">
                  <span>{{ stop.project.regionName || "地区待补充" }}</span>
                  <span>{{ stop.project.categoryName || getCategoryName(stop.project.categoryId) }}</span>
                  <span>{{ stop.project.protectLevel || "级别待补充" }}</span>
                </div>

                <div class="stop-head">
                  <h3>{{ stop.project.name }}</h3>
                  <el-button text type="primary" @click="goDetail(Number(stop.project.id))">查看项目详情</el-button>
                </div>

                <p class="stop-desc">{{ summarizeRichText(stop.project.history || stop.project.features, 128) }}</p>
                <p class="context-copy">{{ stop.historicalContext }}</p>

                <div class="practical-grid">
                  <div class="metric-card heritage-float-card">
                    <span>建议停留</span>
                    <strong>{{ stop.visitDurationLabel }}</strong>
                  </div>
                  <div class="metric-card heritage-float-card">
                    <span>转场建议</span>
                    <strong>{{ stop.transferTip }}</strong>
                  </div>
                  <div class="metric-card heritage-float-card">
                    <span>单站预算</span>
                    <strong>￥{{ stop.estimatedSpend }}</strong>
                  </div>
                </div>

                <div class="tag-list">
                  <span v-for="item in stop.facilityTags" :key="item" class="pill">{{ item }}</span>
                </div>

                <div class="support-list">
                  <div v-for="item in stop.supportTips" :key="item" class="support-item">
                    <el-icon><Guide /></el-icon>
                    <span>{{ item }}</span>
                  </div>
                </div>

                <div class="inheritors">
                  <div class="mini-head">
                    <strong>传承人信息</strong>
                    <span>{{ stop.inheritors.length ? `${stop.inheritors.length} 位相关传承人` : stop.project.inheritorNames || "当前仅有传承人名称" }}</span>
                  </div>

                  <div v-if="stop.inheritors.length" class="inheritor-list">
                    <div v-for="person in stop.inheritors.slice(0, 2)" :key="person.id" class="inheritor-card heritage-float-card">
                      <el-avatar :size="42" :src="buildStaticUrl(person.avatarUrl) || fallbackAvatar" />
                      <div>
                        <strong>{{ person.name }}</strong>
                        <p>{{ person.level || "传承级别待补充" }}</p>
                        <small>{{ summarizeRichText(person.description, 52, "可继续从项目详情页查看完整传承背景。") }}</small>
                      </div>
                    </div>
                  </div>

                  <p v-else class="stop-desc">{{ stop.project.inheritorNames || "当前项目暂无结构化传承人资料。" }}</p>
                </div>
              </div>
            </article>
          </div>
        </article>
      </div>

      <aside class="side-column">
        <article class="side-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">ANALYTICS</p>
              <h2>路线统计</h2>
            </div>
            <el-icon><DataAnalysis /></el-icon>
          </div>

          <div class="stats-grid">
            <div class="summary-card heritage-float-card"><span>访问量</span><strong>{{ analyticsSummary.viewCount }}</strong></div>
            <div class="summary-card heritage-float-card"><span>分享量</span><strong>{{ analyticsSummary.shareCount }}</strong></div>
            <div class="summary-card heritage-float-card"><span>定制量</span><strong>{{ analyticsSummary.customizationCount }}</strong></div>
            <div class="summary-card heritage-float-card"><span>导出量</span><strong>{{ analyticsSummary.exportCount }}</strong></div>
          </div>

          <div class="mini-section">
            <strong>热门路线</strong>
            <button
              v-for="item in analyticsSummary.topTrails.slice(0, 5)"
              :key="`${item.trailId}-${item.routeType}`"
              type="button"
              class="list-item heritage-float-card"
              @click="jumpToTrail(item)"
            >
              <span>{{ item.trailTitle || item.trailId || "未命名路线" }}</span>
              <strong>{{ item.value }}</strong>
            </button>
          </div>

          <div class="mini-section">
            <strong>用户偏好</strong>
            <div v-for="item in analyticsSummary.transportPreferences.slice(0, 3)" :key="item.name" class="bar-row">
              <div class="bar-copy"><span>{{ item.name }}</span><strong>{{ item.value }}</strong></div>
              <el-progress :percentage="toPercent(item.value, analyticsSummary.transportPreferences)" :show-text="false" />
            </div>
          </div>

          <div class="tag-list">
            <span v-for="item in analyticsSummary.interestPreferences.slice(0, 6)" :key="item.name" class="pill">
              {{ item.name }} / {{ item.value }}
            </span>
          </div>
        </article>

        <article class="side-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">MY ROUTES</p>
              <h2>我的路线收藏</h2>
            </div>
            <el-icon><CollectionTag /></el-icon>
          </div>

          <el-empty
            v-if="favoriteRoutes.length === 0"
            description="当前还没有收藏路线，生成或选择一条喜欢的路线后可在这里继续查看。"
            :image-size="90"
          />

          <div v-else class="mini-section">
            <button
              v-for="item in favoriteRoutes.slice(0, 6)"
              :key="item.id"
              type="button"
              class="list-item heritage-float-card"
              @click="restoreFavorite(item)"
            >
              <div>
                <strong>{{ item.trailTitle || item.trailId }}</strong>
                <p>{{ item.routeType === "custom" ? "自定义路线" : "主题模板路线" }}</p>
              </div>
              <span>{{ formatFavoriteDate(item.createdAt) }}</span>
            </button>
          </div>
        </article>

        <article class="side-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">HOT REGIONS</p>
              <h2>热点地区</h2>
            </div>
            <el-icon><Location /></el-icon>
          </div>

          <div class="mini-section">
            <button
              v-for="item in topRegions"
              :key="item.name"
              type="button"
              class="list-item heritage-float-card"
              @click="applyRegion(item.name)"
            >
              <span>{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </button>
          </div>
        </article>

        <article class="side-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">HOT PROJECTS</p>
              <h2>热门补充项目</h2>
            </div>
            <el-icon><Opportunity /></el-icon>
          </div>

          <div class="mini-section">
            <button
              v-for="item in hotProjects.slice(0, 5)"
              :key="item.id"
              type="button"
              class="list-item heritage-float-card"
              @click="goDetail(Number(item.id))"
            >
              <span>{{ item.name }}</span>
              <strong>{{ item.viewCount || 0 }}</strong>
            </button>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";
import {
  CollectionTag,
  DataAnalysis,
  Document,
  Download,
  Guide,
  Location,
  MagicStick,
  Opportunity,
  RefreshRight,
  Share,
  Star,
  StarFilled,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import TrailRouteMap from "@/components/trail/TrailRouteMap.vue";
import { CATEGORY_OPTIONS } from "@/constants/heritage";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import {
  TRAIL_BUDGET_OPTIONS,
  TRAIL_DURATION_OPTIONS,
  TRAIL_TEMPLATE_LIBRARY,
  TRAIL_TRANSPORT_OPTIONS,
} from "@/constants/trail";
import { useHeritageOverview } from "@/composables/useHeritageOverview";
import { useHeritageHubStore } from "@/stores/heritageHub";
import { fetchInheritorPage } from "@/services/project";
import { checkTrailFavorite, recordTrailAnalytics, toggleTrailFavorite } from "@/services/trail";
import type { TrailFavoriteRecord, TrailInheritorProfile, TrailPlannerForm, TrailStatisticItem } from "@/types/trail";
import { downloadTrailItinerary, exportTrailItineraryPdf } from "@/utils/trail-export";
import { buildStaticUrl } from "@/utils/url";
import { showSuccess, showWarning, successText } from "@/utils/uiFeedback";
import {
  buildCustomTrail,
  buildTemplateTrail,
  collectInterestLabels,
  createDefaultTrailPlanner,
  parseCustomTrailId,
  resolveTrailTemplate,
} from "@/utils/trail";
import { getCategoryName, summarizeRichText } from "@/utils/heritage";

const router = useRouter();
const route = useRoute();
const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;
const fallbackAvatar = MATERIAL_PLACEHOLDERS.avatar;

const bootLoading = ref(true);
const routeMode = ref<"template" | "custom">("template");
const selectedTrailId = ref(TRAIL_TEMPLATE_LIBRARY[0].id);
const favoriteBusy = ref(false);
const isFavorited = ref(false);
const hubStore = useHeritageHubStore();
const { trailFavorites: favoriteRoutes, trailAnalyticsSummary: analyticsSummary } = storeToRefs(hubStore);
const inheritorMap = ref<Record<number, TrailInheritorProfile[]>>({});
const appliedCustomPlanner = ref<TrailPlannerForm>(createDefaultTrailPlanner());
const plannerForm = reactive<TrailPlannerForm>(createDefaultTrailPlanner());

const { projects, hotProjects, regionStats, loading: overviewLoading, load } = useHeritageOverview();
const loading = computed(() => bootLoading.value || overviewLoading.value);
const activeTemplate = computed(() => resolveTrailTemplate(selectedTrailId.value));
const templateTrail = computed(() =>
  buildTemplateTrail(activeTemplate.value, projects.value, inheritorMap.value, plannerForm)
);
const customTrail = computed(() => buildCustomTrail(projects.value, inheritorMap.value, appliedCustomPlanner.value));
const activeTrail = computed(() => (routeMode.value === "custom" ? customTrail.value : templateTrail.value));
const topRegions = computed(() =>
  [...regionStats.value].sort((a, b) => Number(b.value || 0) - Number(a.value || 0)).slice(0, 5)
);
const currentInterestLabels = computed(() =>
  collectInterestLabels(
    routeMode.value === "custom"
      ? appliedCustomPlanner.value
      : { ...plannerForm, interestIds: activeTemplate.value.categoryIds }
  )
);
const activeTransportMode = computed(() =>
  routeMode.value === "custom" ? appliedCustomPlanner.value.transportMode : plannerForm.transportMode
);

const heroMetrics = computed(() => [
  {
    help: "可切换的路线模板数量",
    label: "路线模板",
    value: TRAIL_TEMPLATE_LIBRARY.length,
  },
  {
    help: "当前概览中的候选项目数",
    label: "候选项目",
    value: projects.value.length,
  },
  {
    help: "路线模块累计收藏量",
    label: "收藏路线",
    value: analyticsSummary.value.favoriteCount,
  },
  {
    help: "路线模块累计访问量",
    label: "路线访问",
    value: analyticsSummary.value.viewCount,
  },
]);

const plannerSummaryCards = computed(() => [
  {
    label: "当前模式",
    value: routeMode.value === "custom" ? "自定义路线" : "模板路线",
  },
  {
    label: "建议时长",
    value: activeTrail.value.durationLabel,
  },
  {
    label: "交通建议",
    value: activeTrail.value.transportLabel,
  },
  {
    label: "预算预估",
    value: `￥${activeTrail.value.estimatedCost}`,
  },
  {
    label: "路线标签",
    value: currentInterestLabels.value.length ? currentInterestLabels.value.join(" / ") : "按主题自动匹配",
  },
]);

const getPlannerSnapshot = () =>
  routeMode.value === "custom"
    ? { ...appliedCustomPlanner.value, interestIds: [...appliedCustomPlanner.value.interestIds] }
    : { ...plannerForm, interestIds: [...activeTemplate.value.categoryIds] };

const syncTrailContext = async (
  actionType: "view" | "share" | "customize" | "export",
  source: string,
  extraPayload: Record<string, unknown> = {}
) => {
  await refreshFavoriteState();
  await recordCurrentRoute(actionType, { source, ...extraPayload });
  await loadSidebarData(true);
};

const loadInheritors = async () => {
  const grouped: Record<number, TrailInheritorProfile[]> = {};
  const pageSize = 200;
  let pageNum = 1;
  let total = 0;

  do {
    const res = await fetchInheritorPage({ pageNum, pageSize });
    const page = res.data.data || {};
    const records = page.records || [];
    total = Number(page.total || records.length);

    for (const item of records) {
      const key = Number(item.projectId);
      if (!Number.isFinite(key)) {
        continue;
      }

      (grouped[key] ||= []).push({
        address: item.address,
        avatarUrl: item.avatarUrl,
        description: item.description,
        id: Number(item.id),
        latitude: item.latitude,
        level: item.level,
        longitude: item.longitude,
        name: item.name,
        projectId: Number(item.projectId),
        sex: item.sex,
      });
    }

    pageNum += 1;
  } while (Object.values(grouped).reduce((sum, items) => sum + items.length, 0) < total && pageNum <= 20);

  inheritorMap.value = grouped;
};

const loadSidebarData = async (force = false) => hubStore.syncTrailState(force);

const refreshFavoriteState = async () => {
  try {
    const res = await checkTrailFavorite(activeTrail.value.id, activeTrail.value.routeType);
    isFavorited.value = Boolean(res.data.data);
  } catch (error) {
    console.error("Failed to refresh trail favorite state", error);
  }
};

const recordCurrentRoute = async (
  actionType: "view" | "share" | "customize" | "export",
  payload: Record<string, unknown> = {}
) => {
  const planner = getPlannerSnapshot();
  try {
    await recordTrailAnalytics({
      actionType,
      budgetLevel: planner.budgetLevel,
      durationKey: planner.durationKey,
      estimatedCost: activeTrail.value.estimatedCost,
      estimatedHours: activeTrail.value.estimatedHours,
      interests: collectInterestLabels(planner),
      payload,
      routeType: activeTrail.value.routeType,
      stopCount: activeTrail.value.stopCount,
      trailId: activeTrail.value.id,
      trailTitle: activeTrail.value.title,
      transportMode: planner.transportMode,
    });
  } catch (error) {
    console.error("Failed to record trail analytics", error);
  }
};
const switchTemplate = async (trailId: string) => {
  if (routeMode.value === "template" && selectedTrailId.value === trailId) {
    return;
  }

  selectedTrailId.value = trailId;
  routeMode.value = "template";
  await syncTrailContext("view", "template-switch");
};

const applyTemplatePreferences = async () => {
  routeMode.value = "template";
  await syncTrailContext("view", "apply-template-filters");
  showSuccess(successText.refreshed("主题路线"));
};

const generateCustomTrail = async () => {
  appliedCustomPlanner.value = { ...plannerForm, interestIds: [...plannerForm.interestIds] };
  routeMode.value = "custom";
  await syncTrailContext("customize", "planner");
  showSuccess(successText.generated("自定义非遗路线"));
};

const resetPlanner = () => {
  Object.assign(plannerForm, createDefaultTrailPlanner());
  appliedCustomPlanner.value = createDefaultTrailPlanner();
  routeMode.value = "template";
  selectedTrailId.value = TRAIL_TEMPLATE_LIBRARY[0].id;
  refreshFavoriteState().catch(() => undefined);
};

const handleToggleFavorite = async () => {
  favoriteBusy.value = true;
  try {
    const res = await toggleTrailFavorite({
      routeType: activeTrail.value.routeType,
      trailId: activeTrail.value.id,
      trailTitle: activeTrail.value.title,
    });
    isFavorited.value = Boolean(res.data.data);
    await loadSidebarData(true);
    showSuccess(isFavorited.value ? "已收藏当前路线" : "已取消收藏当前路线");
  } catch (error) {
    console.error("Failed to toggle trail favorite", error);
  } finally {
    favoriteBusy.value = false;
  }
};

const shareCurrentTrail = async () => {
  try {
    if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
      throw new Error("当前环境暂不支持复制分享链接");
    }

    const planner = getPlannerSnapshot();
    const query: Record<string, string> = {
      mode: routeMode.value,
      trail: activeTrail.value.id,
      duration: planner.durationKey,
      transport: planner.transportMode,
      budget: planner.budgetLevel,
      stops: String(planner.maxStops),
      hot: planner.preferHot ? "1" : "0",
    };

    if (planner.regionKeyword.trim()) {
      query.region = planner.regionKeyword.trim();
    }
    if (planner.interestIds.length) {
      query.interests = planner.interestIds.join(",");
    }

    const shareUrl = new URL(router.resolve({ path: "/heritage-trail", query }).href, window.location.origin).toString();
    await navigator.clipboard.writeText(shareUrl);
    await syncTrailContext("share", "share-link", { shareUrl });
    showSuccess(successText.copied("路线链接"));
  } catch (error) {
    console.error("Failed to share trail", error);
    ElMessage.error(error instanceof Error ? error.message : "分享路线失败");
  }
};

const handleExportPdf = async () => {
  try {
    exportTrailItineraryPdf(activeTrail.value);
    await syncTrailContext("export", "export-pdf", { format: "pdf-print" });
    showSuccess("路线打印页已打开，可直接另存为 PDF");
  } catch (error) {
    console.error("Failed to export trail PDF", error);
    ElMessage.error(error instanceof Error ? error.message : "导出 PDF 失败");
  }
};

const handleDownloadItinerary = async () => {
  try {
    downloadTrailItinerary(activeTrail.value);
    await syncTrailContext("export", "download-markdown", { format: "markdown" });
    showSuccess(successText.downloaded("行程单"));
  } catch (error) {
    console.error("Failed to download itinerary", error);
    ElMessage.error("下载行程单失败");
  }
};

const restoreFavorite = async (item: TrailFavoriteRecord) => {
  if (item.routeType === "custom") {
    const parsed = parseCustomTrailId(item.trailId);
    if (parsed) {
      Object.assign(plannerForm, parsed);
      appliedCustomPlanner.value = { ...parsed, interestIds: [...parsed.interestIds] };
      routeMode.value = "custom";
    }
  } else {
    selectedTrailId.value = item.trailId;
    routeMode.value = "template";
  }

  await syncTrailContext("view", "favorite-restore");
};

const jumpToTrail = async (item: TrailStatisticItem) => {
  if (item.routeType === "custom" && item.trailId) {
    const parsed = parseCustomTrailId(item.trailId);
    if (parsed) {
      Object.assign(plannerForm, parsed);
      appliedCustomPlanner.value = { ...parsed, interestIds: [...parsed.interestIds] };
      routeMode.value = "custom";
      await syncTrailContext("view", "analytics-custom-restore");
      return;
    }
  }

  if (item.trailId) {
    await switchTemplate(item.trailId);
  }
};

const applyRegion = async (name: string) => {
  plannerForm.regionKeyword = name;
  if (routeMode.value === "custom") {
    await generateCustomTrail();
  } else {
    await applyTemplatePreferences();
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

const toPercent = (value: number, source: TrailStatisticItem[]) => {
  const max = Math.max(...source.map((item) => Number(item.value || 0)), 1);
  return Math.max(8, Math.round((Number(value || 0) / max) * 100));
};

const goDetail = (id: number) => router.push({ path: "/home", query: { id } });

const applyQueryPreset = () => {
  const defaults = createDefaultTrailPlanner();
  Object.assign(plannerForm, defaults);
  appliedCustomPlanner.value = {
    ...defaults,
    interestIds: [...defaults.interestIds],
  };
  routeMode.value = "template";
  selectedTrailId.value = TRAIL_TEMPLATE_LIBRARY[0].id;

  const trailId = typeof route.query.trail === "string" ? route.query.trail : "";
  const interests = typeof route.query.interests === "string" ? route.query.interests : "";
  const region = typeof route.query.region === "string" ? route.query.region : "";
  const stops = Number(route.query.stops);
  const mode = route.query.mode;

  plannerForm.regionKeyword = region;
  plannerForm.preferHot = route.query.hot !== "0";

  if (interests) {
    plannerForm.interestIds = interests
      .split(",")
      .map((item) => Number(item))
      .filter(Number.isFinite);
  }
  if (Number.isFinite(stops) && stops >= 2 && stops <= 6) {
    plannerForm.maxStops = stops;
  }
  if (
    typeof route.query.duration === "string" &&
    TRAIL_DURATION_OPTIONS.some((item) => item.value === route.query.duration)
  ) {
    plannerForm.durationKey = route.query.duration as TrailPlannerForm["durationKey"];
  }
  if (
    typeof route.query.transport === "string" &&
    TRAIL_TRANSPORT_OPTIONS.some((item) => item.value === route.query.transport)
  ) {
    plannerForm.transportMode = route.query.transport as TrailPlannerForm["transportMode"];
  }
  if (
    typeof route.query.budget === "string" &&
    TRAIL_BUDGET_OPTIONS.some((item) => item.value === route.query.budget)
  ) {
    plannerForm.budgetLevel = route.query.budget as TrailPlannerForm["budgetLevel"];
  }

  if (mode === "custom") {
    const parsed = trailId ? parseCustomTrailId(trailId) : null;
    const finalPlanner = parsed || { ...plannerForm, interestIds: [...plannerForm.interestIds] };
    Object.assign(plannerForm, finalPlanner);
    appliedCustomPlanner.value = { ...finalPlanner, interestIds: [...finalPlanner.interestIds] };
    routeMode.value = "custom";
  } else if (trailId && TRAIL_TEMPLATE_LIBRARY.some((item) => item.id === trailId)) {
    selectedTrailId.value = trailId;
  }
};

onMounted(async () => {
  try {
    applyQueryPreset();
    const results = await Promise.allSettled([load(), loadInheritors(), loadSidebarData()]);
    if (results.some((item) => item.status === "rejected")) {
      console.error(
        "Failed to load part of heritage trail data",
        results
          .filter((item): item is PromiseRejectedResult => item.status === "rejected")
          .map((item) => item.reason)
      );
      showWarning("部分路线数据加载失败，已尽量展示可用内容");
    }

    await syncTrailContext("view", route.query.mode === "custom" ? "shared-custom" : "page-enter");
  } catch (error) {
    console.error("Failed to bootstrap heritage trail page", error);
    ElMessage.error("非遗路线数据加载失败，请稍后重试");
  } finally {
    bootLoading.value = false;
  }
});

watch(
  () => route.fullPath,
  async (value, previousValue) => {
    if (bootLoading.value || value === previousValue) {
      return;
    }

    try {
      applyQueryPreset();
      await syncTrailContext("view", "route-query-change");
    } catch (error) {
      console.error("Failed to synchronize heritage trail query state", error);
    }
  }
);
</script>

<style scoped>
.trail-page {
  min-height: calc(100vh - 96px);
  padding: 12px 6px 28px;
  display: grid;
  gap: 18px;
  color: var(--heritage-ink);
}

.hero,
.planner-panel,
.route-profile,
.stops-panel,
.side-card {
  border-radius: 26px;
  border: 1px solid var(--heritage-border);
  background: var(--heritage-surface);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(192, 138, 63, 0.16), transparent 32%),
    linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(255, 251, 244, 0.98));
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
.template-copy h3,
.stop-head h3 {
  margin: 0;
  color: var(--heritage-ink);
}

.page-desc,
.section-desc,
.route-description,
.note-card p,
.stop-desc,
.context-copy,
.support-item,
.inheritor-card small,
.template-copy p,
.summary-note p {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-actions,
.toolbar-actions,
.badge-row,
.section-meta,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions {
  margin-top: 20px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.summary-card,
.note-card,
.list-item,
.inheritor-card,
.summary-note {
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.metric-card span,
.summary-card span,
.list-item span {
  display: block;
  color: var(--heritage-muted);
  font-size: 12px;
}

.metric-card strong,
.summary-card strong,
.summary-note strong,
.list-item strong,
.inheritor-card strong {
  display: block;
  margin-top: 8px;
  color: var(--heritage-ink);
}

.hero .metric-card strong {
  font-size: 32px;
  color: var(--heritage-primary);
}

.planner-panel,
.route-profile,
.stops-panel,
.side-card {
  padding: 24px;
}

.section-head,
.mini-head,
.stop-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.section-head.compact {
  align-items: flex-start;
}

.toolbar-actions {
  justify-content: flex-end;
}

.meta-chip,
.pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(192, 138, 63, 0.12);
  color: var(--heritage-primary);
  font-size: 12px;
}

.template-panel {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.28fr);
  gap: 16px;
  margin-top: 18px;
}

.template-copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.template-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.template-chip {
  border: none;
  cursor: pointer;
  padding: 16px 18px;
  border-radius: 18px;
  text-align: left;
  background: rgba(255, 251, 244, 0.94);
  box-shadow: 0 10px 24px rgba(72, 41, 28, 0.06);
}

.template-chip span {
  display: block;
  font-weight: 600;
  color: var(--heritage-ink);
}

.template-chip small {
  display: block;
  margin-top: 6px;
  color: var(--heritage-muted);
}

.template-chip.active,
.template-chip:hover {
  background: linear-gradient(135deg, var(--heritage-primary), var(--heritage-gold));
  box-shadow: 0 18px 30px rgba(164, 59, 47, 0.22);
}

.template-chip.active span,
.template-chip.active small,
.template-chip:hover span,
.template-chip:hover small {
  color: var(--heritage-paper-soft);
}

.planner-grid,
.content-grid {
  display: grid;
  gap: 18px;
}

.planner-grid {
  grid-template-columns: minmax(0, 1.48fr) minmax(280px, 0.82fr);
  margin-top: 18px;
}

.planner-form,
.planner-summary,
.main-column,
.side-column,
.note-grid,
.stops-list,
.mini-section {
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field label {
  font-size: 13px;
  color: var(--heritage-muted);
  font-weight: 600;
}

.field-row,
.route-metrics,
.stats-grid,
.inheritor-list {
  display: grid;
  gap: 14px;
}

.field-row,
.route-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.planner-summary {
  align-content: start;
}

.summary-note p {
  margin-top: 8px;
}

.content-grid {
  grid-template-columns: minmax(0, 1.52fr) minmax(300px, 0.9fr);
  align-items: start;
}

.route-description {
  margin-top: 14px;
}

.route-metrics,
.note-grid {
  margin-top: 18px;
}

.note-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stop-card {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(192, 138, 63, 0.14);
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.94), rgba(255, 251, 244, 0.98));
}

.stop-cover {
  position: relative;
  min-height: 220px;
}

.stop-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(34, 49, 63, 0.76);
  color: var(--heritage-paper-soft);
  font-size: 12px;
}

.stop-body {
  padding: 20px 20px 20px 0;
  display: grid;
  gap: 14px;
}

.stop-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--heritage-muted);
  font-size: 13px;
}

.practical-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.support-list {
  display: grid;
  gap: 10px;
}

.support-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.inheritors {
  padding-top: 6px;
  border-top: 1px dashed rgba(192, 138, 63, 0.28);
}

.mini-head span,
.inheritor-card p,
.list-item p {
  color: var(--heritage-muted);
  font-size: 13px;
}

.inheritor-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.inheritor-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
  background: rgba(110, 133, 119, 0.08);
}

.side-column {
  display: grid;
  gap: 18px;
}

.stats-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
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
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.94), rgba(223, 188, 123, 0.12));
  color: inherit;
}

.bar-row {
  display: grid;
  gap: 6px;
}

.bar-copy {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.bar-copy span {
  color: var(--heritage-ink);
}

@media (max-width: 1200px) {
  .hero,
  .planner-grid,
  .content-grid,
  .template-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-metrics,
  .template-strip,
  .field-row,
  .route-metrics,
  .note-grid,
  .practical-grid,
  .stats-grid,
  .inheritor-list {
    grid-template-columns: 1fr;
  }

  .stop-card {
    grid-template-columns: 1fr;
  }

  .stop-body {
    padding: 0 18px 18px;
  }
}

@media (max-width: 768px) {
  .trail-page {
    padding: 10px 0 20px;
  }

  .hero,
  .planner-panel,
  .route-profile,
  .stops-panel,
  .side-card {
    padding: 18px;
  }

  .section-head,
  .mini-head,
  .stop-head,
  .toolbar-actions,
  .list-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-copy h1 {
    font-size: 34px;
  }
}
</style>
