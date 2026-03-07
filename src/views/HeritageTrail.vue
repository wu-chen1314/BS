<template>
  <div class="trail-page">
    <section class="hero-section">
      <div class="hero-copy">
        <p class="hero-kicker">HERITAGE TRAIL CURATOR</p>
        <h1>非遗路线</h1>
        <p class="hero-desc">
          把项目、地区和热度数据重新组织成一条条可浏览、可讲述、可延展的文化路线。你可以从器物手艺、戏曲声腔、
          节俗生活三种主题切入，快速获得更有叙事性的非遗探索路径。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="goHome">进入项目展馆</el-button>
          <el-button size="large" round @click="goRegionCategory">查看地区分类</el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <article class="metric-card">
          <span>路线模板</span>
          <strong>{{ trailTemplates.length }}</strong>
          <p>围绕非遗传播主题生成</p>
        </article>
        <article class="metric-card">
          <span>候选项目</span>
          <strong>{{ projects.length }}</strong>
          <p>自动来自当前项目库</p>
        </article>
        <article class="metric-card">
          <span>热点地区</span>
          <strong>{{ topRegions.length }}</strong>
          <p>同步地图统计热区</p>
        </article>
      </div>
    </section>

    <section class="trail-switcher">
      <button
        v-for="trail in trailTemplates"
        :key="trail.id"
        type="button"
        class="trail-chip"
        :class="{ active: trail.id === selectedTrailId }"
        @click="selectedTrailId = trail.id"
      >
        <span>{{ trail.title }}</span>
        <small>{{ trail.subtitle }}</small>
      </button>
    </section>

    <section class="trail-grid">
      <el-card class="trail-main-card" shadow="never">
        <template #header>
          <div class="section-head">
            <div>
              <p class="section-kicker">ROUTE PROFILE</p>
              <h2>{{ activeTrail.title }}</h2>
            </div>
            <el-tag effect="dark" type="danger">{{ activeStops.length }} 个推荐站点</el-tag>
          </div>
        </template>

        <div class="profile-block">
          <div class="profile-copy">
            <p class="trail-summary">{{ activeTrail.description }}</p>
            <div class="profile-tags">
              <span class="profile-tag">推荐时长：{{ activeTrail.duration }}</span>
              <span class="profile-tag">最佳切入：{{ activeTrail.entry }}</span>
              <span class="profile-tag">路线关键词：{{ activeTrail.keywords.join(" / ") }}</span>
            </div>
          </div>
          <div class="profile-side">
            <div class="score-card">
              <span>路线热度</span>
              <strong>{{ averageHeat }}</strong>
              <p>基于当前路线项目的浏览量平均值</p>
            </div>
          </div>
        </div>

        <div class="stops-grid">
          <article
            v-for="(item, index) in activeStops"
            :key="item.id"
            class="stop-card"
            @click="goDetail(item.id)"
          >
            <div class="stop-cover">
              <img :src="buildStaticUrl(item.coverUrl) || fallbackCover" :alt="item.name" />
              <span class="stop-order">第 {{ index + 1 }} 站</span>
            </div>
            <div class="stop-body">
              <div class="stop-meta">
                <span>{{ item.regionName || "地区待补充" }}</span>
                <span>{{ getCategoryName(Number(item.categoryId)) }}</span>
              </div>
              <h3>{{ item.name }}</h3>
              <p>{{ summarize(item.history) }}</p>
              <div class="stop-footer">
                <strong>{{ item.inheritorNames || "传承人信息待补充" }}</strong>
                <span>{{ item.viewCount || 0 }} 次浏览</span>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-if="!activeStops.length" description="当前路线还没有可展示的项目" :image-size="120" />
      </el-card>

      <div class="trail-side-column">
        <el-card class="note-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">TRAVEL NOTES</p>
                <h2>行前建议</h2>
              </div>
            </div>
          </template>

          <div class="note-list">
            <div v-for="item in activeTrail.notes" :key="item.title" class="note-item">
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="region-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">HOT REGIONS</p>
                <h2>热点地区</h2>
              </div>
            </div>
          </template>

          <div class="region-list">
            <button
              v-for="item in topRegions"
              :key="item.name"
              type="button"
              class="region-item"
              @click="goRegionCategory"
            >
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.value }} 个项目进入统计热区</p>
              </div>
              <span class="region-value">{{ item.value }}</span>
            </button>
          </div>
        </el-card>

        <el-card class="hot-card" shadow="never">
          <template #header>
            <div class="section-head compact">
              <div>
                <p class="section-kicker">HOT PICKS</p>
                <h2>热门补充阅读</h2>
              </div>
            </div>
          </template>

          <div class="hot-list">
            <button
              v-for="item in hotProjects.slice(0, 5)"
              :key="item.id"
              type="button"
              class="hot-item"
              @click="goDetail(item.id)"
            >
              <strong>{{ item.name }}</strong>
              <span>{{ item.viewCount || 0 }} 次浏览</span>
            </button>
          </div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request";
