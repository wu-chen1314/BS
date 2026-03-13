import type { TrailTemplateConfig } from "@/types/trail";

export const TRAIL_DURATION_OPTIONS = [
  { value: "2h", label: "2 小时轻游" },
  { value: "4h", label: "半日深看" },
  { value: "6h", label: "6 小时沉浸" },
  { value: "day", label: "1 日完整路线" },
] as const;

export const TRAIL_TRANSPORT_OPTIONS = [
  { value: "walk", label: "步行+短距接驳" },
  { value: "public", label: "公共交通优先" },
  { value: "car", label: "自驾/包车" },
] as const;

export const TRAIL_BUDGET_OPTIONS = [
  { value: "low", label: "轻享预算" },
  { value: "medium", label: "平衡预算" },
  { value: "high", label: "深度体验预算" },
] as const;

export const TRAIL_TEMPLATE_LIBRARY: TrailTemplateConfig[] = [
  {
    id: "craft",
    title: "匠作巡礼",
    subtitle: "工艺细节、作坊气质与器物之美",
    description:
      "适合从传统技艺、传统美术和传统医药切入，强调看工艺、识材料、读工坊，让路线兼具观赏性和学习性。",
    categoryIds: [7, 8, 9],
    durationKey: "6h",
    budgetLevel: "medium",
    transportMode: "public",
    season: "春秋季更适合深度参观",
    audience: "研学团体、设计从业者、亲子家庭",
    highlight: "重点看技艺流程、材料故事与代表性传承人",
    keywords: ["工艺", "器物", "手作", "材料", "匠人"],
    notes: [
      { title: "推荐节奏", content: "先看代表项目，再补技艺脉络，最后安排互动体验或文创采购。" },
      { title: "交通建议", content: "优先公共交通接驳，城市内可步行+打车混合，提高单日到访效率。" },
      { title: "内容亮点", content: "适合做摄影、短视频、图文导览和研学课程笔记。" },
    ],
  },
  {
    id: "performance",
    title: "声腔剧场",
    subtitle: "听戏曲、看舞台、读地方声腔",
    description:
      "围绕传统戏剧、传统音乐和曲艺展开，从舞台表现、地方腔调和表演结构进入非遗，适合大众传播与活动策划。",
    categoryIds: [2, 4, 5],
    durationKey: "4h",
    budgetLevel: "medium",
    transportMode: "public",
    season: "节庆与演出季体验最佳",
    audience: "城市游客、文化爱好者、活动策划团队",
    highlight: "重点看代表性剧种、地方腔调和可演可讲的内容结构",
    keywords: ["戏曲", "表演", "舞台", "声腔", "地方文化"],
    notes: [
      { title: "推荐节奏", content: "先锁定一条主剧种线索，再串联相近地域或相近表演形态。" },
      { title: "交通建议", content: "适合城市内半日路线，演出场所与展陈空间之间保留 20-40 分钟机动时间。" },
      { title: "内容亮点", content: "适合做导赏词、短讲解、活动串词和舞台前置预热。" },
    ],
  },
  {
    id: "folk",
    title: "节俗人间",
    subtitle: "地方记忆、节庆传统与生活方式",
    description:
      "聚焦民俗、民间文学和传统体育游艺，从节庆礼俗、地方记忆与生活现场切入，强调可参与、可讲述、可传播。",
    categoryIds: [1, 3, 6, 10],
    durationKey: "4h",
    budgetLevel: "low",
    transportMode: "walk",
    season: "节庆假日与周末最适合",
    audience: "亲子家庭、社区活动组织者、文化旅游用户",
    highlight: "重点看仪式感、生活感和地方共同体记忆",
    keywords: ["民俗", "节庆", "生活方式", "地方记忆", "参与体验"],
    notes: [
      { title: "推荐节奏", content: "适合从一个节俗点位切入，再串联相关故事、技艺和地方空间。" },
      { title: "交通建议", content: "优先步行和短距接驳，便于保留现场观察和参与感。" },
      { title: "内容亮点", content: "适合社区共创、节庆策展、亲子任务卡和周末体验活动。" },
    ],
  },
];
