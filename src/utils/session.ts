export interface SessionUser {
  id?: number;
  username?: string;
  nickname?: string;
  role?: string;
  avatarUrl?: string;
  token?: string;
}

export const SESSION_CHANGED_EVENT = "session-changed";

const USER_KEY = "user";
const TOKEN_KEY = "token";
const TOKEN_EXPIRES_KEY = "tokenExpires";
const YEAR_IN_SECONDS = 365 * 24 * 60 * 60;

const parseStoredUser = (raw: string | null): SessionUser | null => {
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionUser;
  } catch (error) {
    console.error("Failed to parse current user", error);
    return null;
  }
};

const normalizeExpirationValue = (raw: string | number) => {
  const expires = Number(raw);
  if (!Number.isFinite(expires) || expires <= 0) {
    return null;
  }

  if (expires >= 1_000_000_000_000) {
    return expires;
  }

  if (expires > YEAR_IN_SECONDS) {
    return Date.now() + expires;
  }

  return Date.now() + expires * 1000;
};

const emitSessionChanged = () => {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(new Event(SESSION_CHANGED_EVENT));
};

export const getStoredToken = () => {
  if (typeof window === "undefined") {
    return "";
  }

  return window.sessionStorage.getItem(TOKEN_KEY) || "";
};

export const isSessionExpired = () => {
  if (typeof window === "undefined") {
    return false;
  }

  const expiresAt = window.sessionStorage.getItem(TOKEN_EXPIRES_KEY);
  if (!expiresAt) {
    return false;
  }

  const expiresTimestamp = Number(expiresAt);
  if (!Number.isFinite(expiresTimestamp)) {
    return false;
  }

  return Date.now() >= expiresTimestamp;
};

export const getCurrentUser = (): SessionUser | null => {
  if (typeof window === "undefined") {
    return null;
  }

  if (isSessionExpired()) {
    clearCurrentSession();
    return null;
  }

  const parsedUser = parseStoredUser(window.sessionStorage.getItem(USER_KEY));
  if (!parsedUser) {
    clearCurrentSession();
    return null;
  }

  const storedToken = getStoredToken();
  if (storedToken && !parsedUser.token) {
    return {
      ...parsedUser,
      token: storedToken,
    };
  }

  return parsedUser;
};

export const saveCurrentSession = (user: SessionUser, token?: string, expiresIn?: string | number) => {
  if (typeof window === "undefined") {
    return;
  }

  const mergedUser = {
    ...user,
    token: token || user.token,
  };

  window.sessionStorage.setItem(USER_KEY, JSON.stringify(mergedUser));
  if (token) {
    window.sessionStorage.setItem(TOKEN_KEY, token);
  }
  if (expiresIn !== undefined && expiresIn !== null) {
    const normalizedExpiration = normalizeExpirationValue(expiresIn);
    if (normalizedExpiration) {
      window.sessionStorage.setItem(TOKEN_EXPIRES_KEY, String(normalizedExpiration));
    } else {
      window.sessionStorage.removeItem(TOKEN_EXPIRES_KEY);
    }
  }

  emitSessionChanged();
};

export const clearCurrentSession = (options?: { emitEvent?: boolean }) => {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(USER_KEY);
  window.sessionStorage.removeItem(TOKEN_KEY);
  window.sessionStorage.removeItem(TOKEN_EXPIRES_KEY);
  if (options?.emitEvent !== false) {
    emitSessionChanged();
  }
};
