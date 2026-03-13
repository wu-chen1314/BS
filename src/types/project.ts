export interface HeritageProject {
  id?: number;
  name: string;
  categoryId?: number | null;
  categoryName?: string | null;
  regionId?: number | null;
  regionName?: string | null;
  protectLevel?: string | null;
  status?: string | null;
  history?: string | null;
  features?: string | null;
  coverUrl?: string | null;
  videoUrl?: string | null;
  inheritorNames?: string | null;
  inheritorIds?: number[];
  viewCount?: number | null;
  longitude?: number | null;
  latitude?: number | null;
  address?: string | null;
  contactPhone?: string | null;
  openingHours?: string | null;
  auditStatus?: number | null;
}

export interface StatisticItem {
  name: string;
  value: number;
}

export interface HotProjectItem {
  id: number;
  name: string;
  coverUrl?: string | null;
  viewCount?: number | null;
}

export interface ProjectComment {
  id: number;
  projectId: number;
  userId?: number | null;
  content: string;
  nickname?: string | null;
  avatarUrl?: string | null;
  createdAt?: string | null;
}
