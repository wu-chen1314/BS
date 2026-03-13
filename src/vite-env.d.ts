/// <reference types="vite/client" />

import "axios";

interface ImportMetaEnv {
  readonly VITE_API_ORIGIN?: string;
  readonly VITE_WS_ORIGIN?: string;
  readonly VITE_AMAP_KEY?: string;
  readonly VITE_AMAP_SECURITY_CODE?: string;
  readonly VITE_AMAP_VERSION?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module "axios" {
  interface AxiosRequestConfig<D = any> {
    meta?: {
      skipAuth?: boolean;
      skipErrorHandler?: boolean;
      skipSessionRedirect?: boolean;
    };
  }

  interface InternalAxiosRequestConfig<D = any> {
    meta?: {
      skipAuth?: boolean;
      skipErrorHandler?: boolean;
      skipSessionRedirect?: boolean;
    };
  }
}
