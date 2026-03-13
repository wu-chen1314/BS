<template>
  <div class="map-card heritage-float-card">
    <div class="map-head">
      <div>
        <p class="kicker">AMAP NAVIGATION</p>
        <h3>路线地图与导航指引</h3>
        <p class="sub-copy">{{ mapModeDescription }}</p>
      </div>
      <div class="head-actions">
        <el-tag :type="renderMode === 'amap' ? 'success' : 'warning'" effect="dark" round>
          {{ renderMode === "amap" ? "高德地图模式" : fallbackModeLabel }}
        </el-tag>
        <el-button plain size="small" :loading="planning" @click="reloadRoute">重新规划</el-button>
        <el-button plain size="small" :disabled="renderMode !== 'amap'" @click="resetMapView">重置视角</el-button>
        <el-button size="small" type="primary" :disabled="!canStartLiveNavigation" @click="toggleLiveNavigation">
          {{ liveNavigationEnabled ? "停止实时导航" : "开启实时导航" }}
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="noticeMessage"
      class="map-alert"
      :title="noticeMessage"
      :type="renderMode === 'amap' ? 'info' : 'warning'"
      :closable="false"
      show-icon
    />

    <div class="summary-grid">
      <article class="summary-card heritage-float-card">
        <span>交通方式</span>
        <strong>{{ transportLabel }}</strong>
        <p>{{ navigationRoute?.source === "approximate" ? "当前由估算导航提供路线概览" : "已按当前偏好规划导航服务" }}</p>
      </article>
      <article class="summary-card heritage-float-card">
        <span>预计路程</span>
        <strong>{{ overviewDistance }}</strong>
        <p>{{ liveNavigationEnabled ? "实时剩余里程" : "全程预估路程" }}</p>
      </article>
      <article class="summary-card heritage-float-card">
        <span>预计时长</span>
        <strong>{{ overviewDuration }}</strong>
        <p>{{ liveNavigationEnabled ? "动态剩余时长" : "全程预估时长" }}</p>
      </article>
      <article class="summary-card heritage-float-card">
        <span>预计到达</span>
        <strong>{{ overviewEta }}</strong>
        <p>{{ liveNavigationEnabled ? "根据当前位置动态计算" : "按当前路线估算" }}</p>
      </article>
    </div>

    <div class="map-stage">
      <div class="map-pane">
        <div v-show="renderMode === 'amap'" ref="mapContainer" class="map-canvas" />
        <TrailSchematicMap v-if="renderMode === 'fallback'" :stops="stops" />
        <div v-if="renderMode === 'amap' && planning" class="map-loading">正在加载地图与路线...</div>
      </div>

      <aside class="map-side">
        <section class="side-section heritage-float-card">
          <div class="section-title">
            <strong>导航概览</strong>
            <span>{{ liveNavigationEnabled ? "实时模式" : "规划模式" }}</span>
          </div>
          <p class="side-copy">{{ navigationSummary }}</p>
          <div class="checkpoint-list">
            <div
              v-for="(stop, index) in stops"
              :key="`${stop.project.id || stop.project.name}-${index}`"
              class="checkpoint-item"
            >
              <span class="checkpoint-index">{{ index + 1 }}</span>
              <div>
                <strong>{{ stop.project.name }}</strong>
                <p>{{ stop.project.regionName || "地区待补充" }} · {{ stop.visitDurationLabel }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="side-section heritage-float-card">
          <div class="section-title">
            <strong>{{ liveNavigationEnabled ? "实时导航指引" : "路线规划步骤" }}</strong>
            <span>{{ instructionCountLabel }}</span>
          </div>
        <el-empty v-if="instructionList.length === 0" description="当前路线还没有可展示的导航步骤" :image-size="84" />
          <ol v-else class="instruction-list">
            <li v-for="item in instructionList" :key="item.id" class="instruction-item">
              <strong>{{ item.text }}</strong>
              <p>{{ item.distanceText }} · {{ item.durationText }}</p>
            </li>
          </ol>
        </section>

        <section class="side-section heritage-float-card">
          <div class="section-title">
            <strong>定位与加载状态</strong>
            <span>{{ canUseGeolocation ? "浏览器支持定位" : "当前浏览器不支持定位" }}</span>
          </div>
          <p class="side-copy">{{ locationStatus }}</p>
          <div class="status-grid">
            <div class="status-item">
              <span>SDK 配置</span>
              <strong>{{ sdkStatusLabel }}</strong>
            </div>
            <div class="status-item">
              <span>坐标完整度</span>
              <strong>{{ hasRealCoordinates ? "完整" : "不足" }}</strong>
            </div>
            <div class="status-item">
              <span>实时导航</span>
              <strong>{{ liveNavigationEnabled ? "进行中" : "未开启" }}</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from "vue";
import TrailSchematicMap from "@/components/trail/TrailSchematicMap.vue";
import {
  planApproximateLiveLegNavigation,
  planApproximateTrailNavigation,
  planLiveLegNavigation,
  planTrailNavigation,
} from "@/services/amap-navigation";
import type { AmapMapInstance, AmapMarkerInstance, AmapPolylineInstance, AmapStaticApi } from "@/types/amap";
import type {
  TrailLiveGuidance,
  TrailMapCoordinate,
  TrailNavigationInstruction,
  TrailNavigationRoute,
  TrailStopView,
  TransportMode,
} from "@/types/trail";
import { getAmapAvailability, loadAmapSdk, resolveAmapErrorMessage } from "@/utils/amap";
import {
  buildLiveGuidance,
  formatDistance,
  formatDuration,
  formatEta,
  hasNavigableCoordinates,
  haversineDistance,
  resolveNextStopIndex,
} from "@/utils/trail-navigation";

const props = defineProps<{
  stops: TrailStopView[];
  transportMode: TransportMode;
}>();

const mapContainer = ref<HTMLDivElement | null>(null);
const planning = ref(false);
const renderMode = ref<"amap" | "fallback">("fallback");
const mapError = ref("");
const locationStatus = ref("导航未启动");
const liveNavigationEnabled = ref(false);
const currentStopIndex = ref(0);
const navigationRoute = ref<TrailNavigationRoute | null>(null);
const liveGuidance = ref<TrailLiveGuidance | null>(null);
const currentPosition = ref<TrailMapCoordinate | null>(null);

const mapInstance = shallowRef<AmapMapInstance | null>(null);
const amapModule = shallowRef<AmapStaticApi | null>(null);
const routePolyline = shallowRef<AmapPolylineInstance | null>(null);
const currentMarker = shallowRef<AmapMarkerInstance | null>(null);
const stopMarkers = shallowRef<AmapMarkerInstance[]>([]);
const controlsAdded = ref(false);

let geolocationWatchId: number | null = null;
let lastLiveRefreshAt = 0;
let lastLivePosition: TrailMapCoordinate | null = null;

const amapAvailability = computed(() => getAmapAvailability());
const hasRealCoordinates = computed(() => hasNavigableCoordinates(props.stops));
const canRenderAmap = computed(
  () => amapAvailability.value.available && hasRealCoordinates.value && props.stops.length >= 2
);
const canUseGeolocation = computed(() => typeof navigator !== "undefined" && "geolocation" in navigator);
const canStartLiveNavigation = computed(
  () => canUseGeolocation.value && hasRealCoordinates.value && props.stops.length >= 2 && Boolean(navigationRoute.value)
);

const transportLabel = computed(() => {
  switch (props.transportMode) {
    case "walk":
      return "步行";
    case "car":
      return "驾车";
    case "public":
      return "公共交通";
    default:
      return "骑行";
  }
});

const overviewDistance = computed(() => liveGuidance.value?.distanceText || navigationRoute.value?.distanceText || "待规划");
const overviewDuration = computed(() => liveGuidance.value?.durationText || navigationRoute.value?.durationText || "待规划");
const overviewEta = computed(() => liveGuidance.value?.etaText || navigationRoute.value?.etaText || "--:--");

const mapModeDescription = computed(() => {
  if (renderMode.value === "amap") {
    return "支持缩放、平移、真实路径规划和实时位置更新。";
  }
  if (navigationRoute.value?.source === "approximate") {
    return "高德不可用时会自动切换为估算导航，继续提供里程、时长和步骤概览。";
  }
  return "当前以示意图模式展示路线顺序，等待补齐坐标后再切换到真实地图。";
});

const fallbackModeLabel = computed(() =>
  navigationRoute.value?.source === "approximate" ? "估算导航模式" : "示意图降级模式"
);

const sdkStatusLabel = computed(() => {
  if (renderMode.value === "fallback" && navigationRoute.value?.source === "approximate" && mapError.value) {
    return "已降级";
  }
  if (amapAvailability.value.available) {
    return "已就绪";
  }
  if (amapAvailability.value.status === "missing-key") {
    return "缺少 Key";
  }
  return "未就绪";
});

const instructionList = computed<TrailNavigationInstruction[]>(() => {
  if (liveGuidance.value?.instructions?.length) {
    return liveGuidance.value.instructions;
  }
  return navigationRoute.value?.legs.flatMap((item) => item.instructions) || [];
});

const instructionCountLabel = computed(() =>
  instructionList.value.length ? `${instructionList.value.length} 条指引` : "暂无"
);

const navigationSummary = computed(() => {
  if (liveGuidance.value) {
    return `当前前往 ${liveGuidance.value.destinationName}，预计剩余 ${liveGuidance.value.durationText}，约 ${liveGuidance.value.etaText} 抵达。`;
  }
  if (navigationRoute.value) {
    const providerLabel = navigationRoute.value.source === "amap" ? "高德" : "估算";
    return `${providerLabel}已生成 ${navigationRoute.value.legs.length} 段路线，预计总路程 ${navigationRoute.value.distanceText}，总时长 ${navigationRoute.value.durationText}。`;
  }
  return "路线规划完成后，这里会展示预计时长、到达时间与步骤指引。";
});

const noticeMessage = computed(() => {
  if (!hasRealCoordinates.value) {
    return "当前路线缺少完整经纬度，已自动切换为示意图模式。补齐坐标后会自动启用真实地图。";
  }
  if (!amapAvailability.value.available) {
    return `${amapAvailability.value.message}，当前已切换为估算导航模式。`;
  }
  return mapError.value || "";
});

const routeWatchKey = computed(() =>
  [
    props.transportMode,
    ...props.stops.map((stop) =>
      [Number(stop.project.id || 0), Number(stop.project.longitude || 0).toFixed(6), Number(stop.project.latitude || 0).toFixed(6)].join(":")
    ),
  ].join("|")
);

const getBaselineLegIndex = () => {
  if (!navigationRoute.value?.legs.length) {
    return 0;
  }
  return Math.max(0, Math.min(currentStopIndex.value > 0 ? currentStopIndex.value - 1 : 0, navigationRoute.value.legs.length - 1));
};

const clearRouteOverlays = () => {
  if (!mapInstance.value) {
    return;
  }

  if (routePolyline.value) {
    mapInstance.value.remove(routePolyline.value);
    routePolyline.value = null;
  }
  if (stopMarkers.value.length) {
    mapInstance.value.remove(stopMarkers.value);
    stopMarkers.value = [];
  }
  if (currentMarker.value) {
    mapInstance.value.remove(currentMarker.value);
    currentMarker.value = null;
  }
};

const applyFallbackRoute = (message: string) => {
  clearRouteOverlays();
  renderMode.value = "fallback";

  if (!hasRealCoordinates.value || props.stops.length < 2) {
    navigationRoute.value = null;
    liveGuidance.value = null;
    locationStatus.value = message;
    return;
  }

  navigationRoute.value = planApproximateTrailNavigation(props.stops, props.transportMode);
  liveGuidance.value = buildLiveGuidance(navigationRoute.value, getBaselineLegIndex());
  locationStatus.value = message;
};

const ensureMap = async () => {
  if (!mapContainer.value) {
    return null;
  }

  if (!amapModule.value) {
    amapModule.value = await loadAmapSdk(["AMap.ToolBar", "AMap.Scale"]);
  }

  const AMap = amapModule.value;
  if (!mapInstance.value) {
    mapInstance.value = new AMap.Map(mapContainer.value, {
      resizeEnable: true,
      zoom: 5,
      zooms: [3, 19],
      mapStyle: "amap://styles/whitesmoke",
      center: [105.0, 36.0],
    });
  }

  if (!controlsAdded.value) {
    mapInstance.value.addControl(new AMap.ToolBar());
    mapInstance.value.addControl(new AMap.Scale());
    controlsAdded.value = true;
  }

  return AMap;
};

const renderCurrentLocation = () => {
  if (!mapInstance.value || !amapModule.value || !currentPosition.value || renderMode.value !== "amap") {
    return;
  }

  const AMap = amapModule.value;
  if (!currentMarker.value) {
    currentMarker.value = new AMap.Marker({
      position: [currentPosition.value.longitude, currentPosition.value.latitude],
      anchor: "center",
      content: '<div class="trail-current-marker"></div>',
      zIndex: 140,
    });
    mapInstance.value.add(currentMarker.value);
    return;
  }

  currentMarker.value.setPosition([currentPosition.value.longitude, currentPosition.value.latitude]);
};

const renderPlannedRoute = async () => {
  if (renderMode.value !== "amap" || !navigationRoute.value || navigationRoute.value.source !== "amap") {
    return;
  }

  const AMap = await ensureMap();
  if (!AMap || !mapInstance.value) {
    return;
  }

  clearRouteOverlays();

  stopMarkers.value = props.stops.map((stop, index) => {
    const marker = new AMap.Marker({
      position: [Number(stop.project.longitude), Number(stop.project.latitude)],
      anchor: "bottom-center",
      content: `<div class="trail-stop-marker"><span>${index + 1}</span></div>`,
      title: stop.project.name,
    });
    marker.setLabel({
      direction: "top",
      offset: new AMap.Pixel(0, -10),
      content: `<div class="trail-stop-label">${stop.project.name}</div>`,
    });
    return marker;
  });

  routePolyline.value = new AMap.Polyline({
    path: navigationRoute.value.polyline,
    strokeColor: "#A43B2F",
    strokeWeight: 6,
    strokeOpacity: 0.88,
    showDir: true,
    lineJoin: "round",
  });

  mapInstance.value.add([...stopMarkers.value, routePolyline.value]);
  renderCurrentLocation();
  resetMapView();
};

const resetMapView = () => {
  if (!mapInstance.value) {
    return;
  }
  const overlays = [...stopMarkers.value, routePolyline.value, currentMarker.value].filter(Boolean);
  if (overlays.length) {
    mapInstance.value.setFitView(overlays, false, [60, 60, 60, 60]);
  }
};

const updateLiveGuidance = async () => {
  if (!liveNavigationEnabled.value || !currentPosition.value || !navigationRoute.value) {
    return;
  }

  const nextStopIndex = resolveNextStopIndex(props.stops, currentPosition.value, currentStopIndex.value);
  currentStopIndex.value = nextStopIndex;

  const now = Date.now();
  const movedEnough = !lastLivePosition || haversineDistance(lastLivePosition, currentPosition.value) >= 80;
  if (!movedEnough && now - lastLiveRefreshAt < 12000) {
    return;
  }

  lastLiveRefreshAt = now;
  lastLivePosition = currentPosition.value;

  try {
    const liveLeg =
      renderMode.value === "amap"
        ? await planLiveLegNavigation({
            origin: currentPosition.value,
            destination: props.stops[nextStopIndex],
            transportMode: props.transportMode,
            fromName: "当前位置",
          })
        : planApproximateLiveLegNavigation({
            origin: currentPosition.value,
            destination: props.stops[nextStopIndex],
            transportMode: props.transportMode,
            fromName: "当前位置",
          });

    const trailingLegs =
      nextStopIndex === 0 ? navigationRoute.value.legs : navigationRoute.value.legs.slice(nextStopIndex);
    const remainingDistance = liveLeg.distance + trailingLegs.reduce((sum, item) => sum + Number(item.distance || 0), 0);
    const remainingDuration = liveLeg.duration + trailingLegs.reduce((sum, item) => sum + Number(item.duration || 0), 0);

    liveGuidance.value = {
      activeLegIndex: Math.max(0, Math.min(nextStopIndex - 1, navigationRoute.value.legs.length - 1)),
      destinationName: props.stops[nextStopIndex].project.name,
      distance: remainingDistance,
      duration: remainingDuration,
      distanceText: formatDistance(remainingDistance),
      durationText: formatDuration(remainingDuration),
      etaText: formatEta(remainingDuration),
      instructions: liveLeg.instructions,
    };
    locationStatus.value =
      renderMode.value === "amap"
        ? `正在导航至 ${props.stops[nextStopIndex].project.name}`
        : `正在按估算路线前往 ${props.stops[nextStopIndex].project.name}`;
    renderCurrentLocation();
  } catch (error) {
    console.error("Failed to update live navigation", error);
    locationStatus.value = "实时导航更新失败，已保留最近一次路线信息";
  }
};

const handleGeolocationError = (error?: GeolocationPositionError) => {
  console.error("Failed to watch position", error);
  if (geolocationWatchId !== null && typeof navigator !== "undefined" && "geolocation" in navigator) {
    navigator.geolocation.clearWatch(geolocationWatchId);
  }
  geolocationWatchId = null;
  liveNavigationEnabled.value = false;
  locationStatus.value = "定位失败，请检查浏览器定位权限";
};

const startLiveNavigation = () => {
  if (!canStartLiveNavigation.value || liveNavigationEnabled.value) {
    return;
  }

  liveNavigationEnabled.value = true;
  locationStatus.value = "正在请求定位权限...";
  geolocationWatchId = navigator.geolocation.watchPosition(
    async (position) => {
      currentPosition.value = {
        longitude: position.coords.longitude,
        latitude: position.coords.latitude,
      };
      locationStatus.value = "已获取当前位置，正在刷新导航...";
      renderCurrentLocation();
      await updateLiveGuidance();
    },
    (error) => {
      handleGeolocationError(error);
    },
    {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 10000,
    }
  );
};

const stopLiveNavigation = () => {
  if (geolocationWatchId !== null && typeof navigator !== "undefined" && "geolocation" in navigator) {
    navigator.geolocation.clearWatch(geolocationWatchId);
    geolocationWatchId = null;
  }
  liveNavigationEnabled.value = false;
  liveGuidance.value = navigationRoute.value ? buildLiveGuidance(navigationRoute.value, getBaselineLegIndex()) : null;
  locationStatus.value = "实时导航已停止";
};

const toggleLiveNavigation = () => {
  if (liveNavigationEnabled.value) {
    stopLiveNavigation();
    return;
  }
  startLiveNavigation();
};

const loadRoute = async () => {
  planning.value = true;
  mapError.value = "";
  liveGuidance.value = null;
  navigationRoute.value = null;

  try {
    if (!hasRealCoordinates.value || props.stops.length < 2) {
      applyFallbackRoute("当前路线缺少完整坐标，已切换为示意图模式");
      return;
    }

    if (!canRenderAmap.value) {
      applyFallbackRoute("高德地图未就绪，已切换为估算导航");
      return;
    }

    navigationRoute.value = await planTrailNavigation(props.stops, props.transportMode);
    renderMode.value = "amap";
    liveGuidance.value = buildLiveGuidance(navigationRoute.value, 0);
    await nextTick();
    await renderPlannedRoute();
    locationStatus.value = "路线规划完成，可开启实时导航";
  } catch (error) {
    console.error("Failed to load AMap route", error);
    mapError.value = `${resolveAmapErrorMessage(error)}，已自动切换为估算导航`;
    applyFallbackRoute(mapError.value);
  } finally {
    planning.value = false;
  }
};

const reloadRoute = async () => {
  stopLiveNavigation();
  currentStopIndex.value = 0;
  await loadRoute();
};

watch(routeWatchKey, async () => {
  await reloadRoute();
});

onMounted(async () => {
  await loadRoute();
});

onBeforeUnmount(() => {
  stopLiveNavigation();
  clearRouteOverlays();
  if (mapInstance.value) {
    mapInstance.value.destroy();
    mapInstance.value = null;
  }
});
</script>

<style scoped>
.map-card {
  padding: 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.95), rgba(234, 220, 196, 0.72));
  border: 1px solid var(--heritage-border);
  box-shadow: var(--heritage-card-shadow-rest);
}

