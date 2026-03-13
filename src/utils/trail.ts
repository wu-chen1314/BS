import { TRAIL_BUDGET_OPTIONS, TRAIL_DURATION_OPTIONS, TRAIL_TEMPLATE_LIBRARY, TRAIL_TRANSPORT_OPTIONS } from "@/constants/trail";
import type {
  BudgetLevel,
  DurationKey,
  TrailInheritorProfile,
  TrailPlannerForm,
  TrailStopView,
  TrailTemplateConfig,
  TrailViewModel,
  TransportMode,
} from "@/types/trail";
import type { HeritageProject } from "@/types/project";
import { getCategoryName, summarizeRichText } from "@/utils/heritage";

export const DURATION_HOURS_MAP: Record<DurationKey, number> = {
  "2h": 2,
  "4h": 4,
  "6h": 6,
  day: 8,
};

const DURATION_LABEL_MAP: Record<DurationKey, string> = Object.fromEntries(
  TRAIL_DURATION_OPTIONS.map((item) => [item.value, item.label])
) as Record<DurationKey, string>;

const TRANSPORT_LABEL_MAP: Record<TransportMode, string> = Object.fromEntries(
  TRAIL_TRANSPORT_OPTIONS.map((item) => [item.value, item.label])
) as Record<TransportMode, string>;

const BUDGET_LABEL_MAP: Record<BudgetLevel, string> = Object.fromEntries(
  TRAIL_BUDGET_OPTIONS.map((item) => [item.value, item.label])
) as Record<BudgetLevel, string>;

const CATEGORY_DURATION_WEIGHT: Record<number, number> = {
  1: 1.1,
  2: 1.3,
  3: 1.2,
  4: 1.5,
  5: 1.2,
  6: 1.1,
  7: 1.5,
  8: 1.8,
  9: 1.6,
  10: 1.2,
};

const CATEGORY_BUDGET_WEIGHT: Record<number, number> = {
  1: 0.85,
  2: 1.0,
  3: 1.0,
  4: 1.1,
  5: 0.95,
  6: 0.9,
  7: 1.15,
  8: 1.25,
  9: 1.2,
  10: 0.95,
};

const MAX_STOPS = 6;

export const createDefaultTrailPlanner = (): TrailPlannerForm => ({
  interestIds: [],
  durationKey: "4h",
  budgetLevel: "medium",
  transportMode: "public",
  regionKeyword: "",
  maxStops: 4,
  preferHot: true,
});

export const getDurationLabel = (value: DurationKey) => DURATION_LABEL_MAP[value];
export const getTransportLabel = (value: TransportMode) => TRANSPORT_LABEL_MAP[value];
export const getBudgetLabel = (value: BudgetLevel) => BUDGET_LABEL_MAP[value];

export const buildCustomTrailId = (planner: TrailPlannerForm) =>
  [
    "custom",
    planner.durationKey,
    planner.budgetLevel,
    planner.transportMode,
    [...planner.interestIds].sort((left, right) => left - right).join(".") || "all",
    encodeURIComponent(planner.regionKeyword.trim() || "all"),
    String(planner.maxStops),
    planner.preferHot ? "1" : "0",
  ].join("~");

export const parseCustomTrailId = (trailId: string): TrailPlannerForm | null => {
  const parts = trailId.split("~");
  if (parts[0] !== "custom" || parts.length < 8) {
    return null;
  }

  const durationKey = parts[1] as DurationKey;
  const budgetLevel = parts[2] as BudgetLevel;
  const transportMode = parts[3] as TransportMode;
  if (!DURATION_HOURS_MAP[durationKey] || !BUDGET_LABEL_MAP[budgetLevel] || !TRANSPORT_LABEL_MAP[transportMode]) {
    return null;
  }

  return {
    interestIds: parts[4] === "all" ? [] : parts[4].split(".").map((item) => Number(item)).filter(Number.isFinite),
    durationKey,
    budgetLevel,
    transportMode,
    regionKeyword: decodeURIComponent(parts[5] === "all" ? "" : parts[5]),
    maxStops: Math.max(2, Math.min(Number(parts[6]) || 4, MAX_STOPS)),
    preferHot: parts[7] !== "0",
  };
};

