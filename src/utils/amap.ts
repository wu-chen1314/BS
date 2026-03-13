import AMapLoader from "@amap/amap-jsapi-loader";
import type { AmapAvailability, AmapConfig, AmapStaticApi } from "@/types/amap";

declare global {
  interface Window {
    _AMapSecurityConfig?: {
      securityJsCode?: string;
    };
  }
}

const DEFAULT_AMAP_VERSION = "2.0";

let amapPromise: Promise<AmapStaticApi> | null = null;
const loadedPlugins = new Set<string>();

const normalizePlugins = (plugins: string[]) => Array.from(new Set(plugins.filter(Boolean)));

export const getAmapConfig = (): AmapConfig => ({
  key: (import.meta.env.VITE_AMAP_KEY || "").trim(),
  securityJsCode: (import.meta.env.VITE_AMAP_SECURITY_CODE || "").trim(),
  version: (import.meta.env.VITE_AMAP_VERSION || DEFAULT_AMAP_VERSION).trim() || DEFAULT_AMAP_VERSION,
});

export const evaluateAmapAvailability = (
  config: AmapConfig,
  runtime: {
    hasWindow: boolean;
  } = {
    hasWindow: typeof window !== "undefined",
  }
): AmapAvailability => {
  if (!runtime.hasWindow) {
    return {
      available: false,
      config,
      message: "当前环境不支持加载高德地图 SDK",
      status: "missing-window",
    };
  }

  if (!config.key) {
    return {
      available: false,
      config,
      message: "高德地图未配置，请先设置 VITE_AMAP_KEY",
      status: "missing-key",
    };
  }

  return {
    available: true,
    config,
    message: "",
    status: "ready",
  };
};

export const getAmapAvailability = () => evaluateAmapAvailability(getAmapConfig());

export const isAmapConfigured = () => getAmapAvailability().available;

export const resolveAmapErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return "高德地图服务初始化失败，请检查网络、Key 或安全密钥配置";
};

const ensurePluginList = async (AMap: AmapStaticApi, plugins: string[]) => {
  const missing = normalizePlugins(plugins).filter((plugin) => !loadedPlugins.has(plugin));
  if (!missing.length) {
    return;
  }

  await new Promise<void>((resolve) => {
    AMap.plugin(missing, () => {
      missing.forEach((plugin) => loadedPlugins.add(plugin));
      resolve();
    });
  });
};

export const loadAmapSdk = async (plugins: string[] = []) => {
  const availability = getAmapAvailability();
  if (!availability.available) {
    throw new Error(availability.message);
  }
  const config = availability.config;

  if (config.securityJsCode && typeof window !== "undefined") {
    window._AMapSecurityConfig = {
      securityJsCode: config.securityJsCode,
    };
  }

  if (!amapPromise) {
    amapPromise = (AMapLoader.load({
      key: config.key,
      version: config.version,
    }) as Promise<AmapStaticApi>).catch((error: unknown) => {
      amapPromise = null;
      throw error;
    });
  }

  const AMap = await amapPromise;
  await ensurePluginList(AMap, plugins);
  return AMap;
};
