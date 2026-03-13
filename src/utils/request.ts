import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import errorHandler from "./errorHandler";
import { requestCache, generateCacheKey } from "./cache";
import { apiBaseUrl } from "./url";
import { clearCurrentSession, getCurrentUser, getStoredToken, isSessionExpired } from "./session";

interface ApiEnvelope<T = unknown> {
  code?: number;
  data?: T;
  message?: string;
  msg?: string;
}

interface CreateRequestErrorOptions {
  code?: number;
  handled?: boolean;
  message: string;
  originalError?: unknown;
  payload?: unknown;
  status?: number;
}

export class RequestError extends Error {
  public readonly code?: number;
  public readonly handled: boolean;
  public readonly originalError?: unknown;
  public readonly payload?: unknown;
  public readonly status?: number;

  constructor({
    code,
    handled = false,
    message,
    originalError,
    payload,
    status,
  }: CreateRequestErrorOptions) {
    super(message);
    this.name = "RequestError";
    this.code = code;
    this.handled = handled;
    this.originalError = originalError;
    this.payload = payload;
    this.status = status;
  }
}

const pendingRequests = new Set<string>();

const getRequestKey = (config: InternalAxiosRequestConfig) =>
  `${config.method}-${config.url}-${JSON.stringify(config.data || {})}-${JSON.stringify(config.params || {})}`;

const normalizeMessage = (payload?: Partial<ApiEnvelope> | null, fallback = "操作失败") =>
  String(payload?.msg || payload?.message || fallback).trim();

const shouldSkipErrorHandler = (config?: InternalAxiosRequestConfig) =>
  Boolean(config?.meta?.skipErrorHandler);

const shouldSkipSessionRedirect = (config?: InternalAxiosRequestConfig) =>
  Boolean(config?.meta?.skipSessionRedirect);

const createRequestError = (options: CreateRequestErrorOptions) => new RequestError(options);

const buildLoginRedirect = () => {
  if (typeof window === "undefined") {
    return "/login";
  }

  const redirectTarget = `${window.location.pathname}${window.location.search}`;
  return `/login?redirect=${encodeURIComponent(redirectTarget)}`;
};

const toRequestError = (
  error: unknown,
  fallback = "操作失败",
  handled = false
): RequestError => {
  if (error instanceof RequestError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const message = resolveRequestErrorMessage(error, fallback);
    return createRequestError({
      code: error.response?.data?.code,
      handled,
      message,
      originalError: error,
      payload: error.response?.data,
      status: error.response?.status,
    });
  }

  if (error instanceof Error) {
    return createRequestError({
      handled,
      message: error.message || fallback,
      originalError: error,
    });
  }

  return createRequestError({
    handled,
    message: fallback,
    originalError: error,
  });
};

export const resolveRequestErrorMessage = (error: unknown, fallback = "操作失败") => {
  if (error instanceof RequestError) {
    return error.message || fallback;
  }

  if (axios.isAxiosError(error)) {
    if (error.code === "ECONNABORTED") {
      return "服务响应超时，请稍后重试";
    }

    if (error.message === "Network Error") {
      return "网络连接失败，请稍后重试";
    }

    return normalizeMessage(error.response?.data, error.message || fallback);
  }

  if (error instanceof Error) {
    return error.message || fallback;
  }

  return fallback;
};

const service = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json;charset=UTF-8",
  },
});

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const isTrackingRequest = config.url?.includes("/record") || config.url?.includes("/bump");
    if (config.method !== "get" && !isTrackingRequest) {
      const key = getRequestKey(config);
      if (pendingRequests.has(key)) {
        return Promise.reject(new Error("DUPLICATE_REQUEST"));
      }
      pendingRequests.add(key);
    }

    if (isSessionExpired()) {
      clearCurrentSession();
    }

    if (!config.meta?.skipAuth) {
      const currentUser = getCurrentUser();
      const token = currentUser?.token || getStoredToken();
      if (token) {
        config.headers["Authorization"] = `Bearer ${token}`;
      }
    }

    if (config.method === "get" && config.url?.includes("/region-category/")) {
      const cacheKey = generateCacheKey(config.url || "", config.params);
      config.headers["X-Cache-Key"] = cacheKey;
    }

    return config;
  },
  (error: AxiosError) => {
    console.error("Request error:", error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  (response: AxiosResponse<ApiEnvelope>) => {
    const config = response.config as InternalAxiosRequestConfig;
    const isTrackingRequest = config.url?.includes("/record") || config.url?.includes("/bump");
    if (config.method !== "get" && !isTrackingRequest) {
      pendingRequests.delete(getRequestKey(config));
    }

    const res = response.data || {};

    if (res.code === 200) {
      if (config.method === "get" && config.headers["X-Cache-Key"]) {
        const cacheKey = config.headers["X-Cache-Key"] as string;
        requestCache.set(cacheKey, res);
      }
      return response;
    }

    const handled = !shouldSkipErrorHandler(config);
    const requestError = createRequestError({
      code: res.code,
      handled,
      message: normalizeMessage(res, "操作失败"),
      payload: res,
      status: response.status,
    });

    if (handled) {
      errorHandler.showSimpleError(requestError.message);
    }

    return Promise.reject(requestError);
  },
  (error: AxiosError | Error) => {
    if (error.message === "DUPLICATE_REQUEST") {
      console.warn("Canceled duplicate request");
      return Promise.reject(error);
    }

    const axiosError = error as AxiosError<ApiEnvelope>;
    const config = axiosError.config as InternalAxiosRequestConfig & {
      headers: Record<string, string>;
    };
    const isTrackingRequest = config?.url?.includes("/record") || config?.url?.includes("/bump");
    if (config && config.method !== "get" && !isTrackingRequest) {
      pendingRequests.delete(getRequestKey(config));
    }

    const status = axiosError.response?.status;
    const handled = !shouldSkipErrorHandler(config);

    if (status === 401) {
      clearCurrentSession();

      const unauthorizedMessage = normalizeMessage(
        axiosError.response?.data,
        "您的登录已过期，请重新登录"
      );

      if (handled) {
        errorHandler.showWarning(unauthorizedMessage);
      }

      if (
        !shouldSkipSessionRedirect(config) &&
        typeof window !== "undefined" &&
        window.location.pathname !== "/login"
      ) {
        window.location.href = buildLoginRedirect();
      }

      return Promise.reject(
        createRequestError({
          code: axiosError.response?.data?.code,
          handled,
          message: unauthorizedMessage,
          originalError: axiosError,
          payload: axiosError.response?.data,
          status,
        })
      );
    }

    const canUseCachedFallback = !status || status >= 500;
    if (canUseCachedFallback && config && config.method === "get" && config.headers["X-Cache-Key"]) {
      const cacheKey = config.headers["X-Cache-Key"];
      const cachedData = requestCache.get(cacheKey);

      if (cachedData) {
        return Promise.resolve({ data: cachedData } as AxiosResponse);
      }
    }

    if (handled) {
      errorHandler.handleError(axiosError, "Request");
    }

    return Promise.reject(toRequestError(axiosError, "操作失败", handled));
  }
);

export default service;
