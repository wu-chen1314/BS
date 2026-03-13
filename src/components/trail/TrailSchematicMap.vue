<template>
  <div class="map-stage">
    <svg viewBox="0 0 100 100" class="map-svg" preserveAspectRatio="xMidYMid meet">
      <path
        class="map-outline"
        d="M10,22 C18,12 30,10 40,14 C48,8 58,8 66,14 C76,14 86,20 90,30 C95,40 94,50 88,58 C85,67 80,74 70,76 C62,83 52,85 42,82 C32,86 18,82 12,72 C6,64 6,54 10,46 C7,38 7,29 10,22 Z"
      />
      <path
        v-for="(segment, index) in mapData.segments"
        :key="`line-${index}`"
        class="route-line"
        :d="`M ${segment.fromX} ${segment.fromY} Q ${(segment.fromX + segment.toX) / 2} ${Math.min(segment.fromY, segment.toY) - 4} ${segment.toX} ${segment.toY}`"
      />
      <g v-for="(node, index) in mapData.nodes" :key="node.id">
        <circle class="route-node" :cx="node.x" :cy="node.y" r="2.6" />
        <circle class="route-glow" :cx="node.x" :cy="node.y" r="4.4" />
        <text class="route-order" :x="node.x" :y="node.y - 4.6">{{ index + 1 }}</text>
      </g>
    </svg>

    <div class="map-legend">
      <div v-for="(node, index) in mapData.nodes" :key="`legend-${node.id}`" class="legend-item">
        <span class="legend-index">{{ index + 1 }}</span>
        <div>
          <strong>{{ node.name }}</strong>
          <p>{{ node.regionName }} · {{ node.durationLabel }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { TrailStopView } from "@/types/trail";
import { buildTrailMap } from "@/utils/trail-map";

const props = defineProps<{
  stops: TrailStopView[];
}>();

const mapData = computed(() => buildTrailMap(props.stops));
</script>

<style scoped>
.map-stage{display:grid;grid-template-columns:minmax(0,1fr) 240px;gap:16px;align-items:start}.map-svg{width:100%;height:360px;border-radius:18px;background:linear-gradient(180deg,rgba(255,255,255,.48),rgba(255,250,241,.9))}.map-outline{fill:rgba(34,49,63,.04);stroke:rgba(192,138,63,.28);stroke-width:.7;stroke-dasharray:1.2 1.2}.route-line{fill:none;stroke:var(--heritage-primary);stroke-width:1.1;stroke-linecap:round;stroke-dasharray:2 1.2}.route-node{fill:var(--heritage-primary)}.route-glow{fill:none;stroke:rgba(192,138,63,.5);stroke-width:.7}.route-order{fill:var(--heritage-ink);font-size:3px;text-anchor:middle;font-weight:700}.map-legend{display:grid;gap:10px}.legend-item{display:grid;grid-template-columns:auto minmax(0,1fr);gap:10px;align-items:flex-start;padding:10px 12px;border-radius:16px;background:rgba(255,251,244,.9)}.legend-index{width:26px;height:26px;border-radius:50%;display:grid;place-items:center;background:var(--heritage-primary);color:var(--heritage-paper-soft);font-size:12px;font-weight:700}.legend-item strong{color:var(--heritage-ink)}.legend-item p{margin:4px 0 0;font-size:12px;color:var(--heritage-muted)}@media (max-width:960px){.map-stage{grid-template-columns:1fr}.map-svg{height:300px}}
</style>
