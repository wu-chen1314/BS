export interface InheritorRecord {
  id?: number;
  name: string;
  avatarUrl?: string | null;
  sex?: string | null;
  level?: string | null;
  description?: string | null;
  projectId?: number | null;
  projectName?: string | null;
}

export interface InheritorQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  projectId?: number;
}

export interface ProjectOption {
  id: number;
  name: string;
}

