// 前端请求缓存工具
interface CacheItem<T> {
  data: T;
  timestamp: number;
  key: string;
}

class RequestCache {
  private cache = new Map<string, CacheItem<any>>();
  private defaultTTL = 5 * 60 * 1000; // 5 minutes

  get<T>(key: string, ttl: number = this.defaultTTL): T | null {
    const item = this.cache.get(key);
    if (!item) return null;

    const now = Date.now();
    if (now - item.timestamp > ttl) {
      this.cache.delete(key);
      return null;
    }

    return item.data as T;
  }

  set<T>(key: string, data: T, ttl: number = this.defaultTTL): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      key
    });
  }

  delete(key: string): boolean {
    return this.cache.delete(key);
  }

  clear(): void {
    this.cache.clear();
  }

  getStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    };
  }

  cleanup(ttl: number = this.defaultTTL): number {
    const now = Date.now();
    let count = 0;

    for (const [key, item] of this.cache.entries()) {
      if (now - item.timestamp > ttl) {
        this.cache.delete(key);
        count++;
      }
    }

    return count;
  }
}

export const requestCache = new RequestCache();

export function generateCacheKey(url: string, params?: any): string {
  if (!params) return url;

  const sortedParams = Object.keys(params)
    .sort()
    .map(key => `${key}=${params[key]}`)
    .join('&');

  return `${url}?${sortedParams}`;
}
