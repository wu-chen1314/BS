// 性能监控工具
interface PerformanceMetrics {
  pageLoadTime: number;
  apiResponseTime: number;
  cacheHitRate: number;
  totalRequests: number;
  cachedRequests: number;
}

class PerformanceMonitor {
  private apiTimings: Map<string, number[]> = new Map();
  private cacheHits: number = 0;
  private cacheMisses: number = 0;
  private startTime: number = Date.now();

  // 记录 API 请求开始
  startApiTiming(apiName: string): void {
    (window as any).__apiStartTime = Date.now();
    (window as any).__apiName = apiName;
  }

  // 记录 API 请求结束
  endApiTiming(apiName?: string): void {
    const endTime = Date.now();
    const startTime = (window as any).__apiStartTime || endTime;
    const name = apiName || (window as any).__apiName || 'unknown';
    const duration = endTime - startTime;

    if (!this.apiTimings.has(name)) {
      this.apiTimings.set(name, []);
    }
    this.apiTimings.get(name)!.push(duration);

    console.log(`API [${name}] 响应时间：${duration}ms`);
  }

  // 记录缓存命中
  recordCacheHit(): void {
    this.cacheHits++;
  }

  // 记录缓存未命中
  recordCacheMiss(): void {
    this.cacheMisses++;
  }

  // 获取缓存命中率
  getCacheHitRate(): number {
    const total = this.cacheHits + this.cacheMisses;
    if (total === 0) return 0;
    return (this.cacheHits / total) * 100;
  }

  // 获取 API 平均响应时间
  getAverageApiTime(apiName?: string): number {
    if (apiName) {
      const timings = this.apiTimings.get(apiName);
      if (!timings || timings.length === 0) return 0;
      return timings.reduce((a, b) => a + b, 0) / timings.length;
    }

    // 所有 API 的平均时间
    let total = 0;
    let count = 0;
    this.apiTimings.forEach(timings => {
      total += timings.reduce((a, b) => a + b, 0);
      count += timings.length;
    });

    return count === 0 ? 0 : total / count;
  }

  // 获取性能报告
  getReport(): PerformanceMetrics {
    const pageLoadTime = Date.now() - this.startTime;
    const apiResponseTime = this.getAverageApiTime();
    const cacheHitRate = this.getCacheHitRate();

    return {
      pageLoadTime,
      apiResponseTime,
      cacheHitRate,
      totalRequests: this.cacheHits + this.cacheMisses,
      cachedRequests: this.cacheHits
    };
  }

  // 打印性能报告
  printReport(): void {
    const report = this.getReport();
    console.group('性能监控报告');
    console.log('页面加载时间:', report.pageLoadTime, 'ms');
    console.log('API 平均响应时间:', report.apiResponseTime.toFixed(2), 'ms');
    console.log('缓存命中率:', report.cacheHitRate.toFixed(2), '%');
    console.log('总请求数:', report.totalRequests);
    console.log('缓存请求数:', report.cachedRequests);
    console.groupEnd();
  }

  // 重置统计数据
  reset(): void {
    this.apiTimings.clear();
    this.cacheHits = 0;
    this.cacheMisses = 0;
    this.startTime = Date.now();
  }
}

export const performanceMonitor = new PerformanceMonitor();
