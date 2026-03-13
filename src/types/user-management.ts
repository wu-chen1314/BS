export interface UserRecord {
  id?: number;
  username?: string;
  nickname?: string | null;
  email?: string | null;
  phone?: string | null;
  avatarUrl?: string | null;
  role?: "admin" | "user" | string | null;
  status?: number | null;
}

export interface UserQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

