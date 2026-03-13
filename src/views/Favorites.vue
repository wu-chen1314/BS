<template>
  <div class="favorites-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">MY HERITAGE FAVORITES</p>
        <h1>我的非遗收藏</h1>
        <p class="page-desc">
          收藏页现在也统一成了更轻的工作台节奏。进入页面只刷新收藏结果区，取消收藏后会直接同步分页和摘要，不再反复抖动整页。
        </p>
      </div>

      <div class="hero-metrics">
        <article v-for="item in heroMetrics" :key="item.label" class="metric-card heritage-float-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.help }}</p>
        </article>
      </div>
    </section>

    <section class="content-grid">
      <article class="collection-panel heritage-float-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">COLLECTION LIST</p>
            <h2>收藏列表</h2>
            <p class="section-desc">保留分页和取消收藏能力，点击卡片仍然可以回到首页项目详情继续浏览。</p>
          </div>
          <div class="section-meta">
            <span class="meta-chip">共 {{ total }} 项</span>
            <span class="meta-chip">第 {{ currentPage }} 页</span>
            <span class="meta-chip">每页 {{ pageSize }} 项</span>
          </div>
        </div>

        <div class="collection-shell" v-loading="loading" element-loading-text="正在加载收藏列表...">
          <el-empty
            v-if="!loading && favoritesList.length === 0"
            description="当前还没有收藏项目"
            :image-size="150"
          >
            <el-button type="primary" round @click="$router.push('/home')">去首页看看</el-button>
          </el-empty>

          <div v-else class="favorites-grid">
            <article
              v-for="item in favoritesList"
              :key="item.id"
              class="favorite-card heritage-float-card"
              @click="handleView(item)"
            >
              <div class="card-cover">
                <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" />
                <div class="card-cover-overlay">
                  <el-button link type="primary" class="overlay-action">查看详情</el-button>
                </div>
              </div>

              <div class="card-body">
                <div class="card-topline">
                  <el-tag size="small" :type="getProtectLevelType(item.protectLevel)" effect="dark" round>
                    {{ item.protectLevel || "待补充" }}
                  </el-tag>
                  <span>{{ item.regionName || "地区待补充" }}</span>
                </div>

                <h3>{{ item.name }}</h3>
                <p>{{ summarizeRichText(item.history || item.features, 72) }}</p>

                <div class="card-tags">
                  <el-tag size="small" effect="plain">{{ item.categoryName || getCategoryName(item.categoryId) }}</el-tag>
                  <el-tag size="small" effect="plain" type="info">{{ item.inheritorNames || "传承人待补充" }}</el-tag>
                </div>

                <div class="card-footer">
                  <div>
                    <span class="footer-label">浏览热度</span>
                    <strong>{{ item.viewCount || 0 }}</strong>
                  </div>
                  <el-button
                    type="warning"
                    plain
                    round
                    :loading="busyId === Number(item.id)"
                    @click.stop="toggleCardFavorite(Number(item.id))"
                  >
                    取消收藏
                  </el-button>
                </div>
              </div>
            </article>
          </div>
        </div>

        <div v-if="total > pageSize" class="pagination-row">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </article>

      <aside class="insight-column">
        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">COLLECTION SUMMARY</p>
              <h2>收藏摘要</h2>
            </div>
          </div>

          <div class="summary-list">
            <div class="summary-item">
              <span>收藏总数</span>
              <strong>{{ total }}</strong>
            </div>
            <div class="summary-item">
              <span>当前页数量</span>
              <strong>{{ favoritesList.length }}</strong>
            </div>
            <div class="summary-item">
              <span>平均热度</span>
              <strong>{{ averageViews }}</strong>
            </div>
          </div>
        </article>

        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">TOP PICK</p>
              <h2>收藏里最热的项目</h2>
            </div>
          </div>

          <el-empty v-if="!topFavorite" description="收藏项目后会在这里展示热度摘要" :image-size="88" />
          <button v-else type="button" class="top-pick heritage-float-card" @click="handleView(topFavorite)">
            <img :src="buildStaticUrl(topFavorite.coverUrl) || fallbackCover" :alt="topFavorite.name" class="top-pick-cover" />
            <div class="top-pick-copy">
              <strong>{{ topFavorite.name }}</strong>
              <p>{{ topFavorite.categoryName || getCategoryName(topFavorite.categoryId) }}</p>
              <span>{{ Number(topFavorite.viewCount || 0) }} 次浏览</span>
            </div>
          </button>
        </article>

        <article class="insight-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">QUICK GUIDE</p>
              <h2>收藏说明</h2>
            </div>
          </div>

          <div class="guide-list">
            <div class="guide-item">
              <strong>跨页面同步</strong>
              <p>首页、详情弹窗和收藏页共享同一套收藏状态，不会再出现取消后延迟刷新的断点。</p>
            </div>
            <div class="guide-item">
              <strong>分页稳定</strong>
              <p>取消收藏后会优先保留当前分页；如果当前页被清空，会自动退回上一页。</p>
            </div>
            <div class="guide-item">
              <strong>继续浏览</strong>
              <p>点击任意收藏卡片都能回到首页详情页，继续查看项目、评论和热度变化。</p>
            </div>
          </div>
        </article>
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
import { toggleFavorite } from "@/services/project";
import type { HeritageProject } from "@/types/project";
import { getAverageViewCount, getCategoryName, getProtectLevelType, summarizeRichText } from "@/utils/heritage";
import { showSuccess, successText } from "@/utils/uiFeedback";
import { buildStaticUrl } from "@/utils/url";

const router = useRouter();
const hubStore = useHeritageHubStore();
const { favoriteProjects, favoriteProjectsTotal } = storeToRefs(hubStore);

