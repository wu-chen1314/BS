<template>
  <div class="curation-page">
    <section class="hero" v-loading="loading" element-loading-text="正在加载主题策展...">
      <div class="hero-copy heritage-float-card">
        <p class="hero-kicker">INTANGIBLE HERITAGE CURATION</p>
        <h1>非遗主题策展</h1>
        <p class="hero-desc">
          将项目、地区热度、传承人与研学需求重新编排成适合传播、展示和讲述的专题内容。
          你可以从工艺、节俗、演艺三个方向进入，再继续联动到研学工坊与非遗路线。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="goLinkedLearningStudio">
            联动研学工坊
          </el-button>
          <el-button size="large" round @click="goLinkedTrail">生成主题路线</el-button>
          <el-button size="large" round @click="goHome">返回项目总览</el-button>
        </div>
      </div>

      <div class="hero-panel heritage-float-card">
        <div class="panel-card headline heritage-float-card">
          <span class="panel-label">当前主题</span>
          <strong>{{ activeTheme.title }}</strong>
          <p>{{ activeTheme.subtitle }}</p>
        </div>
        <div class="panel-grid">
          <div class="panel-card heritage-float-card">
            <span class="panel-label">主题项目</span>
            <strong>{{ filteredProjects.length }}</strong>
            <p>按当前策展主题筛出的重点项目</p>
          </div>
          <div class="panel-card heritage-float-card">
            <span class="panel-label">联动热榜</span>
            <strong>{{ displayHotProjects.length }}</strong>
            <p>优先展示与当前主题关联的热门项目</p>
          </div>
          <div class="panel-card heritage-float-card">
            <span class="panel-label">覆盖热区</span>
            <strong>{{ topRegions.length }}</strong>
            <p>来自统计地图的重点地区</p>
          </div>
          <div class="panel-card heritage-float-card">
            <span class="panel-label">联动研学</span>
            <strong>{{ linkedTrack.title }}</strong>
            <p>{{ linkedTrack.subtitle }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="theme-strip heritage-float-card">
      <div class="theme-strip-copy">
        <p class="section-kicker">THEME NAVIGATOR</p>
        <h2>切换策展主题</h2>
        <p>主题切换会保留联动逻辑，但不再重新刷整页概览数据。</p>
      </div>
      <div class="theme-chip-grid">
        <button
          v-for="theme in CURATION_THEMES"
          :key="theme.id"
          type="button"
          class="theme-chip heritage-float-card"
          :class="{ active: theme.id === selectedTheme }"
          @click="handleThemeSelect(theme.id)"
        >
          <span>{{ theme.title }}</span>
          <small>{{ theme.subtitle }}</small>
        </button>
      </div>
    </section>

    <section class="content-grid">
      <el-card class="collection-card heritage-float-card" shadow="never">
        <template #header>
          <div class="section-head">
            <div>
              <p class="section-kicker">CURATION OVERVIEW</p>
              <h2>{{ activeTheme.title }}</h2>
            </div>
            <el-tag effect="dark" type="danger">{{ filteredProjects.length }} 个项目</el-tag>
          </div>
        </template>

        <p class="theme-description">{{ activeTheme.description }}</p>

        <div class="integration-grid">
          <article class="integration-item heritage-float-card">
            <span>联动研学模板</span>
            <strong>{{ linkedTrack.title }}</strong>
            <p>{{ linkedTrack.audience }}</p>
          </article>
          <article class="integration-item heritage-float-card">
            <span>路线预设</span>
            <strong>{{ linkedTrailLabel }}</strong>
            <p>{{ linkedTrailHint }}</p>
          </article>
          <article class="integration-item heritage-float-card">
            <span>当前热区</span>
            <strong>{{ primaryRegion?.name || "待补充" }}</strong>
            <p>{{ primaryRegion ? `${primaryRegion.value} 个项目进入热点统计` : "将按主题自动生成路线重点区域" }}</p>
          </article>
        </div>

        <div class="collection-list">
          <article
            v-for="item in filteredProjects"
            :key="item.id"
            class="collection-item heritage-float-card"
            @click="goDetail(item.id)"
          >
            <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" class="item-cover" />
            <div class="item-content">
              <div class="item-topline">
                <el-tag size="small" effect="plain">{{ item.protectLevel || "未定级" }}</el-tag>
                <span>{{ item.regionName || "地区待补充" }}</span>
              </div>
              <h3>{{ item.name }}</h3>
              <p>{{ summarizeRichText(item.history, 86) }}</p>
              <div class="item-footer">
                <span>{{ getCategoryName(item.categoryId) }}</span>
                <span>{{ item.inheritorNames || "传承人信息待补充" }}</span>
              </div>
            </div>
          </article>

          <el-empty
            v-if="!filteredProjects.length"
            description="当前主题下还没有可展示的项目"
            :image-size="120"
          />
        </div>
      </el-card>

      <div class="side-column">
        <el-card class="action-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">CURATION FLOW</p>
                <h2>联动动作</h2>
              </div>
            </div>
          </template>

          <div class="action-list">
            <button type="button" class="action-item heritage-float-card" @click="goLinkedLearningStudio">
              <strong>进入研学工坊</strong>
              <p>把当前主题直接映射成适合课堂、社区或亲子场景的研学方案。</p>
            </button>
            <button type="button" class="action-item heritage-float-card" @click="goLinkedTrail">
              <strong>生成主题路线</strong>
              <p>按主题门类、热点地区和推荐停留时长，快速生成可游览的非遗路线。</p>
            </button>
            <button type="button" class="action-item heritage-float-card" @click="goRegionCategory">
              <strong>联动地区分类</strong>
              <p>继续按区域筛选非遗项目，适合做地方文化专题与区域传播。</p>
            </button>
          </div>
        </el-card>

        <el-card class="hot-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">HOT PICKS</p>
                <h2>本周热榜</h2>
              </div>
            </div>
          </template>

          <div class="hot-list">
            <button
              v-for="(item, index) in displayHotProjects"
              :key="item.id || index"
              type="button"
              class="hot-item heritage-float-card"
              @click="goDetail(item.id)"
            >
              <span class="hot-rank">{{ index + 1 }}</span>
              <div class="hot-copy">
                <strong>{{ item.name }}</strong>
                <span>{{ item.viewCount || 0 }} 次浏览</span>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>

            <el-empty
              v-if="!displayHotProjects.length"
            description="当前主题下还没有联动热点项目"
              :image-size="96"
            />
          </div>
        </el-card>

        <el-card class="region-card heritage-float-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">REGION FOCUS</p>
                <h2>热门地区</h2>
              </div>
            </div>
          </template>

          <div class="region-list">
            <button
              v-for="item in topRegions"
              :key="item.name"
              type="button"
              class="region-item heritage-float-card"
              @click="goRegionTrail(item.name)"
            >
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.value }} 个项目进入热点统计，点击带入主题路线</p>
              </div>
              <span class="region-badge">{{ item.value }}</span>
            </button>
          </div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowRight } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { CURATION_THEMES, LEARNING_TRACKS } from "@/constants/heritage";