export const resolveTrailTemplate = (trailId: string) =>
  TRAIL_TEMPLATE_LIBRARY.find((item) => item.id === trailId) || TRAIL_TEMPLATE_LIBRARY[0];

export const buildTemplateTrail = (
  template: TrailTemplateConfig,
  projects: HeritageProject[],
  inheritorMap: Record<number, TrailInheritorProfile[]>,
  planner: TrailPlannerForm
): TrailViewModel => {
  const mergedPlanner: TrailPlannerForm = {
    ...planner,
    interestIds: template.categoryIds,
  };

  const stops = buildTrailStops(projects, inheritorMap, mergedPlanner, template.categoryIds);
  const estimatedHours = estimateRouteHours(stops, mergedPlanner.transportMode);
  const estimatedCost = estimateRouteCost(stops, mergedPlanner.transportMode);

  return {
    id: template.id,
    routeType: "template",
    title: template.title,
    subtitle: template.subtitle,
    description: `${template.description} 推荐受众：${template.audience}。最佳体验季节：${template.season}。`,
    durationLabel: getDurationLabel(mergedPlanner.durationKey),
    transportLabel: getTransportLabel(mergedPlanner.transportMode),
    budgetLabel: getBudgetLabel(mergedPlanner.budgetLevel),
    estimatedHours,
    estimatedCost,
    stopCount: stops.length,
    keywords: [...template.keywords, template.highlight],
    notes: [
      ...template.notes,
      {
        title: "路线实用建议",
        content: `建议以${getTransportLabel(mergedPlanner.transportMode)}展开，控制在 ${Math.max(
          estimatedHours - 0.8,
          1
        ).toFixed(1)}-${estimatedHours.toFixed(1)} 小时之间，预算按 ${getBudgetLabel(mergedPlanner.budgetLevel)} 规划。`,
      },
    ],
    stops,
  };
};

export const buildCustomTrail = (
  projects: HeritageProject[],
  inheritorMap: Record<number, TrailInheritorProfile[]>,
  planner: TrailPlannerForm
): TrailViewModel => {
  const selectedCategoryIds = planner.interestIds.length
    ? planner.interestIds
    : TRAIL_TEMPLATE_LIBRARY.flatMap((item) => item.categoryIds).slice(0, 4);
  const stops = buildTrailStops(projects, inheritorMap, planner, selectedCategoryIds);
  const estimatedHours = estimateRouteHours(stops, planner.transportMode);
  const estimatedCost = estimateRouteCost(stops, planner.transportMode);
  const interestLabels = selectedCategoryIds.map((item) => getCategoryName(item));

  return {
    id: buildCustomTrailId(planner),
    routeType: "custom",
    title: "自定义非遗路线",
    subtitle: "按兴趣、时长、预算和出行方式即时生成",
    description: `这条路线根据你的兴趣偏好（${interestLabels.join(" / ")}）自动生成，兼顾路线密度、内容代表性和游览实用性。`,
    durationLabel: getDurationLabel(planner.durationKey),
    transportLabel: getTransportLabel(planner.transportMode),
    budgetLabel: getBudgetLabel(planner.budgetLevel),
    estimatedHours,
    estimatedCost,
    stopCount: stops.length,
    keywords: [...interestLabels, planner.regionKeyword || "跨区域串联", planner.preferHot ? "热门优先" : "均衡探索"],
    notes: [
      {
        title: "路线生成逻辑",
        content: "优先选择与你的兴趣相符、资料较完整、热度较高且便于串联的项目，避免同类点位过于重复。",
      },
      {
        title: "出行建议",
        content: `${getTransportLabel(planner.transportMode)}更适合这条路线；若希望降低成本，可减少停留点位或将预算切换到轻享模式。`,
      },
      {
        title: "实地体验建议",
        content: "若遇到传承人工作坊或展演场次，建议提前电话咨询，优先预约互动性更强的场景。",
      },
    ],
    stops,
  };
};

export const collectInterestLabels = (planner: TrailPlannerForm) =>
  planner.interestIds.map((item) => getCategoryName(item));

