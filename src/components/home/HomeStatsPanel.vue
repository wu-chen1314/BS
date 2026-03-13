<template>
  <section class="visual-grid">
    <article class="visual-card level-card heritage-float-card">
      <div class="panel-header">
        <div>
          <span class="panel-kicker">保护级别</span>
          <h3>项目分布图</h3>
        </div>
        <div class="meta-chips">
          <span class="meta-chip">当前页 {{ currentCount }}</span>
          <span class="meta-chip">累计 {{ totalCount }}</span>
        </div>
      </div>

      <div class="level-body">
        <div class="ring-chart" :style="{ background: levelRingBackground }">
          <div class="ring-core">
            <strong>{{ totalCount }}</strong>
            <span>累计项目</span>
          </div>
        </div>

        <div class="legend-list">
          <div v-for="item in levelLegend" :key="item.name" class="legend-item">
            <div class="legend-top">
              <span class="legend-mark" :style="{ background: item.color }"></span>
              <strong>{{ item.name }}</strong>
            </div>
            <div class="legend-bottom">
              <span>{{ item.value }} 项</span>
              <span>{{ item.percentLabel }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="status-strip">
        <span v-for="item in statusStats" :key="item.name" class="status-pill">
          {{ item.name }} {{ item.value }}
        </span>
      </div>
    </article>

    <article class="visual-card heat-card heritage-float-card">
      <div class="panel-header">
        <div>
          <span class="panel-kicker">实时热度</span>
          <h3>热度排行图</h3>
        </div>
        <div class="meta-chips">
          <span class="meta-chip">平均热度 {{ averageViews }}</span>
        </div>
      </div>

      <div class="heat-list">
        <button
          v-for="(item, index) in heatItems"
          :key="item.id"
          type="button"
          class="heat-item"
          @click="$emit('open-project', item.id)"
        >
          <div class="heat-title">
            <span class="heat-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
            <strong>{{ item.name }}</strong>
            <span class="heat-value">{{ item.viewCount || 0 }} 次</span>
          </div>
          <div class="heat-track">
            <div class="heat-fill" :style="{ width: calcWidth(Number(item.viewCount || 0), hotMax) }"></div>
          </div>
        </button>
          <el-empty v-if="heatItems.length === 0" description="当前还没有可展示的热度排行" :image-size="72" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { HotProjectItem, StatisticItem } from "@/types/project";

const LEVEL_COLORS = ["#c0392b", "#d4af37", "#52be80", "#5b8def", "#7f8c8d"];

const props = defineProps<{
  currentCount: number;
  totalCount: number;
  averageViews: number;
  levelStats: StatisticItem[];
  statusStats: StatisticItem[];
  hotProjects: HotProjectItem[];
}>();

defineEmits<{ (e: "open-project", id: number): void }>();

const hotMax = computed(() => Math.max(...props.hotProjects.map((item) => Number(item.viewCount || 0)), 1));
const heatItems = computed(() => props.hotProjects.slice(0, 5));
const totalLevelValue = computed(() =>
  Math.max(
    props.levelStats.reduce((sum, item) => sum + Number(item.value || 0), 0),
    1
  )
);

const levelLegend = computed(() =>
  props.levelStats.map((item, index) => {
    const value = Number(item.value || 0);
    const percent = value / totalLevelValue.value;
    return {
      color: LEVEL_COLORS[index % LEVEL_COLORS.length],
      name: item.name,
      percent,
      percentLabel: `${Math.round(percent * 100)}%`,
      value,
    };
  })
);

const levelRingBackground = computed(() => {
  if (levelLegend.value.length === 0) {
    return "conic-gradient(rgba(28, 40, 51, 0.08) 0deg 360deg)";
  }

  let currentAngle = 0;
  const segments = levelLegend.value.map((item) => {
    const start = currentAngle;
    const end = currentAngle + item.percent * 360;
    currentAngle = end;
    return `${item.color} ${start}deg ${end}deg`;
  });

  return `conic-gradient(${segments.join(", ")})`;
});

const calcWidth = (value: number, max: number) => `${Math.max((Number(value || 0) / max) * 100, 10)}%`;
</script>

<style scoped>
.visual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.visual-card {
  border-radius: 24px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
  box-shadow: var(--heritage-card-shadow-rest);
}

.level-card {
  background:
    radial-gradient(circle at top right, rgba(212, 175, 55, 0.18), transparent 30%),
    rgba(255, 255, 255, 0.94);
}

.heat-card {
  background:
    radial-gradient(circle at top left, rgba(192, 57, 43, 0.12), transparent 28%),
    rgba(255, 255, 255, 0.94);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-header h3 {
  margin: 4px 0 0;
  font-size: 22px;
  color: var(--heritage-ink);
}

.panel-kicker {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--heritage-primary);
}

.meta-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.meta-chip,
.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(192, 57, 43, 0.08);
  color: var(--heritage-primary);
  font-size: 13px;
}

.level-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  margin-top: 20px;
}

.ring-chart {
  width: 220px;
  height: 220px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  box-shadow: inset 0 0 0 1px rgba(28, 40, 51, 0.06);
}

.ring-core {
  width: 136px;
  height: 136px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--heritage-ink);
  box-shadow: 0 16px 32px rgba(28, 40, 51, 0.08);
}

.ring-core strong {
  font-size: 34px;
  line-height: 1;
}

.ring-core span {
  font-size: 13px;
  color: var(--heritage-ink-soft);
}

.legend-list,
.heat-list {
  display: grid;
  gap: 12px;
}

.legend-item,
.heat-item {
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.legend-top,
.legend-bottom,
.heat-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.legend-top strong,
.heat-title strong {
  color: var(--heritage-ink);
  flex: 1;
}

.legend-mark {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-bottom {
  margin-top: 8px;
  color: var(--heritage-ink-soft);
  font-size: 13px;
}

.status-strip {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.heat-item {
  border: none;
  cursor: pointer;
  text-align: left;
}

.heat-rank {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(192, 57, 43, 0.12);
  color: var(--heritage-primary);
  font-weight: 700;
}

.rank-1,
.rank-2,
.rank-3 {
  color: #fff;
}

.rank-1 {
  background: var(--heritage-primary);
}

.rank-2 {
  background: var(--heritage-gold);
}

.rank-3 {
  background: var(--heritage-jade);
}

.heat-value {
  color: var(--heritage-primary);
  font-size: 13px;
  white-space: nowrap;
}

.heat-track {
  height: 10px;
  margin-top: 12px;
  border-radius: 999px;
  background: rgba(28, 40, 51, 0.08);
  overflow: hidden;
}

.heat-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--heritage-primary), var(--heritage-gold));
}

@media (max-width: 1100px) {
  .visual-grid,
  .level-body {
    grid-template-columns: 1fr;
  }

  .ring-chart {
    margin: 0 auto;
  }
}

@media (max-width: 768px) {
  .visual-grid {
    grid-template-columns: 1fr;
  }

  .visual-card {
    padding: 18px;
  }

  .panel-header,
  .heat-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-chips {
    justify-content: flex-start;
  }

  .ring-chart {
    width: 190px;
    height: 190px;
  }

  .ring-core {
    width: 116px;
    height: 116px;
  }
}
</style>
