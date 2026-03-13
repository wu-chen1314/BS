import type { Router } from "vue-router";
import { clearCurrentSession, type SessionUser } from "./session";
import { confirmDangerAction, showSuccess, successText } from "./uiFeedback";

interface ConfirmManualLogoutOptions {
  user?: SessionUser | null;
}

interface PerformLogoutOptions {
  clearState?: (() => void) | null;
  hardRedirect?: boolean;
  redirectTo?: string;
  router?: Pick<Router, "replace"> | Pick<Router, "push"> | null;
  showSuccessMessage?: boolean;
}

let activeLogoutPromise: Promise<void> | null = null;
const NEXT_AUTH_NOTICE_KEY = "auth:next-notice";

const resolveAccountName = (user?: SessionUser | null) =>
  String(user?.nickname || user?.username || "").trim();

export const buildLogoutSubject = (user?: SessionUser | null) => {
  const accountName = resolveAccountName(user);
  return accountName ? `账号「${accountName}」` : "当前账号";
};

export const buildManualLogoutDetail = (user?: SessionUser | null) => {
  const roleLabel = user?.role === "admin" ? "管理员身份" : "当前身份";
  return `退出后将返回登录页，且需要重新登录才能继续访问平台；当前页面里未提交的内容不会保留。${roleLabel}会一并结束。`;
};

export const isDialogDismissError = (error: unknown) =>
  Boolean(
    error &&
      typeof error === "object" &&
      "action" in error &&
      ((error as { action?: string }).action === "cancel" ||
        (error as { action?: string }).action === "close")
  );

export const confirmManualLogout = ({ user }: ConfirmManualLogoutOptions = {}) =>
  confirmDangerAction({
    action: "退出",
    confirmButtonText: "确认退出",
    detail: buildManualLogoutDetail(user),
    subject: buildLogoutSubject(user),
    title: "退出登录",
  });

export const consumeNextAuthNotice = () => {
  if (typeof window === "undefined") {
    return "";
  }

  const message = window.sessionStorage.getItem(NEXT_AUTH_NOTICE_KEY) || "";
  window.sessionStorage.removeItem(NEXT_AUTH_NOTICE_KEY);
  return message;
};

export const performLogout = ({
  clearState,
  hardRedirect = false,
  redirectTo = "/login",
  router,
  showSuccessMessage = false,
}: PerformLogoutOptions = {}) => {
  if (activeLogoutPromise) {
    return activeLogoutPromise;
  }

  activeLogoutPromise = (async () => {
    const successMessage = showSuccessMessage ? successText.logout() : "";
    if (successMessage && typeof window !== "undefined") {
      window.sessionStorage.setItem(NEXT_AUTH_NOTICE_KEY, successMessage);
    }

    clearCurrentSession({
      emitEvent: false,
    });
    clearState?.();

    if (hardRedirect && typeof window !== "undefined") {
      window.location.replace(redirectTo);
      return;
    }

    if (router) {
      if ("replace" in router) {
        await router.replace(redirectTo);
      } else {
        await router.push(redirectTo);
      }
    }

    if (successMessage) {
      showSuccess(successMessage);
    }
  })().finally(() => {
    activeLogoutPromise = null;
  });

  return activeLogoutPromise;
};
