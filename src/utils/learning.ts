import { CURATION_THEMES, type LearningTrack } from "@/constants/heritage";
import type { HeritageProject, StatisticItem } from "@/types/project";
import type {
  LearningPlanModule,
  LearningPlanViewModel,
  LearningTeacherAgendaItem,
  LearningTeacherSheet,
} from "@/types/learning";
import { buildCustomTrailId, createDefaultTrailPlanner } from "@/utils/trail";
import { summarizeRichText } from "@/utils/heritage";

const moduleTemplates: Array<{
  title: string;
  duration: string;
  objective: (track: LearningTrack) => string;
  activities: (track: LearningTrack, projects: HeritageProject[], regionName: string) => string[];
  outputs: (track: LearningTrack) => string[];
}> = [
  {
    title: "主题导入",
    duration: "15-20 分钟",
    objective: (track) => `建立“${track.title}”的观察视角和学习目标。`,
    activities: (track, projects, regionName) => [
      `用 ${projects.slice(0, 2).map((item) => item.name).join("、") || "代表性项目"} 作为导入案例。`,
      `从 ${track.keywords.join("、")} 三个关键词切入项目观察。`,
      `结合 ${regionName || "重点地区"} 的地域背景说明非遗分布特点。`,
    ],
    outputs: (track) => [`完成一张${track.title}导入观察卡`, "记录首轮问题与兴趣点"],
  },
  {
    title: "项目拆解",
    duration: "25-35 分钟",
    objective: (track) => `围绕${track.goal}`,
    activities: (_track, projects) => [
      `小组分工阅读 ${projects.slice(0, 3).map((item) => item.name).join("、") || "重点项目"} 的背景与技艺介绍。`,
      "提炼项目共同点、地域差异和可传播亮点。",
      "补充传承人信息和历史文化背景，形成主题叙事线。",
    ],
    outputs: () => ["形成项目对比表", "完成一段 100-150 字主题讲述"],
  },
  {
    title: "路线联动",
    duration: "20-30 分钟",
    objective: () => "将研学内容转化为可游览、可实践的线下活动方案。",
    activities: (_track, projects, regionName) => [
      `优先选择 ${projects.slice(0, 4).map((item) => item.name).join("、") || "代表项目"} 进入路线候选池。`,
      `将 ${regionName || "当前热区"} 作为实地考察优先区域。`,
      "结合交通方式、时间预算和活动目标，生成一条非遗路线。",
    ],
    outputs: () => ["生成路线建议", "补全现场观察任务与安全提示"],
  },
  {
    title: "成果产出",
    duration: "15-20 分钟",
    objective: (track) => `将学习结果沉淀为${track.deliverables.join("、")}等成果。`,
    activities: (track) => [
      `围绕 ${track.keywords[0]}、${track.keywords[1] || track.keywords[0]} 输出传播内容。`,
      "整理活动照片、关键词和访谈记录。",
      "复盘活动节奏、互动效果和后续延展方向。",
    ],
    outputs: (track) => track.deliverables,
  },
];

export const buildLearningPlanId = (trackId: string, regionName: string, projectIds: number[]) =>
  [
    "learning",
    trackId,
    encodeURIComponent(regionName.trim() || "all"),
    projectIds.slice(0, 4).map((item) => String(item)).join(".") || "none",
  ].join("~");

export const parseLearningPlanId = (planId: string) => {
  const parts = planId.split("~");
  if (parts.length < 4 || parts[0] !== "learning") {
    return null;
  }
  return {
    trackId: parts[1],
    regionName: decodeURIComponent(parts[2] === "all" ? "" : parts[2]),
    projectIds:
      parts[3] === "none"
        ? []
        : parts[3].split(".").map((item) => Number(item)).filter((item) => Number.isFinite(item)),
  };
};

export const buildLearningRouteQuery = (track: LearningTrack, regionName: string) => {
  const planner = {
    ...createDefaultTrailPlanner(),
    interestIds: [...track.categoryIds],
    ...track.trailPreset,
    regionKeyword: track.trailPreset.regionKeyword || regionName || "",
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
    source: "learning-studio",
    track: track.id,
  };
};