import { useHeritageOverview } from "@/composables/useHeritageOverview";
import type { HeritageProject } from "@/types/project";
import { buildCustomTrailId, createDefaultTrailPlanner } from "@/utils/trail";
import { buildStaticUrl } from "@/utils/url";
import { filterProjectsByCategories, getCategoryName, sortRegionsByValue, summarizeRichText } from "@/utils/heritage";

interface DisplayHotProject {
  id: number;
  name: string;
  viewCount: number;
  categoryId?: number | null;
}

const router = useRouter();
const route = useRoute();
const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const selectedTheme = ref(CURATION_THEMES[0].id);
const { projects, hotProjects, regionStats, loading, load } = useHeritageOverview();

const activeTheme = computed(
  () => CURATION_THEMES.find((item) => item.id === selectedTheme.value) || CURATION_THEMES[0]
);
const filteredProjects = computed(() =>
  filterProjectsByCategories(projects.value, activeTheme.value.categoryIds).slice(0, 6)
);
const topRegions = computed(() => sortRegionsByValue(regionStats.value).slice(0, 5));
const primaryRegion = computed(() => topRegions.value[0] || null);
const projectLookup = computed(
  () =>
    new Map(
      projects.value
        .filter((item): item is HeritageProject & { id: number } => Number.isFinite(Number(item.id)))
        .map((item) => [Number(item.id), item])
    )
);
const linkedTrack = computed(
  () => LEARNING_TRACKS.find((item) => item.linkedThemeId === activeTheme.value.id) || LEARNING_TRACKS[0]
);
const displayHotProjects = computed<DisplayHotProject[]>(() => {
  const resolvedHotProjects = hotProjects.value
    .map((item) => {
      const projectId = Number(item.id);
      if (!Number.isFinite(projectId) || projectId <= 0) {
        return null;
      }

      const projectDetail = projectLookup.value.get(projectId);
      return {
        id: projectId,
        name: projectDetail?.name || item.name || "未命名项目",
        viewCount: Number(projectDetail?.viewCount ?? item.viewCount ?? 0),
        categoryId: projectDetail?.categoryId ?? null,
      };
    })
    .filter((item): item is DisplayHotProject => Boolean(item));

  const themedHotProjects = resolvedHotProjects.filter((item) =>
    activeTheme.value.categoryIds.includes(Number(item.categoryId))
  );

  return (themedHotProjects.length > 0 ? themedHotProjects : resolvedHotProjects).slice(0, 6);
});
const linkedPlanner = computed(() => ({
  ...createDefaultTrailPlanner(),
  interestIds: [...activeTheme.value.categoryIds],
  ...linkedTrack.value.trailPreset,
  regionKeyword: linkedTrack.value.trailPreset.regionKeyword || primaryRegion.value?.name || "",
}));
const linkedTrailLabel = computed(() => {
  const transportLabel =
    linkedPlanner.value.transportMode === "walk"
      ? "步行"
      : linkedPlanner.value.transportMode === "car"
        ? "自驾"
        : "公共交通";
  return `${linkedPlanner.value.maxStops} 个点位 / ${transportLabel}`;
});
const linkedTrailHint = computed(
  () =>
    `建议 ${linkedPlanner.value.durationKey} 内完成，预算 ${linkedPlanner.value.budgetLevel} 档，${linkedPlanner.value.preferHot ? "优先热点项目" : "均衡探索项目"}`
);

