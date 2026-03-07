<template>
  <div class="curation-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">INTANGIBLE HERITAGE CURATION</p>
        <h1>非遗主题策展</h1>
        <p class="hero-desc">
          把项目、地区和热度数据重新编排成更好逛的文化专题。你可以从技艺、节俗、声腔三个角度进入，快速找到值得深入阅读的非遗内容。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="selectedTheme = themes[0].id">进入今日策展</el-button>
          <el-button size="large" round @click="goHome">返回项目展馆</el-button>
        </div>
      </div>

      <div class="hero-panel">
        <div class="panel-card headline">
          <span class="panel-label">本期主题</span>
          <strong>{{ activeTheme.title }}</strong>
          <p>{{ activeTheme.subtitle }}</p>
        </div>
        <div class="panel-grid">
          <div class="panel-card">
            <span class="panel-label">项目采样</span>
            <strong>{{ stats.projectCount }}</strong>
            <p>已纳入专题筛选</p>
          </div>
          <div class="panel-card">
            <span class="panel-label">热门项目</span>
            <strong>{{ hotProjects.length }}</strong>
            <p>同步热度榜单</p>
          </div>
          <div class="panel-card">
            <span class="panel-label">覆盖地区</span>
            <strong>{{ stats.regionCount }}</strong>
            <p>来自地图统计</p>
          </div>
        </div>
      </div>
    </section>

    <section class="theme-strip">
      <button
        v-for="theme in themes"
        :key="theme.id"
        type="button"
        class="theme-chip"
        :class="{ active: theme.id === selectedTheme }"
        @click="selectedTheme = theme.id"
      >
        <span>{{ theme.title }}</span>
        <small>{{ theme.subtitle }}</small>
      </button>
    </section>

    <section class="content-grid">
      <el-card class="collection-card" shadow="never">
        <template #header>
          <div class="section-head">
            <div>
              <p class="section-kicker">专题导览</p>
              <h2>{{ activeTheme.title }}</h2>
            </div>
            <el-tag effect="dark" type="danger">{{ filteredProjects.length }} 个项目</el-tag>
          </div>
        </template>

        <p class="theme-description">{{ activeTheme.description }}</p>

        <div class="collection-list">
          <article v-for="item in filteredProjects" :key="item.id" class="collection-item" @click="goDetail(item.id)">
            <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" class="item-cover" />
            <div class="item-content">
              <div class="item-topline">
                <el-tag size="small" effect="plain">{{ item.protectLevel || '未定级' }}</el-tag>
                <span>{{ item.regionName || '地区待补充' }}</span>
              </div>
              <h3>{{ item.name }}</h3>
              <p>{{ summarize(item.history) }}</p>
              <div class="item-footer">
                <span>{{ getCategoryName(item.categoryId) }}</span>
                <span>{{ item.inheritorNames || '传承人信息待补充' }}</span>
              </div>
            </div>
          </article>

          <el-empty v-if="!filteredProjects.length" description="当前主题下暂无可展示项目" :image-size="120" />
        </div>
      </el-card>

      <div class="side-column">
        <el-card class="hot-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">热度联动</p>
                <h2>本周热荐</h2>
              </div>
            </div>
          </template>

          <div class="hot-list">
            <button v-for="(item, index) in hotProjects" :key="item.id || index" class="hot-item" @click="goDetail(item.id)">
              <span class="hot-rank">{{ index + 1 }}</span>
              <div class="hot-copy">
                <strong>{{ item.name }}</strong>
                <span>{{ item.viewCount || 0 }} 次浏览</span>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </el-card>

        <el-card class="region-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">地域微展</p>
                <h2>热门地区</h2>
              </div>
            </div>
          </template>

          <div class="region-list">
            <div v-for="item in topRegions" :key="item.name" class="region-item">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.value }} 个项目被统计收录</p>
              </div>
              <span class="region-badge">{{ item.value }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ArrowRight } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { buildStaticUrl } from "@/utils/url";

interface ThemeConfig {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  categoryIds: number[];
}

const router = useRouter();
const fallbackCover = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?q=80&w=1200&auto=format&fit=crop";

const themes: ThemeConfig[] = [
  {
    id: "craft",
    title: "守艺匠心",
    subtitle: "技艺与器物",
    description: "聚焦手工技艺、传统美术与器物之美，适合快速了解非遗里的工艺谱系与审美结构。",
    categoryIds: [7, 8, 9],
  },
  {
    id: "festival",
    title: "节序人间",
    subtitle: "节俗与民间叙事",
    description: "围绕民俗、民间文学和礼俗场景组织内容，更适合做节庆活动选题和校园传播。",
    categoryIds: [1, 10],
  },
  {
    id: "performance",
    title: "声腔流韵",
    subtitle: "戏剧、音乐与曲艺",
    description: "把传统戏剧、传统音乐与曲艺集中展示，适合从可听可看的入口进入非遗。",
    categoryIds: [2, 4, 5],
  },
];

const selectedTheme = ref(themes[0].id);
const projects = ref<any[]>([]);
const hotProjects = ref<any[]>([]);
const regionStats = ref<any[]>([]);

