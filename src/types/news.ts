export interface NewsArticleCard {
  id: number;
  title: string;
  summary: string;
  source: string;
  tag: string;
  tagType?: string | null;
  image?: string | null;
  coverImageUrl?: string | null;
  videoUrl?: string | null;
  publishedAt: string;
  viewCount: number;
}

export interface NewsArticleDetail extends NewsArticleCard {
  content: string;
}

export interface NewsArticleEditorPayload {
  title: string;
  summary?: string;
  content: string;
  source?: string;
  tag?: string;
  tagType?: string;
  coverImageUrl?: string;
  videoUrl?: string;
  publishedAt?: string;
}

export interface NewsArticlePage {
  records: NewsArticleCard[];
  total: number;
  current: number;
  size: number;
  hasMore: boolean;
  availableTags: string[];
}

export interface NewsDashboardMetric {
  name: string;
  value: number;
}

export interface NewsReadHistoryItem extends NewsArticleCard {
  lastReadAt?: string | null;
}

export interface NewsDashboard {
  articleCount: number;
  totalViewCount: number;
  featuredTags: NewsDashboardMetric[];
  hotArticles: NewsArticleCard[];
  recentReads: NewsReadHistoryItem[];
}
