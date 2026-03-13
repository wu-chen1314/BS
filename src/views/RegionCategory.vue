<template>
  <div class="region-page">
    <section class="hero heritage-float-card" v-loading="bootLoading" element-loading-text="正在加载地区概览...">
      <div class="hero-copy">
        <p class="page-kicker">REGION DISCOVERY</p>
        <h1>地区分类浏览</h1>
        <p class="page-desc">
          这个页面现在也统一成更轻的工作台节奏了。筛选切换只刷新必要的项目列表和统计视角，不再为了重置筛选重新刷整页基础数据。
        </p>
      </div>

      <div class="hero-stats">
        <article v-for="item in heroStats" :key="item.label" class="hero-stat heritage-float-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.help }}</p>
        </article>
      </div>
    </section>

    <section class="filter-panel heritage-float-card">
      <div class="filter-copy">
        <p class="section-kicker">FILTER WORKBENCH</p>
        <h2>筛选工作台</h2>
        <p>按省份、城市和类别组合定位项目，筛选结果会直接联动右侧摘要和下方项目卡片。</p>
      </div>

      <div class="filter-grid">
        <div class="filter-item">
          <label class="filter-label">省份</label>
          <el-select
            v-model="selectedProvince"
            placeholder="选择省份"
            clearable
            @change="handleProvinceChange"
          >
            <el-option v-for="province in provinces" :key="province.id" :label="province.name" :value="province.id" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">城市</label>
          <el-select
            v-model="selectedCity"
            placeholder="选择城市"
            clearable
            :loading="citiesLoading"
            :disabled="!selectedProvince || citiesLoading"
            @change="handleCityChange"
          >
            <el-option v-for="city in cities" :key="city.id" :label="city.name" :value="city.id" />
          </el-select>
        </div>

        <div class="filter-item wide">
          <label class="filter-label">类别</label>
          <el-cascader
            v-model="selectedCategory"
            :options="categoryTree"
            clearable
            placeholder="选择非遗类别"
            :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children' }"
            @change="handleCategoryChange"
          />
        </div>

        <div class="filter-actions">
          <el-button type="primary" round :icon="Refresh" :disabled="!hasActiveFilters" @click="resetFilters">
            重置筛选
          </el-button>
        </div>
      </div>

      <div class="active-filters">
        <span v-for="tag in activeFilterTags" :key="tag" class="active-tag">{{ tag }}</span>
        <span v-if="activeFilterTags.length === 0" class="empty-tag">当前未设置筛选条件，展示全量项目。</span>
      </div>
    </section>

    <section class="content-grid">
      <article class="project-panel heritage-float-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">PROJECT RESULT</p>
            <h2>项目结果</h2>
            <p class="section-desc">保留稳定分页，当前每次只刷新项目结果区，避免切换筛选时整页抖动。</p>
          </div>
          <div class="section-meta">
            <span class="meta-chip">共 {{ total }} 条</span>
            <span class="meta-chip">{{ currentRegionLabel }}</span>
            <span class="meta-chip">{{ currentCategoryLabel }}</span>
          </div>
        </div>

        <div class="project-list-shell" v-loading="projectLoading" element-loading-text="正在加载项目结果...">
          <el-empty
            v-if="!projectLoading && projectList.length === 0"
            description="当前筛选下还没有符合条件的项目"
            :image-size="118"
          />

          <div v-else class="project-grid">
            <article
              v-for="item in projectList"
              :key="item.id"
              class="project-card heritage-float-card"
              @click="handleRowClick(item)"
            >
              <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" class="project-cover" />

              <div class="project-body">
                <div class="project-head">
                  <h3>{{ item.name }}</h3>
                  <el-tag :type="getProtectLevelType(item.protectLevel)" size="small" effect="dark" round>
                    {{ item.protectLevel || "未定级" }}
                  </el-tag>
                </div>

                <div class="project-meta">
                  <el-tag type="info" size="small" effect="plain" round>
                    {{ item.categoryName || "未分类" }}
                  </el-tag>
                  <el-tag type="success" size="small" effect="plain" round>
                    {{ item.regionName || "未设置地区" }}
                  </el-tag>
                  <el-tag size="small" effect="plain" round>
                    {{ item.status || "在传承" }}
                  </el-tag>
                </div>

                <p>{{ summarizeProject(item) }}</p>

                <div class="project-footer">
                  <span>{{ item.inheritorNames || "传承人信息待补充" }}</span>
                  <strong>{{ Number(item.viewCount || 0) }} 次浏览</strong>
                </div>

                <div class="project-actions">
                  <el-button type="primary" plain round @click.stop="viewDetail(item.id)">查看详情</el-button>
                </div>
              </div>
            </article>
          </div>
        </div>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="limit"
            :page-sizes="[8, 16, 24, 48]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </article>

      <aside class="insight-column">
        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">FILTER SUMMARY</p>
              <h2>当前筛选摘要</h2>
            </div>
          </div>

          <div class="summary-list">
            <div class="summary-item">
              <span>地区视角</span>
              <strong>{{ currentRegionLabel }}</strong>
            </div>
            <div class="summary-item">
              <span>类别视角</span>
              <strong>{{ currentCategoryLabel }}</strong>
            </div>
            <div class="summary-item">
              <span>当前分页</span>
              <strong>第 {{ page }} 页 / {{ limit }} 条</strong>
            </div>
          </div>
        </article>

        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">REGION STATS</p>
              <h2>地区统计</h2>
            </div>
          </div>

          <div class="metric-stack">
            <div class="metric-row">
              <span>项目总数</span>
              <strong>{{ regionStats.totalProjects }}</strong>
            </div>
            <div class="metric-row">
              <span>覆盖类别</span>
              <strong>{{ regionStats.categoryCount }}</strong>
            </div>
            <div class="metric-row">
              <span>传承人数</span>
              <strong>{{ regionStats.inheritorCount }}</strong>
            </div>
          </div>
        </article>

        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">CATEGORY STATS</p>
              <h2>类别统计</h2>
            </div>
          </div>

          <div class="metric-stack">
            <div class="metric-row">
              <span>项目总数</span>
              <strong>{{ categoryStats.totalProjects }}</strong>
            </div>
            <div class="metric-row">
              <span>覆盖地区</span>
              <strong>{{ categoryStats.regionCount }}</strong>
            </div>
            <div class="metric-row">
              <span>传承人数</span>
              <strong>{{ categoryStats.inheritorCount }}</strong>
            </div>
          </div>

          <div class="hint-card">
            <strong>联动说明</strong>
            <p>省份和城市控制地区统计口径，类别级联控制类别统计口径，两者会同时作用到下方项目结果。</p>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import {
  fetchRegionCategoryBootstrap,
  fetchRegionCategoryCategoryStats,
  fetchRegionCategoryCities,
  fetchRegionCategoryProjects,
  fetchRegionCategoryRegionStats,
} from "@/services/region-category";
import type {
  RegionCategoryBootstrapData,
  RegionCategoryCategoryStats,
  RegionCategoryNode,
  RegionCategoryOption,
  RegionCategoryRegionStats,
} from "@/types/region-category";
import type { HeritageProject } from "@/types/project";
import { getProtectLevelType, summarizeRichText } from "@/utils/heritage";
import { buildStaticUrl } from "@/utils/url";