const stats = computed(() => ({
  projectCount: projects.value.length,
  regionCount: regionStats.value.length,
}));

const activeTheme = computed(() => themes.find((item) => item.id === selectedTheme.value) || themes[0]);

const filteredProjects = computed(() => {
  const categoryIds = activeTheme.value.categoryIds;
  return projects.value
    .filter((item) => categoryIds.includes(Number(item.categoryId)))
    .slice(0, 6);
});

const topRegions = computed(() => regionStats.value.slice(0, 5));

onMounted(async () => {
  await Promise.all([fetchProjects(), fetchHotProjects(), fetchMapStats()]);
});

const fetchProjects = async () => {
  const res = await request.get("/projects/page", {
    params: {
      pageNum: 1,
      pageSize: 60,
    },
  });
  if (res.data.code === 200) {
    projects.value = res.data.data?.records || [];
  }
};

const fetchHotProjects = async () => {
  const res = await request.get("/view/hot", { params: { limit: 6 } });
  if (res.data.code === 200) {
    hotProjects.value = res.data.data || [];
  }
};

const fetchMapStats = async () => {
  const res = await request.get("/statistics/map");
  if (res.data.code === 200) {
    regionStats.value = (res.data.data || []).sort((a: any, b: any) => Number(b.value || 0) - Number(a.value || 0));
  }
};

const getCategoryName = (id: number) => {
  const categoryMap: Record<number, string> = {
    1: "民间文学",
    2: "传统音乐",
    3: "传统舞蹈",
    4: "传统戏剧",
    5: "曲艺",
    6: "传统体育、游艺与杂技",
    7: "传统美术",
    8: "传统技艺",
    9: "传统医药",
    10: "民俗",
  };
  return categoryMap[id] || "其他类别";
};

const summarize = (html: string) => {
  if (!html) return "暂缺项目简介，可进入详情页查看后续补充内容。";
  const plain = html.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
  return plain.length > 80 ? `${plain.slice(0, 80)}...` : plain;
};

const goDetail = (id: number) => {
  router.push(`/home?id=${id}`);
};

const goHome = () => {
  router.push("/home");
};
</script>

<style scoped>
.curation-page {
  min-height: calc(100vh - 84px);
  background:
    radial-gradient(circle at top left, rgba(196, 30, 58, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(230, 162, 60, 0.16), transparent 35%),
    #f6f1ea;
  padding: 8px;
}

.hero {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.hero-copy,
.hero-panel,
.collection-card,
.hot-card,
.region-card {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(140, 110, 82, 0.12);
  border-radius: 28px;
  box-shadow: 0 20px 50px rgba(75, 55, 40, 0.08);
  backdrop-filter: blur(10px);
}

.hero-copy {
  padding: 36px;
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
.section-head h2 {
  margin: 0;
  color: #2f241f;
  font-weight: 700;
}

.hero-copy h1 {
  font-size: 42px;
  line-height: 1.08;
}

.hero-desc {
  margin: 18px 0 0;
  line-height: 1.8;
  color: #5c4b43;
  max-width: 720px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.hero-panel {
  padding: 24px;
  display: grid;
  gap: 16px;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.panel-card {
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
.panel-label {
  color: #725d53;
}

.panel-label {
  font-size: 12px;
}

.theme-strip {
  display: flex;
  gap: 14px;
  margin-bottom: 24px;
}

.theme-chip {
  border: 0;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 18px;
  padding: 14px 18px;
  min-width: 180px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 10px 28px rgba(75, 55, 40, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
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
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(196, 30, 58, 0.12);
  background: linear-gradient(135deg, rgba(196, 30, 58, 0.1), rgba(230, 162, 60, 0.08));
}

.content-grid {
  display: grid;
  grid-template-columns: 1.45fr 0.9fr;
  gap: 24px;
}

.collection-card,
.hot-card,
.region-card {
  padding: 6px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-head.compact {
  align-items: flex-end;
}

.theme-description {
  margin: 0 0 20px;
  color: #5f4e45;
  line-height: 1.7;
}

.collection-list {
  display: grid;
  gap: 16px;
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
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.collection-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(75, 55, 40, 0.08);
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
  color: #806b61;
  font-size: 13px;
}

.item-content h3 {
  margin: 12px 0 8px;
  color: #2f241f;
  font-size: 22px;
}

.item-content p {
  margin: 0 0 14px;
  color: #5d4f49;
  line-height: 1.7;
}

.side-column {
  display: grid;
  gap: 24px;
}

.hot-list,
.region-list {
  display: grid;
  gap: 12px;
}

.hot-item {
  width: 100%;
  border: 0;
  background: linear-gradient(180deg, #fff, #f9f3ee);
  border-radius: 16px;
  padding: 14px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  cursor: pointer;
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
.region-item strong {
  color: #2f241f;
}

.hot-copy span,
.region-item p {
  display: block;
  margin-top: 4px;
  color: #7c6860;
  font-size: 13px;
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
  .content-grid {
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

  .theme-strip {
    overflow-x: auto;
    padding-bottom: 6px;
  }

  .collection-item {
    grid-template-columns: 1fr;
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>