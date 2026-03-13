import request from "@/utils/request";

const publicRequestMeta = {
  skipAuth: true,
  skipErrorHandler: true,
  skipSessionRedirect: true,
} as const;

export interface LoginPayload {
  username: string;
  password: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface RegisterPayload {
  username: string;
  passwordHash: string;
  nickname: string;
  email: string;
  code: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface ResetPasswordPayload {
  username: string;
  email: string;
  code: string;
  newPassword: string;
}

export const fetchCaptcha = async () => {
  const candidates = ["/auth/captcha", "/captcha", "/login/captcha"];
  let lastError: unknown;

  for (const path of candidates) {
    try {
      return await request.get(path, {
        meta: publicRequestMeta,
      });
    } catch (error: any) {
      lastError = error;
      if (error?.status !== 404) {
        throw error;
      }
    }
  }

  throw lastError;
};

export const loginWithPassword = (payload: LoginPayload) =>
  request.post("/login", payload, {
    meta: publicRequestMeta,
  });

export const sendVerificationCode = (email: string) =>
  request.post(
    "/auth/send-code",
    {
      email,
    },
    {
      meta: publicRequestMeta,
    }
  );

export const registerAccount = (payload: RegisterPayload) =>
  request.post(
    `/auth/register?code=${encodeURIComponent(payload.code)}&captchaId=${encodeURIComponent(
      payload.captchaId
    )}&captchaAnswer=${encodeURIComponent(payload.captchaAnswer)}`,
    {
      username: payload.username,
      passwordHash: payload.passwordHash,
      nickname: payload.nickname,
      email: payload.email,
    },
    {
      meta: publicRequestMeta,
    }
  );

export const resetPassword = (payload: ResetPasswordPayload) =>
  request.post("/auth/reset-password", payload, {
    meta: publicRequestMeta,
  });

export const fetchLockStatus = (username: string) =>
  request.get(`/login/lock-status/${encodeURIComponent(username)}`, {
    meta: publicRequestMeta,
  });