const router = useRouter();
const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const createEmptyRegionStats = (): RegionCategoryRegionStats => ({
  totalProjects: 0,
  categoryCount: 0,
  inheritorCount: 0,
});

const createEmptyCategoryStats = (): RegionCategoryCategoryStats => ({
  totalProjects: 0,
  regionCount: 0,
  inheritorCount: 0,
});

const getErrorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

const normalizeRegionStats = (
  stats?: Partial<RegionCategoryRegionStats> | null
): RegionCategoryRegionStats => ({
  totalProjects: Number(stats?.totalProjects || 0),
  categoryCount: Number(stats?.categoryCount || 0),
  inheritorCount: Number(stats?.inheritorCount || 0),
});

const normalizeCategoryStats = (
  stats?: Partial<RegionCategoryCategoryStats> | null
): RegionCategoryCategoryStats => ({
  totalProjects: Number(stats?.totalProjects || 0),
  regionCount: Number(stats?.regionCount || 0),
  inheritorCount: Number(stats?.inheritorCount || 0),
});

const provinces = ref<RegionCategoryOption[]>([]);
const cities = ref<RegionCategoryOption[]>([]);
const categoryTree = ref<RegionCategoryNode[]>([]);
const projectList = ref<HeritageProject[]>([]);

const selectedProvince = ref<number | null>(null);
const selectedCity = ref<number | null>(null);
const selectedCategory = ref<number[]>([]);