const buildTrailQuery = (regionName?: string) => {
  const planner = {
    ...linkedPlanner.value,
    regionKeyword: regionName || linkedPlanner.value.regionKeyword,
  };
  return {
    mode: "custom",
    trail: buildCustomTrailId(planner),
    interests: planner.interestIds.join(","),
    duration: planner.durationKey,
    transport: planner.transportMode,
    budget: planner.budgetLevel,
    stops: String(planner.maxStops),
    hot: planner.preferHot ? "1" : "0",
    ...(planner.regionKeyword ? { region: planner.regionKeyword } : {}),
  };
};

const applyThemeQuery = (value: unknown) => {
  if (typeof value !== "string") {
    return;
  }
  if (CURATION_THEMES.some((item) => item.id === value)) {
    selectedTheme.value = value;
  }
};

const syncThemeQuery = async (themeId: string) => {
  if (route.query.theme === themeId) {
    return;
  }

  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      theme: themeId,
    },
  });
};

const handleThemeSelect = async (themeId: string) => {
  if (selectedTheme.value === themeId) {
    return;
  }

  selectedTheme.value = themeId;
  await syncThemeQuery(themeId);
};

const goDetail = (id?: number | null) => {
  if (!Number.isFinite(Number(id)) || Number(id) <= 0) {
    return;
  }

  router.push({ path: "/home", query: { id } });
};

const goHome = () => {
  router.push("/home");
};

const goRegionCategory = () => {
  router.push("/region-category");
};

const goLinkedLearningStudio = () => {
  router.push({ path: "/learning-studio", query: { track: linkedTrack.value.id } });
};

const goLinkedTrail = () => {
  router.push({ path: "/heritage-trail", query: buildTrailQuery() });
};

const goRegionTrail = (regionName: string) => {
  router.push({ path: "/heritage-trail", query: buildTrailQuery(regionName) });
};

onMounted(async () => {
  applyThemeQuery(route.query.theme);
  try {
    await load();
  } catch (error) {
    console.error("Failed to load curation overview", error);
    ElMessage.error("主题策展数据加载失败，请稍后重试");
  }
});

watch(
  () => route.query.theme,
  (value) => {
    applyThemeQuery(value);
  }
);
</script>