import { buildStaticUrl } from "@/utils/url";

interface TrailNote {
  title: string;
  content: string;
}

interface TrailTemplate {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  duration: string;
  entry: string;
  keywords: string[];
  categoryIds: number[];
  notes: TrailNote[];
}

const router = useRouter();
const fallbackCover =
  "https://images.unsplash.com/photo-1518998053901-5348d3961a04?q=80&w=1200&auto=format&fit=crop";

const trailTemplates: TrailTemplate[] = [
  {
    id: "craft",
    title: "匠作巡礼",
    subtitle: "器物、工坊与手艺肌理",
    description:
      "从传统美术、传统技艺到传统医药，适合用“看工艺细节”的方式进入非遗内容，强调器物、材料与技法的连续性。",
    duration: "半日到一日",
    entry: "先看工艺，再补人物",
    keywords: ["器物", "技法", "手工", "材料"],
    categoryIds: [7, 8, 9],
    notes: [
      { title: "适合人群", content: "适合做专题导览、课程案例和视觉内容策划。" },
      { title: "建议顺序", content: "先浏览封面和简介，再进入项目详情补充工艺脉络。" },
      { title: "推荐搭配", content: "可与地区分类联动，形成区域工艺路线。" },
    ],
  },
  {
    id: "performance",
    title: "声腔剧场",
    subtitle: "戏曲、音乐与曲艺现场感",
    description:
      "聚焦传统音乐、传统戏剧与曲艺内容，用更有表演张力的方式串联项目，适合快速建立听觉与舞台感知。",
    duration: "一小时到半日",
    entry: "先看热度，再看代表项目",
    keywords: ["声腔", "舞台", "表演", "观看体验"],
    categoryIds: [2, 4, 5],
    notes: [
      { title: "适合人群", content: "适合做活动策划、节庆专题和大众传播内容。" },
      { title: "建议顺序", content: "先看热门项目，再按类别延伸到相近表演形态。" },
      { title: "推荐搭配", content: "适合与热度排行联动使用，快速找到爆点内容。" },
    ],
  },
  {
    id: "folk",
    title: "节俗人间",
    subtitle: "民间文学、民俗与生活现场",
    description:
      "把民间文学、民俗、传统体育游艺等内容编织成生活化路线，更适合从节日、习俗与地方记忆进入项目。",
    duration: "半日",
    entry: "先看地区，再看生活场景",
    keywords: ["节俗", "地方记忆", "生活方式", "民间叙事"],
    categoryIds: [1, 3, 6, 10],
    notes: [
      { title: "适合人群", content: "适合校园传播、城市品牌叙事和节庆专题整合。" },
      { title: "建议顺序", content: "优先查看地区热区，再按生活场景挑选代表项目。" },
      { title: "推荐搭配", content: "可与地图统计联动，形成“从地区到习俗”的传播路径。" },
    ],
  },
];

const selectedTrailId = ref(trailTemplates[0].id);
const projects = ref<any[]>([]);
const hotProjects = ref<any[]>([]);
const regionStats = ref<any[]>([]);

const activeTrail = computed(
  () => trailTemplates.find((item) => item.id === selectedTrailId.value) || trailTemplates[0]
);

const activeStops = computed(() => {
  const matched = projects.value.filter((item) =>
    activeTrail.value.categoryIds.includes(Number(item.categoryId))
  );
  const base = matched.length ? matched : projects.value;
  return base.slice(0, 6);
});

const topRegions = computed(() =>
  [...regionStats.value].sort((a: any, b: any) => Number(b.value || 0) - Number(a.value || 0)).slice(0, 5)
);

const averageHeat = computed(() => {
  if (!activeStops.value.length) {
    return 0;
  }
  const total = activeStops.value.reduce((sum, item) => sum + Number(item.viewCount || 0), 0);
  return Math.round(total / activeStops.value.length);
});

const fetchProjects = async () => {
  const res = await request.get("/projects/page", {
    params: { pageNum: 1, pageSize: 60 },
  });
  if (res.data.code === 200) {
    projects.value = res.data.data?.records || [];
  }
};

const fetchHotProjects = async () => {
  const res = await request.get("/view/hot", { params: { limit: 8 } });
  if (res.data.code === 200) {
    hotProjects.value = res.data.data || [];
  }
};

const fetchMapStats = async () => {
  const res = await request.get("/statistics/map");
  if (res.data.code === 200) {
    regionStats.value = res.data.data || [];
  }
};

const goDetail = (id: number) => {
  router.push({ path: "/home", query: { id } });
};

const goHome = () => {
  router.push("/home");
};

const goRegionCategory = () => {
  router.push("/region-category");
};