export const createLearningPlan = (
  track: LearningTrack,
  projects: HeritageProject[],
  topRegions: StatisticItem[]
): LearningPlanViewModel => {
  const regionName = topRegions[0]?.name || projects[0]?.regionName || "重点地区";
  const linkedTheme = CURATION_THEMES.find((item) => item.id === track.linkedThemeId) || CURATION_THEMES[0];
  const planId = buildLearningPlanId(
    track.id,
    regionName,
    projects.map((item) => Number(item.id)).filter((item) => Number.isFinite(item))
  );
  const modules = createLearningModules(track, projects, regionName);
  const teacherSheet = createTeacherSheet(track, linkedTheme.title, regionName, projects, modules);

  return {
    id: planId,
    trackId: track.id,
    title: `${track.title}方案`,
    subtitle: track.subtitle,
    audience: track.audience,
    duration: track.duration,
    goal: track.goal,
    keywords: track.keywords,
    deliverables: track.deliverables,
    linkedThemeId: linkedTheme.id,
    linkedThemeTitle: linkedTheme.title,
    linkedThemeSubtitle: linkedTheme.subtitle,
    highlightedRegion: regionName,
    projectCount: projects.length,
    projects,
    modules,
    teacherSheet,
  };
};

export const summarizeLearningPlan = (plan: LearningPlanViewModel) =>
  `${plan.title} 面向 ${plan.audience}，建议时长 ${plan.duration}，以 ${plan.highlightedRegion} 为重点区域，围绕 ${plan.keywords.join("、")} 展开。`;

const createLearningModules = (track: LearningTrack, projects: HeritageProject[], regionName: string): LearningPlanModule[] =>
  moduleTemplates.map((template) => ({
    title: template.title,
    duration: template.duration,
    objective: template.objective(track),
    activities: template.activities(track, projects, regionName),
    outputs: template.outputs(track),
  }));

const createTeacherSheet = (
  track: LearningTrack,
  linkedThemeTitle: string,
  regionName: string,
  projects: HeritageProject[],
  modules: LearningPlanModule[]
): LearningTeacherSheet => {
  const agenda = modules.map<LearningTeacherAgendaItem>((module, index) => ({
    title: module.title,
    duration: module.duration,
    objective: module.objective,
    teacherTasks: [
      `引导学生围绕模块 ${index + 1} 的目标开展观察与讨论。`,
      `补充 ${projects[index]?.name || projects[0]?.name || "代表项目"} 的背景资料与提问线索。`,
    ],
    studentTasks: [
      `完成“${module.title}”阶段任务记录。`,
      `产出${module.outputs.join("、")}中的至少一项成果。`,
    ],
  }));

  return {
    title: `${track.title}教师活动单`,
    summary: `${track.title}以“${linkedThemeTitle}”为主题支点，结合${regionName}的区域热度与非遗项目开展课堂或活动组织。`,
    targetAudience: track.audience,
    suggestedDuration: track.duration,
    prepChecklist: [
      "确认活动目标、参与人数与场地形式",
      `准备 ${projects.slice(0, 3).map((item) => item.name).join("、") || "代表项目"} 的图片或简介`,
      "提前确认路线候选点位与交通方式",
      "准备观察卡、讨论问题和成果展示模板",
    ],
    materials: [
      "项目封面与基础介绍",
      "地区分布与热区统计",
      "主题关键词卡片",
      "成果展示模板或活动海报底板",
    ],
    discussionPrompts: [
      `${track.keywords[0]} 在当前非遗项目中体现在哪里？`,
      `${regionName} 为什么会成为本次活动的重点区域？`,
      "如果把今天的学习延展为实地路线，最值得保留的项目站点是什么？",
    ],
    assessmentPoints: [
      "能否准确说出项目门类、地区与文化价值",
      "能否完成一条清晰的主题叙事或路线建议",
      "能否把成果沉淀为可传播、可展示的内容",
    ],
    agenda,
  };
};

export const buildLearningProjectHighlights = (projects: HeritageProject[]) =>
  projects.slice(0, 4).map((project) => ({
    id: Number(project.id),
    title: project.name,
    summary: summarizeRichText(project.history || project.features, 72),
    regionName: project.regionName || "地区待补充",
    categoryName: project.categoryName || "门类待补充",
  }));