<style scoped>
.curation-page {
  min-height: calc(100vh - 84px);
  background:
    radial-gradient(circle at top left, rgba(196, 30, 58, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(230, 162, 60, 0.16), transparent 35%),
    #f6f1ea;
  padding: 10px 6px 24px;
}

.hero {
  display: grid;
  grid-template-columns: 1.4fr 0.95fr;
  gap: 18px;
  margin-bottom: 18px;
}

.hero-copy,
.hero-panel,
.collection-card,
.action-card,
.hot-card,
.region-card {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(140, 110, 82, 0.12);
  border-radius: 28px;
  box-shadow: var(--heritage-card-shadow-rest);
  backdrop-filter: blur(10px);
}

.hero-copy {
  padding: 32px;
}

.hero-kicker,
.section-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #9b6b4a;
}

.hero-copy h1,
.section-head h2,
.item-content h3,
.integration-item strong,
.action-item strong {
  margin: 0;
  color: #2f241f;
  font-weight: 700;
}

.hero-copy h1 {
  font-size: 42px;
  line-height: 1.08;
}

.hero-desc,
.theme-description,
.item-content p,
.hot-copy span,
.region-item p,
.action-item p,
.integration-item p {
  color: #5c4b43;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-panel {
  padding: 24px;
  display: grid;
  gap: 16px;
}

.panel-grid,
.integration-grid,
.collection-list,
.hot-list,
.region-list,
.action-list,
.side-column {
  display: grid;
  gap: 16px;
}

.panel-grid {
  grid-template-columns: repeat(2, 1fr);
}

.panel-card,
.integration-item,
.action-item {
  border-radius: 20px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(196, 30, 58, 0.08), rgba(230, 162, 60, 0.06));
}

.panel-card.headline {
  min-height: 148px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.panel-card strong {
  font-size: 30px;
  color: #2f241f;
}

.panel-card p,
.panel-label,
.item-topline,
.item-footer,
.hot-copy span,
.region-item p,
.action-item p {
  color: #725d53;
}

.panel-label {
  font-size: 12px;
}

.theme-strip {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.28fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 24px;
  border: 1px solid rgba(140, 110, 82, 0.12);
  box-shadow: var(--heritage-card-shadow-rest);
}

.theme-strip-copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.theme-strip-copy h2 {
  margin: 0;
  color: #2f241f;
}

.theme-strip-copy p {
  margin: 0;
  color: #6c584c;
  line-height: 1.8;
}

.theme-chip-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.theme-chip {
  border: 0;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 18px;
  padding: 14px 18px;
  min-width: 0;
  min-height: 96px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 10px 28px rgba(75, 55, 40, 0.06);
}

.theme-chip span {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #2f241f;
}

.theme-chip small {
  display: block;
  margin-top: 6px;
  color: #7a665d;
}

.theme-chip.active,
.theme-chip:hover {
  box-shadow: 0 14px 30px rgba(196, 30, 58, 0.12);
  background: linear-gradient(135deg, rgba(196, 30, 58, 0.1), rgba(230, 162, 60, 0.08));
}

.content-grid {
  display: grid;
  grid-template-columns: 1.56fr 0.84fr;
  gap: 18px;
}

.collection-card,
.action-card,
.hot-card,
.region-card {
  padding: 6px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-head.compact {
  align-items: flex-end;
}

.integration-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0 0 20px;
}

.collection-item {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 18px;
  border-radius: 22px;
  padding: 16px;
  background: linear-gradient(180deg, #fff, #f9f5f0);
  border: 1px solid rgba(181, 154, 129, 0.18);
  cursor: pointer;
}

.item-cover {
  width: 100%;
  height: 148px;
  border-radius: 16px;
  object-fit: cover;
}

.item-topline,
.item-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
}

.item-content h3 {
  margin: 12px 0 8px;
  font-size: 22px;
}

.item-content p {
  margin: 0 0 14px;
}

.action-item,
.hot-item,
.region-item {
  width: 100%;
  border: none;
  text-align: left;
  cursor: pointer;
}

.hot-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  border-radius: 16px;
  padding: 14px;
  background: linear-gradient(180deg, #fff, #f9f3ee);
  color: inherit;
}

.hot-rank,
.region-badge {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #c41e3a;
  color: #fff;
  font-weight: 700;
}

.hot-copy strong,
.region-item strong,
.action-item strong {
  color: #2f241f;
}

.region-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fff, #fbf8f4);
}

@media (max-width: 1100px) {
  .hero,
  .content-grid,
  .theme-strip {
    grid-template-columns: 1fr;
  }

  .theme-chip-grid,
  .integration-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-copy {
    padding: 24px;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .collection-item,
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
