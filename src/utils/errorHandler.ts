import axios, { AxiosError } from "axios";
import { ElMessage, ElNotification } from "element-plus";
import { h } from "vue";

interface ErrorResponse {
  code?: number;
  data?: unknown;
  message?: string;
  msg?: string;
}

interface ErrorDetail {
  title: string;
  message: string;
  suggestion: string;
  type: "error" | "warning" | "info";
}

const normalizeErrorMessage = (data?: ErrorResponse | null, fallback = "操作失败") =>
  String(data?.msg || data?.message || fallback).trim();

class GlobalErrorHandler {
  private static instance: GlobalErrorHandler;
  private readonly recentUiEvents = new Map<string, number>();
  private readonly dedupeWindowMs = 1800;

  private constructor() {}

  public static getInstance(): GlobalErrorHandler {
    if (!GlobalErrorHandler.instance) {
      GlobalErrorHandler.instance = new GlobalErrorHandler();
    }
    return GlobalErrorHandler.instance;
  }

  public handleError(error: unknown, context = "操作"): void {
    console.error(`[${context}] error:`, error);

    if (axios.isAxiosError(error)) {
      this.handleAxiosError(error, context);
      return;
    }

    if (error instanceof Error) {
      this.handleSystemError(error, context);
      return;
    }

    this.handleUnknownError(context);
  }

  private handleAxiosError(error: AxiosError<ErrorResponse>, context: string): void {
    if (error.response) {
      this.handleResponseError(error.response.status, error.response.data, context);
      return;
    }

    if (error.request) {
      this.handleNetworkError(context);
      return;
    }

    this.handleRequestSetupError(context);
  }

  private handleResponseError(status: number, data: ErrorResponse | undefined, context: string): void {
    this.showErrorNotification(this.getErrorDetailByStatus(status, data, context));
  }

  private getErrorDetailByStatus(status: number, data: ErrorResponse | undefined, context: string): ErrorDetail {
    switch (status) {
      case 400:
        return {
          title: `${context}失败`,
          message: normalizeErrorMessage(data, "请求参数错误"),
          suggestion: "请检查输入内容后重试。",
          type: "error",
        };
      case 401:
        return {
          title: "认证失败",
          message: normalizeErrorMessage(data, "用户名或密码错误"),
          suggestion: "请重新登录，或确认账号密码是否正确。",
          type: "error",
        };
      case 403:
        return {
          title: "权限不足",
          message: normalizeErrorMessage(data, "当前账号无权执行该操作"),
          suggestion: "请联系管理员确认权限配置。",
          type: "warning",
        };
      case 404:
        return {
          title: "资源不存在",
          message: normalizeErrorMessage(data, "请求的资源不存在"),
          suggestion: "请检查访问路径或刷新页面后重试。",
          type: "error",
        };
      case 429:
        return {
          title: "请求过于频繁",
          message: normalizeErrorMessage(data, "操作过于频繁，请稍后再试"),
          suggestion: "请稍等片刻后再次提交。",
          type: "warning",
        };
      case 500:
        return {
          title: "服务异常",
          message: normalizeErrorMessage(data, "服务器内部错误"),
          suggestion: "请稍后重试；如果问题持续存在，请联系技术支持。",
          type: "error",
        };
      case 502:
      case 503:
      case 504:
        return {
          title: "服务暂不可用",
          message: normalizeErrorMessage(data, "服务器当前无法响应请求"),
          suggestion: "请检查网络连接，稍后再试。",
          type: "error",
        };
      default:
        return {
          title: `${context}失败`,
          message: normalizeErrorMessage(data, `未知错误（状态码：${status}）`),
          suggestion: "请稍后重试；如果问题持续存在，请联系技术支持。",
          type: "error",
        };
    }
  }

  private handleNetworkError(_context: string): void {
    this.showErrorNotification({
      title: "网络连接失败",
      message: "无法连接到服务器。",
      suggestion: "请检查本机网络、接口地址或后端服务是否启动。",
      type: "error",
    });
  }

  private handleRequestSetupError(context: string): void {
    this.showErrorNotification({
      title: `${context}失败`,
      message: "请求配置异常。",
      suggestion: "请检查请求参数和当前页面状态后重试。",
      type: "error",
    });
  }

  private handleSystemError(error: Error, context: string): void {
    this.showErrorNotification({
      title: `${context}失败`,
      message: error.message || "系统异常",
      suggestion: "请刷新页面后重试；如果问题持续存在，请联系技术支持。",
      type: "error",
    });
  }

  private handleUnknownError(context: string): void {
    this.showErrorNotification({
      title: `${context}失败`,
      message: "未知错误",
      suggestion: "请刷新页面后重试；如果问题持续存在，请联系技术支持。",
      type: "error",
    });
  }

  private buildNotificationMessage(detail: ErrorDetail) {
    return h("div", { style: "line-height: 1.8;" }, [
      h("div", { style: "margin-bottom: 8px; font-size: 14px;" }, detail.message),
      h("div", { style: "color: #909399; font-size: 12px;" }, [
        h("strong", "建议："),
        h("span", detail.suggestion),
      ]),
    ]);
  }

  private shouldSuppressUiEvent(key: string) {
    const now = Date.now();
    for (const [storedKey, timestamp] of this.recentUiEvents.entries()) {
      if (now - timestamp > this.dedupeWindowMs) {
        this.recentUiEvents.delete(storedKey);
      }
    }

    const previousTimestamp = this.recentUiEvents.get(key);
    if (previousTimestamp && now - previousTimestamp < this.dedupeWindowMs) {
      return true;
    }

    this.recentUiEvents.set(key, now);
    return false;
  }

  private showMessage(type: "success" | "warning" | "info", message: string) {
    const normalizedMessage = String(message || "").trim();
    if (!normalizedMessage) {
      return;
    }

    const dedupeKey = `message:${type}:${normalizedMessage}`;
    if (this.shouldSuppressUiEvent(dedupeKey)) {
      return;
    }

    ElMessage({
      type,
      message: normalizedMessage,
    });
  }

  private showErrorNotification(detail: ErrorDetail): void {
    const dedupeKey = `notification:${detail.type}:${detail.title}:${detail.message}`;
    if (this.shouldSuppressUiEvent(dedupeKey)) {
      return;
    }

    ElNotification({
      duration: 5000,
      message: this.buildNotificationMessage(detail),
      showClose: true,
      title: detail.title,
      type: detail.type,
    });
  }

  public showSimpleError(message: string, suggestion = ""): void {
    this.showErrorNotification({
      title: "操作失败",
      message: String(message || "操作失败"),
      suggestion: suggestion || "请稍后重试。",
      type: "error",
    });
  }

  public showSuccess(message: string): void {
    this.showMessage("success", message);
  }

  public showWarning(message: string): void {
    this.showMessage("warning", message);
  }

  public showInfo(message: string): void {
    this.showMessage("info", message);
  }
}

export const errorHandler = GlobalErrorHandler.getInstance();

export default errorHandler;