const summarize = (html?: string) => {
  if (!html) {
    return "暂无补充简介，可进入详情页查看完整内容。";
  }

  const text = html.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
  return text.length > 88 ? `${text.slice(0, 88)}...` : text;
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

onMounted(async () => {
  await Promise.all([fetchProjects(), fetchHotProjects(), fetchMapStats()]);
});
</script>

<style scoped>
.trail-page {
  min-height: calc(100vh - 110px);
  padding: 12px 4px 32px;
  color: #1f2c3d;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 20px;
  margin-bottom: 22px;
}

.hero-copy,
.hero-metrics,
.trail-main-card,
.note-card,
.region-card,
.hot-card {
  border-radius: 24px;
  border: 1px solid rgba(196, 30, 58, 0.08);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 42px rgba(34, 45, 66, 0.08);
}

.hero-copy {
  padding: 32px;
  background:
    radial-gradient(circle at top right, rgba(212, 175, 55, 0.14), transparent 34%),
    linear-gradient(135deg, rgba(255, 248, 238, 0.96), rgba(255, 255, 255, 0.98));
}

.hero-kicker,
.section-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: #a66a3f;
}

.hero-copy h1 {
  margin: 0 0 12px;
  font-size: 40px;
  line-height: 1.08;
}

.hero-desc,
.trail-summary {
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
  color: #5c6a7d;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-metrics {
  padding: 18px;
  display: grid;
  gap: 14px;
}

.metric-card,
.score-card {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fffaf1, #fff);
  border: 1px solid rgba(166, 106, 63, 0.1);
}

.metric-card span,
.score-card span {
  display: block;
  font-size: 13px;
  color: #8b6d59;
}

.metric-card strong,
.score-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 34px;
  color: #b4362f;
}

.metric-card p,
.score-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.trail-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.trail-chip {
  border: none;
  cursor: pointer;
  min-width: 210px;
  padding: 16px 18px;
  border-radius: 18px;
  text-align: left;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 28px rgba(34, 45, 66, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.trail-chip span {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #243246;
}

.trail-chip small {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #8a96a3;
}

.trail-chip.active,
.trail-chip:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #c41e3a, #d8863d);
  box-shadow: 0 18px 34px rgba(196, 30, 58, 0.2);
}

.trail-chip.active span,
.trail-chip.active small,
.trail-chip:hover span,
.trail-chip:hover small {
  color: #fffaf1;
}

.trail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.trail-main-card :deep(.el-card__body),
.note-card :deep(.el-card__body),
.region-card :deep(.el-card__body),
.hot-card :deep(.el-card__body) {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.section-head h2 {
  margin: 0;
  font-size: 26px;
}

.section-head.compact h2 {
  font-size: 22px;
}

.profile-block {
  margin-top: 6px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
}

.profile-tags {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-tag {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(196, 30, 58, 0.08);
  color: #8d3d34;
  font-size: 13px;
}

.stops-grid {
  margin-top: 22px;
  display: grid;
  gap: 16px;
}

.stop-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  border: none;
  cursor: pointer;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(255, 249, 242, 0.9), #fff);
  box-shadow: 0 12px 28px rgba(34, 45, 66, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stop-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(34, 45, 66, 0.1);
}

.stop-cover {
  position: relative;
  min-height: 180px;
}

.stop-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.stop-order {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(31, 44, 61, 0.75);
  color: #fff;
  font-size: 12px;
}

.stop-body {
  padding: 18px 18px 18px 0;
}

.stop-meta,
.stop-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #8a96a3;
}

.stop-body h3 {
  margin: 10px 0 12px;
  font-size: 22px;
}

.stop-body p {
  margin: 0 0 14px;
  color: #5c6a7d;
  line-height: 1.8;
}

.stop-footer strong {
  color: #243246;
}

.trail-side-column {
  display: grid;
  gap: 18px;
  align-content: start;
}

.note-list,
.region-list,
.hot-list {
  display: grid;
  gap: 12px;
}

.note-item,
.hot-item,
.region-item {
  border-radius: 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(250, 243, 232, 0.75), #fff);
  border: 1px solid rgba(166, 106, 63, 0.08);
}

.note-item strong,
.region-item strong,
.hot-item strong {
  color: #243246;
}

.note-item p,
.region-item p {
  margin: 8px 0 0;
  line-height: 1.7;
  color: #64748b;
}

.region-item,
.hot-item {
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.region-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.region-value {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(196, 30, 58, 0.12);
  color: #b4362f;
  font-weight: 700;
}

.hot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.hot-item span {
  color: #8a96a3;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .hero-section,
  .trail-grid,
  .profile-block,
  .stop-card {
    grid-template-columns: 1fr;
  }

  .stop-body {
    padding: 0 18px 18px;
  }
}

@media (max-width: 768px) {
  .trail-page {
    padding: 8px 0 18px;
  }

  .hero-copy,
  .trail-main-card :deep(.el-card__body),
  .note-card :deep(.el-card__body),
  .region-card :deep(.el-card__body),
  .hot-card :deep(.el-card__body) {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .trail-chip {
    min-width: 100%;
  }
}
</style>