const buildTrailStops = (
  sourceProjects: HeritageProject[],
  inheritorMap: Record<number, TrailInheritorProfile[]>,
  planner: TrailPlannerForm,
  preferredCategoryIds: number[]
) => {
  const maxStops = resolveStopLimit(planner);
  const categorySet = new Set(preferredCategoryIds.map(Number));
  const regionKeyword = planner.regionKeyword.trim().toLowerCase();

  const rankedProjects = sourceProjects
    .filter((project) => categorySet.size === 0 || categorySet.has(Number(project.categoryId)))
    .filter((project) => matchesRegion(project, regionKeyword))
    .sort((left, right) => scoreProject(right, planner) - scoreProject(left, planner));
  const coordinateReadyProjects = rankedProjects.filter(hasProjectCoordinates);
  const candidates =
    coordinateReadyProjects.length >= 2 ? coordinateReadyProjects.slice(0, maxStops) : rankedProjects.slice(0, maxStops);

  return candidates.map((project, index) => {
    const inheritors = inheritorMap[Number(project.id)] || [];
    const previousProject = index > 0 ? candidates[index - 1] : undefined;
    const visitDurationHours = estimateVisitDuration(project);
    const estimatedSpend = estimateStopCost(project, planner.budgetLevel);
    return {
      project,
      inheritors,
      visitDurationHours,
      visitDurationLabel: formatVisitDuration(visitDurationHours),
      transferTip: buildTransferTip(previousProject, project, planner.transportMode),
      estimatedSpend,
      facilityTags: deriveFacilityTags(project, planner.transportMode),
      supportTips: buildSupportTips(project, planner.transportMode, visitDurationHours),
      historicalContext: buildHistoricalContext(project, inheritors),
    } satisfies TrailStopView;
  });
};

const scoreProject = (project: HeritageProject, planner: TrailPlannerForm) => {
  const viewScore = Number(project.viewCount || 0) / 1000;
  const contentScore =
    (project.history ? 2 : 0) +
    (project.features ? 1 : 0) +
    (project.coverUrl ? 1 : 0) +
    (project.inheritorNames ? 1.2 : 0) +
    (project.address ? 0.6 : 0) +
    (project.openingHours ? 0.5 : 0);
  const hotBonus = planner.preferHot ? viewScore : Math.min(viewScore, 1.8);
  return contentScore + hotBonus;
};

const matchesRegion = (project: HeritageProject, keyword: string) => {
  if (!keyword) {
    return true;
  }
  const target = [project.regionName, project.address]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  return target.includes(keyword);
};

const hasProjectCoordinates = (project: HeritageProject) =>
  Number.isFinite(Number(project.longitude)) && Number.isFinite(Number(project.latitude));

const resolveStopLimit = (planner: TrailPlannerForm) => {
  const durationHours = DURATION_HOURS_MAP[planner.durationKey];
  const transportFactor: Record<TransportMode, number> = {
    walk: 1.6,
    public: 1.3,
    car: 1.15,
  };
  const suggested = Math.floor(durationHours / transportFactor[planner.transportMode]);
  return Math.max(2, Math.min(planner.maxStops || suggested || 4, suggested || 4, MAX_STOPS));
};

const estimateVisitDuration = (project: HeritageProject) => {
  const base = CATEGORY_DURATION_WEIGHT[Number(project.categoryId)] || 1.1;
  const protectBonus = project.protectLevel?.includes("国家") ? 0.3 : project.protectLevel?.includes("省") ? 0.2 : 0.1;
  return roundToHalf(base + protectBonus);
};

const estimateStopCost = (project: HeritageProject, budgetLevel: BudgetLevel) => {
  const baseCost: Record<BudgetLevel, number> = {
    low: 40,
    medium: 80,
    high: 140,
  };
  const categoryFactor = CATEGORY_BUDGET_WEIGHT[Number(project.categoryId)] || 1;
  return Math.round(baseCost[budgetLevel] * categoryFactor);
};

const estimateRouteHours = (stops: TrailStopView[], transportMode: TransportMode) => {
  const stayHours = stops.reduce((sum, item) => sum + item.visitDurationHours, 0);
  const transferHours = Math.max(stops.length - 1, 0) * ({ walk: 0.35, public: 0.5, car: 0.4 }[transportMode]);
  return roundToOne(stayHours + transferHours);
};

