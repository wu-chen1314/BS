export interface ThemeConfig {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  categoryIds: number[];
}

export interface TrailNote {
  title: string;
  content: string;
}

export interface TrailTemplate {
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

export interface LearningTrack {
  id: string;
  title: string;
  subtitle: string;
  audience: string;
  duration: string;
  goal: string;
  keywords: string[];
  categoryIds: number[];
  deliverables: string[];
  linkedThemeId: string;
  trailPreset: {
    durationKey: "2h" | "4h" | "6h" | "day";
    budgetLevel: "low" | "medium" | "high";
    transportMode: "walk" | "public" | "car";
    maxStops: number;
    preferHot: boolean;
    regionKeyword?: string;
  };
}

export const CATEGORY_LABELS: Record<number, string> = {
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

export const CATEGORY_OPTIONS = Object.entries(CATEGORY_LABELS).map(([value, label]) => ({
  value: Number(value),
  label,
}));

export const PROTECT_LEVEL_OPTIONS = [
  { value: "国家级", label: "国家级" },
  { value: "省级", label: "省级" },
  { value: "市级", label: "市级" },
  { value: "县级", label: "县级" },
];

export const ROUTE_TITLES: Record<string, string> = {
  login: "登录",
  home: "非遗项目",
  curation: "主题策展",
  "heritage-trail": "非遗路线",
  "learning-studio": "研学工坊",
  "region-category": "地区分类",
  "hot-ranking": "热度排行",
  chat: "AI 助手",
  favorites: "我的收藏",
  inheritor: "传承人管理",
  user: "用户管理",
  profile: "个人中心",
};

export const CURATION_THEMES: ThemeConfig[] = [
  {
    id: "craft",
    title: "守艺匠心",
    subtitle: "技艺、器物与工坊细节",
    description:
      "聚焦传统技艺、传统美术与传统医药，用“看工艺、识材料、懂技法”的方式构建更适合传播的非遗专题内容。",
    categoryIds: [7, 8, 9],
  },
  {
    id: "festival",
    title: "节俗人间",
    subtitle: "节庆、习俗与地方记忆",
    description:
      "围绕民俗与民间文学进行主题重组，适合地方文化传播、节庆活动策划和社区非遗展示。",
    categoryIds: [1, 10],
  },
  {
    id: "performance",
    title: "声腔流韵",
    subtitle: "戏剧、音乐与曲艺舞台",
    description:
      "从可听、可看、可演的入口切入非遗内容，适合校园传播、舞台活动策划和沉浸式导览。",
    categoryIds: [2, 4, 5],
  },
];

export const TRAIL_TEMPLATES: TrailTemplate[] = [
  {
    id: "craft",
    title: "匠作工礼",
    subtitle: "器物、工坊与手艺肌理",
    description:
      "从传统美术、传统技艺到传统医药，适合用“看工艺细节”的方式进入非遗内容，强调材料、器具与技法传承。",
    duration: "半日到一日",
    entry: "先看工艺，再补人物",
    keywords: ["器物", "技法", "手工", "材料"],
    categoryIds: [7, 8, 9],
    notes: [
      { title: "适用场景", content: "适合主题导览、课程案例与品牌内容策划。" },
      { title: "浏览建议", content: "先看封面与简介，再进入详情页补全工艺脉络。" },
      { title: "推荐联动", content: "可与地区分类联动，生成区域工艺探索路线。" },
    ],
  },
  {
    id: "performance",
    title: "声腔剧场",
    subtitle: "戏曲、音乐与曲艺现场感",
    description:
      "聚焦传统音乐、传统戏剧与曲艺内容，适合用“舞台体验”的方式建立内容记忆和传播抓手。",
    duration: "一小时到半日",
    entry: "先看热度，再看项目",
    keywords: ["声腔", "舞台", "表演", "观看体验"],
    categoryIds: [2, 4, 5],
    notes: [
      { title: "适用场景", content: "适合活动策划、节庆宣发和大众内容传播。" },
      { title: "浏览建议", content: "先浏览热榜，再按相近表演形态延展阅读。" },
      { title: "推荐联动", content: "适合与热度排行配合使用，快速定位热点内容。" },
    ],
  },
  {
    id: "folk",
    title: "节俗人间",
    subtitle: "民间叙事与生活现场",
    description:
      "从民间文学、民俗到传统体育游艺，适合从地方记忆、节庆习俗和生活方式切入非遗内容。",
    duration: "半日",
    entry: "先看地区，再看生活场景",
    keywords: ["节俗", "地方记忆", "生活方式", "民间叙事"],
    categoryIds: [1, 3, 6, 10],
    notes: [
      { title: "适用场景", content: "适合校园传播、城市品牌叙事和节庆专题整合。" },
      { title: "浏览建议", content: "先看地区热区，再按生活场景挑选代表项目。" },
      { title: "推荐联动", content: "可与地图统计联动，形成从地区到习俗的传播路径。" },
    ],
  },
];

export const LEARNING_TRACKS: LearningTrack[] = [
  {
    id: "campus",
    title: "校园研学计划",
    subtitle: "面向课程、社团与课堂展示",
    audience: "教师、学生社团、课程策划者",
    duration: "2-3 课时 / 半日活动",
    goal: "快速建立对非遗门类、代表项目与地区分布的系统理解，适合课堂导入与项目化学习。",
    keywords: ["课程导入", "课堂展示", "案例教学", "分组共创"],
    categoryIds: [1, 2, 4, 8],
    deliverables: ["一页课程导图", "项目观察卡", "主题展示提纲"],
    linkedThemeId: "performance",
    trailPreset: {
      durationKey: "4h",
      budgetLevel: "medium",
      transportMode: "public",
      maxStops: 4,
      preferHot: true,
    },
  },
  {
    id: "community",
    title: "社区传播计划",
    subtitle: "面向街区、社区与文化活动",
    audience: "社区工作者、活动策划者、文化馆志愿者",
    duration: "半日到一日",
    goal: "围绕地区和节俗组织可传播、可互动、可落地的非遗活动路线，适合公共文化活动设计。",
    keywords: ["社区活动", "节庆策划", "地方叙事", "公众互动"],
    categoryIds: [1, 6, 10],
    deliverables: ["活动主题墙", "地区故事线", "互动体验清单"],
    linkedThemeId: "festival",
    trailPreset: {
      durationKey: "day",
      budgetLevel: "medium",
      transportMode: "public",
      maxStops: 5,
      preferHot: true,
    },
  },
  {
    id: "family",
    title: "亲子探索计划",
    subtitle: "面向家庭与周末文化体验",
    audience: "亲子家庭、公众游客、文旅体验人群",
    duration: "1-2 小时",
    goal: "以看得见、听得懂、能参与的方式完成轻量化非遗启蒙，适合家庭与周末短时体验。",
    keywords: ["亲子导览", "体验活动", "轻量学习", "兴趣启发"],
    categoryIds: [2, 5, 7, 8],
    deliverables: ["家庭观察任务", "体验路线建议", "延伸阅读清单"],
    linkedThemeId: "craft",
    trailPreset: {
      durationKey: "2h",
      budgetLevel: "low",
      transportMode: "walk",
      maxStops: 3,
      preferHot: true,
    },
  },
];
