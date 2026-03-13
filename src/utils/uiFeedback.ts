import { ElMessageBox } from "element-plus";
import errorHandler from "./errorHandler";

type SuccessSubject = string;

interface ConfirmDangerActionOptions {
  action?: string;
  confirmButtonText?: string;
  detail?: string;
  subject: string;
  title?: string;
}

const normalizeSubject = (subject: string) => String(subject || "").trim() || "内容";

export const buildLoadingText = (subject: string, action = "加载") =>
  `正在${String(action || "处理").trim()}${normalizeSubject(subject)}...`;

export const successText = {
  batchDeleted: (subject: SuccessSubject) => `${normalizeSubject(subject)}已批量删除`,
  cleared: (subject: SuccessSubject) => `${normalizeSubject(subject)}已清空`,
  copied: (subject: SuccessSubject) => `${normalizeSubject(subject)}已复制`,
  created: (subject: SuccessSubject) => `${normalizeSubject(subject)}已新增`,
  deleted: (subject: SuccessSubject) => `${normalizeSubject(subject)}已删除`,
  downloaded: (subject: SuccessSubject) => `${normalizeSubject(subject)}已下载`,
  exported: (subject: SuccessSubject) => `${normalizeSubject(subject)}已导出`,
  generated: (subject: SuccessSubject) => `${normalizeSubject(subject)}已生成`,
  published: (subject: SuccessSubject) => `${normalizeSubject(subject)}已发布`,
  refreshed: (subject: SuccessSubject) => `${normalizeSubject(subject)}已刷新`,
  reset: (subject: SuccessSubject) => `${normalizeSubject(subject)}已重置`,
  saved: (subject: SuccessSubject) => `${normalizeSubject(subject)}已保存`,
  sent: (subject: SuccessSubject) => `${normalizeSubject(subject)}已发送`,
  updated: (subject: SuccessSubject) => `${normalizeSubject(subject)}已更新`,
  uploaded: (subject: SuccessSubject) => `${normalizeSubject(subject)}上传成功`,
  login: () => "登录成功",
  logout: () => "已退出登录",
  passwordChanged: () => "密码修改成功，请重新登录",
  passwordReset: () => "密码重置成功，请重新登录",
  register: () => "注册成功，请使用新账号登录",
};

export const selectionRequiredText = (subject: string, action = "操作") =>
  `请先选择需要${String(action || "操作").trim()}的${normalizeSubject(subject)}`;

export const buildConfirmDangerMessage = ({
  action = "删除",
  detail,
  subject,
}: ConfirmDangerActionOptions) =>
  `${`确认${String(action || "删除").trim()}${normalizeSubject(subject)}吗？`}${detail ? detail.trim() : ""}`;

export const confirmDangerAction = ({
  action = "删除",
  confirmButtonText,
  detail,
  subject,
  title = "风险确认",
}: ConfirmDangerActionOptions) =>
  ElMessageBox.confirm(buildConfirmDangerMessage({ action, detail, subject }), title, {
    appendTo: typeof document !== "undefined" ? document.body : undefined,
    autofocus: false,
    cancelButtonText: "取消",
    confirmButtonText: confirmButtonText || `确认${String(action || "删除").trim()}`,
    distinguishCancelAndClose: true,
    lockScroll: false,
    type: "warning",
    customClass: "heritage-confirm-dialog",
  });

export const showSuccess = (message: string) => errorHandler.showSuccess(message);
export const showWarning = (message: string) => errorHandler.showWarning(message);
export const showInfo = (message: string) => errorHandler.showInfo(message);