const page = ref(1);
const limit = ref(8);
const total = ref(0);
const bootLoading = ref(false);
const projectLoading = ref(false);
const citiesLoading = ref(false);

const regionStats = ref<RegionCategoryRegionStats>(createEmptyRegionStats());
const categoryStats = ref<RegionCategoryCategoryStats>(createEmptyCategoryStats());
const currentRegionId = computed(() => selectedCity.value || selectedProvince.value || undefined);
const currentCategoryId = computed(() =>
  selectedCategory.value.length > 0 ? selectedCategory.value[selectedCategory.value.length - 1] : undefined
);
const hasActiveFilters = computed(
  () => Boolean(selectedProvince.value || selectedCity.value || selectedCategory.value.length > 0)
);
const projectQuery = computed(() => ({
  page: page.value,
  limit: limit.value,
  ...(currentRegionId.value ? { regionId: currentRegionId.value } : {}),
  ...(currentCategoryId.value ? { categoryId: currentCategoryId.value } : {}),
}));

const findOptionLabel = (list: RegionCategoryOption[], id?: number | null, fallback = "全部地区") => {
  const matched = list.find((item) => item.id === id);
  return matched?.name || fallback;
};

const findCategoryPath = (nodes: RegionCategoryNode[], targetId?: number): string[] => {
  for (const node of nodes) {
    if (node.id === targetId) {
      return [node.name];
    }
    if (node.children?.length) {
      const childPath = findCategoryPath(node.children, targetId);
      if (childPath.length) {
        return [node.name, ...childPath];
      }
    }
  }
  return [];
};

const currentProvinceLabel = computed(() => findOptionLabel(provinces.value, selectedProvince.value, "全部省份"));
const currentCityLabel = computed(() => findOptionLabel(cities.value, selectedCity.value, "全部城市"));
const currentRegionLabel = computed(() => (selectedCity.value ? currentCityLabel.value : currentProvinceLabel.value));
const currentCategoryLabel = computed(() => {
  const path = findCategoryPath(categoryTree.value, currentCategoryId.value);
  return path.length ? path.join(" / ") : "全部类别";
});

const activeFilterTags = computed(() => {
  const tags: string[] = [];
  if (selectedProvince.value) {
    tags.push(`省份：${currentProvinceLabel.value}`);
  }
  if (selectedCity.value) {
    tags.push(`城市：${currentCityLabel.value}`);
  }
  if (currentCategoryId.value) {
    tags.push(`类别：${currentCategoryLabel.value}`);
  }
  return tags;
});

const heroStats = computed(() => [
  {
    help: "当前筛选结果中的项目数量",
    label: "项目结果",
    value: total.value,
  },
  {
    help: "当前地区视角覆盖的类别数",
    label: "地区类别覆盖",
    value: regionStats.value.categoryCount,
  },
  {
    help: "当前类别视角覆盖的地区数",
    label: "类别地区覆盖",
    value: categoryStats.value.regionCount,
  },
  {
    help: "当前统计视角下的传承人数量",
    label: "传承人",
    value: Math.max(regionStats.value.inheritorCount, categoryStats.value.inheritorCount),
  },
]);