const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const currentPage = ref(1);
const pageSize = ref(8);
const loading = ref(false);
const busyId = ref<number | null>(null);

const favoritesList = computed(() => favoriteProjects.value);
const total = computed(() => favoriteProjectsTotal.value);
const averageViews = computed(() =>
  getAverageViewCount(favoritesList.value as Array<{ viewCount?: number | null }>)
);
const topFavorite = computed(() =>
  [...favoritesList.value].sort((left, right) => Number(right.viewCount || 0) - Number(left.viewCount || 0))[0] || null
);
const heroMetrics = computed(() => [
  {
    help: "当前收藏总量",
    label: "收藏总数",
    value: total.value,
  },
  {
    help: "当前分页展示的项目数量",
    label: "当前页项目",
    value: favoritesList.value.length,
  },
  {
    help: "当前页项目的平均热度",
    label: "平均热度",
    value: averageViews.value,
  },
  {
    help: "当前所在分页",
    label: "分页位置",
    value: `P${currentPage.value}`,
  },
]);

const loadFavorites = async (page = currentPage.value) => {
  loading.value = true;
  currentPage.value = page;
  try {
    await hubStore.syncFavoriteProjects(currentPage.value, pageSize.value);
  } catch (error) {
    console.error("Failed to load favorites", error);
    ElMessage.error("获取收藏列表失败");
  } finally {
    loading.value = false;
  }
};

const toggleCardFavorite = async (projectId: number) => {
  busyId.value = projectId;
  try {
    const res = await toggleFavorite(projectId);
    hubStore.setProjectFavorite(projectId, Boolean(res.data.data));
    await loadFavorites(currentPage.value);

    if (currentPage.value > 1 && favoritesList.value.length === 0) {
      currentPage.value -= 1;
      await loadFavorites(currentPage.value);
    }

    showSuccess(successText.updated("收藏状态"));
  } catch (error) {
    console.error("Failed to toggle favorite", error);
    ElMessage.error("更新收藏失败");
  } finally {
    busyId.value = null;
  }
};

const handleView = (item: HeritageProject) => {
  router.push({ path: "/home", query: { id: item.id } });
};

const handlePageChange = async (page: number) => {
  await loadFavorites(page);
};

onMounted(async () => {
  await Promise.allSettled([loadFavorites(), hubStore.syncProjectFavoriteIds()]);
});
</script>

<style scoped>
.favorites-page {
  min-height: calc(100vh - 88px);
  display: grid;
  gap: 18px;
  padding: 12px 6px 28px;
}

.hero,
.collection-panel,
.insight-card {
  border-radius: 26px;
  border: 1px solid var(--heritage-border);
  background: rgba(255, 251, 244, 0.92);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(192, 57, 43, 0.12), transparent 28%),
    linear-gradient(135deg, rgba(255, 249, 241, 0.96), rgba(255, 255, 255, 0.98));
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
.favorite-card h3 {
  margin: 0;
  color: var(--heritage-ink);
}

.page-desc,
.section-desc,
.favorite-card p,
.guide-item p,
.top-pick-copy p {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border-radius: 20px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.metric-card span {
  color: var(--heritage-ink-soft);
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 32px;
  color: var(--heritage-primary);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(290px, 0.82fr);
  gap: 18px;
}

.collection-panel,
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

.section-meta,
.card-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.08);
  color: var(--heritage-primary);
  font-size: 13px;
}

.collection-shell {
  min-height: 340px;
  margin-top: 18px;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.favorite-card {
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff, #fbf6ef);
  border: 1px solid rgba(192, 138, 63, 0.16);
  cursor: pointer;
  box-shadow: 0 18px 36px rgba(72, 41, 28, 0.08);
}

.card-cover {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.36s ease;
}

.favorite-card:hover .card-cover img {
  transform: scale(1.05);
}

.card-cover-overlay {
  position: absolute;
  inset: auto 0 0;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, transparent, rgba(34, 49, 63, 0.68));
  opacity: 0;
  transition: opacity 0.24s ease;
}

.favorite-card:hover .card-cover-overlay {
  opacity: 1;
}

.overlay-action {
  color: var(--heritage-paper-soft) !important;
}

.card-body {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.card-topline,
.card-footer,
.project-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  color: var(--heritage-muted);
  font-size: 13px;
}

.footer-label {
  display: block;
  font-size: 12px;
  color: var(--heritage-muted);
}

.card-footer strong {
  color: var(--heritage-ink);
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

.insight-column {
  display: grid;
  gap: 18px;
}

.summary-list,
.guide-list {
  display: grid;
  gap: 12px;
}

.summary-item,
.guide-item {
  border-radius: 18px;
  padding: 16px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.summary-item span {
  color: var(--heritage-ink-soft);
}

.summary-item strong,
.guide-item strong,
.top-pick-copy strong {
  display: block;
  margin-top: 8px;
  color: var(--heritage-ink);
}

.top-pick {
  border: none;
  cursor: pointer;
  text-align: left;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(180deg, #fff, #fbf6ef);
  box-shadow: 0 14px 30px rgba(72, 41, 28, 0.08);
}

.top-pick-cover {
  width: 100%;
  height: 190px;
  object-fit: cover;
}

.top-pick-copy {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.top-pick-copy span {
  color: var(--heritage-primary);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .hero,
  .content-grid,
  .favorites-grid {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .favorites-page {
    padding: 10px 0 20px;
  }

  .hero,
  .collection-panel,
  .insight-card {
    padding: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .section-head,
  .card-topline,
  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-cover,
  .top-pick-cover {
    height: 180px;
  }
}
</style>