const estimateRouteCost = (stops: TrailStopView[], transportMode: TransportMode) => {
  const stopCost = stops.reduce((sum, item) => sum + item.estimatedSpend, 0);
  const transportCost = Math.max(stops.length - 1, 0) * ({ walk: 8, public: 16, car: 36 }[transportMode]);
  return Math.round(stopCost + transportCost);
};

const buildTransferTip = (
  previousProject: HeritageProject | undefined,
  currentProject: HeritageProject,
  transportMode: TransportMode
) => {
  if (!previousProject) {
    return `建议从 ${currentProject.regionName || "当前城市文化地标"} 开始进入路线。`;
  }
  const sameRegion = previousProject.regionName && previousProject.regionName === currentProject.regionName;
  if (transportMode === "walk") {
    return sameRegion ? "步行或短距接驳约 15-25 分钟，适合慢游串联。" : "建议步行+打车接驳，跨区机动约 35-50 分钟。";
  }
  if (transportMode === "car") {
    return sameRegion ? "自驾/打车机动约 10-20 分钟，适合压缩转场时间。" : "跨区自驾约 30-50 分钟，注意停车点和高峰拥堵。";
  }
  return sameRegion ? "公共交通接驳约 20-35 分钟，适合城市内半日串联。" : "跨区公共交通约 40-70 分钟，建议提前预留换乘时间。";
};

const deriveFacilityTags = (project: HeritageProject, transportMode: TransportMode) => {
  const tags = new Set<string>();
  if (project.openingHours) {
    tags.add("开放时段明确");
  }
  if (project.contactPhone) {
    tags.add("可电话咨询");
  }
  if (project.address) {
    tags.add("定位友好");
  }
  if (Number(project.viewCount || 0) >= 3000) {
    tags.add("热门点位");
    tags.add("周边配套较成熟");
  }
  if (project.videoUrl) {
    tags.add("有影像资料");
  }
  if (project.inheritorNames) {
    tags.add("可串联传承人故事");
  }
  if (transportMode === "walk") {
    tags.add("适合慢游体验");
  } else if (transportMode === "car") {
    tags.add("适合连点成线");
  } else {
    tags.add("适合城市公共交通");
  }
  return Array.from(tags).slice(0, 5);
};

const buildSupportTips = (project: HeritageProject, transportMode: TransportMode, visitDurationHours: number) => {
  const tips = [
    `建议单点停留 ${formatVisitDuration(visitDurationHours)}，以 ${getTransportLabel(transportMode)} 为主。`,
  ];
  if (project.openingHours) {
    tips.push(`开放时间：${project.openingHours}`);
  }
  if (project.address) {
    tips.push(`地址线索：${project.address}`);
  }
  if (project.contactPhone) {
    tips.push(`咨询电话：${project.contactPhone}`);
  } else {
    tips.push("如需深度参访，建议提前联系场馆或当地文旅服务台。");
  }
  return tips.slice(0, 3);
};

const buildHistoricalContext = (project: HeritageProject, inheritors: TrailInheritorProfile[]) => {
  const history = summarizeRichText(project.history, 86, "该项目可从地方记忆、技艺脉络和现实传承三个层面理解。");
  const features = summarizeRichText(project.features, 60, "");
  const inheritor = inheritors[0]
    ? `${inheritors[0].name}${inheritors[0].level ? `（${inheritors[0].level}）` : ""}`
    : project.inheritorNames || "";
  return [history, features ? `推荐关注：${features}` : "", inheritor ? `传承人线索：${inheritor}` : ""]
    .filter(Boolean)
    .join(" ");
};

const formatVisitDuration = (hours: number) => {
  if (hours <= 1) {
    return `${Math.round(hours * 60)} 分钟`;
  }
  if (Number.isInteger(hours)) {
    return `${hours} 小时`;
  }
  return `${hours.toFixed(1)} 小时`;
};

const roundToHalf = (value: number) => Math.max(0.5, Math.round(value * 2) / 2);
const roundToOne = (value: number) => Math.round(value * 10) / 10;