const summarizeProject = (project: HeritageProject) =>
  summarizeRichText(project.features || project.history || "项目简介正在补充中。", 72);

onMounted(() => {
  void loadBootstrap();
});

const applyBootstrapData = (data?: RegionCategoryBootstrapData | null) => {
  provinces.value = data?.provinces || [];
  categoryTree.value = data?.categoryTree || [];
  regionStats.value = normalizeRegionStats(data?.regionStatistics);
  categoryStats.value = normalizeCategoryStats(data?.categoryStatistics);
};

const loadBootstrap = async () => {
  bootLoading.value = true;
  try {
    const res = await fetchRegionCategoryBootstrap();
    applyBootstrapData(res.data.data);
    await loadProjects();
  } catch (error) {
    console.error("Failed to load region category data", error);
    provinces.value = [];
    cities.value = [];
    categoryTree.value = [];
    projectList.value = [];
    total.value = 0;
    regionStats.value = createEmptyRegionStats();
    categoryStats.value = createEmptyCategoryStats();
    ElMessage.error(getErrorMessage(error, "加载地区分类数据失败"));
  } finally {
    bootLoading.value = false;
  }
};

const loadProjects = async () => {
  projectLoading.value = true;
  try {
    const res = await fetchRegionCategoryProjects(projectQuery.value);
    const data = res.data.data;
    projectList.value = data?.records || [];
    total.value = Number(data?.total || 0);
  } catch (error) {
    console.error("Failed to load projects", error);
    projectList.value = [];
    total.value = 0;
    ElMessage.error(getErrorMessage(error, "加载项目列表失败"));
  } finally {
    projectLoading.value = false;
  }
};

const loadCities = async (provinceId?: number | null) => {
  if (!provinceId) {
    cities.value = [];
    return;
  }

  citiesLoading.value = true;
  try {
    const res = await fetchRegionCategoryCities(provinceId);
    cities.value = res.data.data || [];
  } catch (error) {
    console.error("Failed to load cities", error);
    cities.value = [];
    ElMessage.error(getErrorMessage(error, "加载城市列表失败"));
  } finally {
    citiesLoading.value = false;
  }
};

const refreshStatistics = async () => {
  const [regionResult, categoryResult] = await Promise.allSettled([
    fetchRegionCategoryRegionStats(currentRegionId.value),
    fetchRegionCategoryCategoryStats(currentCategoryId.value),
  ]);

  if (regionResult.status === "fulfilled") {
    regionStats.value = normalizeRegionStats(regionResult.value.data.data);
  } else {
    console.error("Failed to update region statistics", regionResult.reason);
    regionStats.value = createEmptyRegionStats();
  }

  if (categoryResult.status === "fulfilled") {
    categoryStats.value = normalizeCategoryStats(categoryResult.value.data.data);
  } else {
    console.error("Failed to update category statistics", categoryResult.reason);
    categoryStats.value = createEmptyCategoryStats();
  }
};

const applyFilters = async () => {
  await Promise.all([loadProjects(), refreshStatistics()]);
};

const handleProvinceChange = async (provinceId: number | null) => {
  selectedCity.value = null;
  cities.value = [];
  page.value = 1;
  await loadCities(provinceId);
  await applyFilters();
};

const handleCityChange = async () => {
  page.value = 1;
  await applyFilters();
};

const handleCategoryChange = async () => {
  page.value = 1;
  await applyFilters();
};

const resetFilters = async () => {
  selectedProvince.value = null;
  selectedCity.value = null;
  selectedCategory.value = [];
  cities.value = [];
  page.value = 1;
  await applyFilters();
};

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage;
  await loadProjects();
};