.map-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--heritage-gold);
}

.map-head h3 {
  margin: 0;
  color: var(--heritage-ink);
}

.sub-copy {
  margin-top: 8px;
  color: var(--heritage-muted);
  line-height: 1.7;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.map-alert {
  margin-bottom: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card,
.side-section {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 251, 244, 0.9);
}

.summary-card span,
.status-item span {
  display: block;
  font-size: 12px;
  color: var(--heritage-muted);
}

.summary-card strong,
.status-item strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--heritage-ink);
}

.summary-card p,
.side-copy,
.checkpoint-item p,
.instruction-item p {
  margin: 8px 0 0;
  color: var(--heritage-muted);
  line-height: 1.7;
}

.map-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.95fr);
  gap: 16px;
  align-items: start;
}

.map-pane {
  position: relative;
  min-height: 420px;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(255, 251, 244, 0.8);
}

.map-canvas {
  width: 100%;
  height: 420px;
  border-radius: 22px;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 251, 244, 0.72);
  color: var(--heritage-ink);
  font-weight: 600;
  backdrop-filter: blur(6px);
}

.map-side {
  display: grid;
  gap: 14px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.section-title strong {
  color: var(--heritage-ink);
}

.section-title span {
  font-size: 12px;
  color: var(--heritage-muted);
}

.checkpoint-list,
.instruction-list,
.status-grid {
  display: grid;
  gap: 10px;
}

.checkpoint-item,
.instruction-item,
.status-item {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(192, 138, 63, 0.08);
}

.checkpoint-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
}

.checkpoint-index {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--heritage-primary);
  color: var(--heritage-paper-soft);
  font-weight: 700;
  font-size: 12px;
}

.checkpoint-item strong,
.instruction-item strong {
  display: block;
  color: var(--heritage-ink);
}

.instruction-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.status-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.status-item {
  min-height: 84px;
}

.map-pane :deep(.trail-stop-marker) {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #a43b2f, #c08a3f);
  box-shadow: 0 8px 18px rgba(72, 41, 28, 0.22);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border: 2px solid rgba(255, 255, 255, 0.86);
}

.map-pane :deep(.trail-stop-label) {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(34, 49, 63, 0.86);
  color: #fff;
  font-size: 12px;
  white-space: nowrap;
}

.map-pane :deep(.trail-current-marker) {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.95);
  border: 4px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 8px rgba(64, 158, 255, 0.16);
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .map-stage {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .map-head {
    flex-direction: column;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .map-canvas {
    height: 360px;
  }

  .map-pane {
    min-height: 360px;
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .map-canvas {
    height: 320px;
  }

  .map-pane {
    min-height: 320px;
  }
}
</style>