const handlePageSizeChange = async (nextSize: number) => {
  limit.value = nextSize;
  page.value = 1;
  await loadProjects();
};

const handleRowClick = (row: HeritageProject) => {
  viewDetail(row.id);
};

const viewDetail = (projectId?: number | null) => {
  if (!Number.isFinite(Number(projectId)) || Number(projectId) <= 0) {
    return;
  }

  router.push({ path: "/home", query: { id: projectId } });
};
</script>

<style scoped>
.region-page {
  min-height: calc(100vh - 84px);
  padding: 12px 6px 28px;
  display: grid;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(212, 175, 55, 0.1), transparent 26%),
    linear-gradient(180deg, var(--heritage-paper-soft) 0%, var(--heritage-paper) 100%);
}

.hero,
.filter-panel,
.project-panel,
.insight-card {
  border-radius: 26px;
  border: 1px solid rgba(113, 72, 44, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(192, 57, 43, 0.12), transparent 30%),
    linear-gradient(135deg, rgba(255, 249, 241, 0.96), rgba(255, 255, 255, 0.98));
}

.hero-copy h1,
.section-head h2,
.filter-copy h2,
.project-head h3 {
  margin: 0;
  color: var(--heritage-ink);
}

.page-kicker,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--heritage-gold);
}

.page-desc,
.filter-copy p,
.section-desc,
.project-body p,
.hint-card p {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  border-radius: 20px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.hero-stat span {
  color: var(--heritage-ink-soft);
}

.hero-stat strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 32px;
  color: var(--heritage-primary);
}

.filter-panel {
  display: grid;
  gap: 18px;
  padding: 22px 24px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: end;
}

.filter-item,
.filter-actions {
  display: grid;
  gap: 8px;
}

.filter-item.wide {
  grid-column: span 2;
}

.filter-label {
  font-size: 14px;
  color: var(--heritage-muted);
}

.filter-item :deep(.el-select),
.filter-item :deep(.el-cascader) {
  width: 100%;
}

.active-filters,
.section-meta,
.project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.active-tag,
.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.08);
  color: var(--heritage-primary);
  font-size: 13px;
}

.empty-tag {
  color: var(--heritage-ink-soft);
  font-size: 13px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.38fr) minmax(280px, 0.82fr);
  gap: 18px;
  align-items: start;
}

.project-panel,
.insight-card {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.section-head.compact {
  align-items: flex-start;
}

.project-list-shell {
  min-height: 320px;
  margin-top: 18px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.project-card {
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 251, 244, 0.96), rgba(251, 246, 239, 0.92));
  border: 1px solid rgba(192, 138, 63, 0.16);
  box-shadow: 0 14px 30px rgba(72, 41, 28, 0.08);
  cursor: pointer;
}

.project-cover {
  width: 100%;
  height: 190px;
  object-fit: cover;
}

.project-body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.project-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.project-head h3 {
  font-size: 20px;
  line-height: 1.5;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--heritage-ink-soft);
  font-size: 13px;
  flex-wrap: wrap;
}

.project-footer strong {
  color: var(--heritage-primary);
}

.project-actions {
  display: flex;
  justify-content: flex-start;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.insight-column {
  display: grid;
  gap: 18px;
}

.summary-list,
.metric-stack {
  display: grid;
  gap: 12px;
}

.summary-item,
.metric-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.summary-item span,
.metric-row span {
  color: var(--heritage-ink-soft);
}

.summary-item strong,
.metric-row strong {
  color: var(--heritage-ink);
}

.hint-card {
  margin-top: 16px;
  border-radius: 18px;
  padding: 16px;
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
  .project-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-item.wide {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .region-page {
    padding: 10px 0 20px;
  }

  .hero,
  .filter-panel,
  .project-panel,
  .insight-card {
    padding: 18px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .section-head,
  .project-head,
  .project-footer,
  .summary-item,
  .metric-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .project-cover {
    height: 180px;
  }

  .project-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
