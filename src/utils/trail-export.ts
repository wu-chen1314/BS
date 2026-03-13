import type { TrailViewModel } from "@/types/trail";
import { stripHtml } from "@/utils/heritage";

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

export const buildTrailItineraryHtml = (route: TrailViewModel) => {
  const stopHtml = route.stops
    .map(
      (stop, index) => `
        <section class="stop">
          <div class="stop-head">
            <div>
              <p class="order">第 ${index + 1} 站</p>
              <h3>${escapeHtml(stop.project.name)}</h3>
              <p class="meta">${escapeHtml(stop.project.regionName || "地区待补充")} · ${escapeHtml(
                stop.project.categoryName || "门类待补充"
              )}</p>
            </div>
            <div class="pillbox">
              <span>${escapeHtml(stop.visitDurationLabel)}</span>
              <span>¥${stop.estimatedSpend}</span>
            </div>
          </div>
          <p>${escapeHtml(stripHtml(stop.project.history || stop.project.features || ""))}</p>
          <p><strong>路线建议：</strong>${escapeHtml(stop.transferTip)}</p>
          <p><strong>配套信息：</strong>${escapeHtml(stop.supportTips.join(" / ") || "现场服务信息待补充")}</p>
          <p><strong>传承人：</strong>${escapeHtml(
            stop.inheritors.map((item) => item.name).join("、") || stop.project.inheritorNames || "待补充"
          )}</p>
        </section>
      `
    )
    .join("");

  const noteHtml = route.notes
    .map((item) => `<li><strong>${escapeHtml(item.title)}：</strong>${escapeHtml(item.content)}</li>`)
    .join("");

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>${escapeHtml(route.title)} 行程单</title>
  <style>
    body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; margin: 0; color: #2b2b2b; background: #f6efe3; }
    .page { max-width: 820px; margin: 0 auto; background: #fffdf9; min-height: 100vh; padding: 36px 42px 48px; }
    .hero { border-bottom: 2px solid #a43b2f; padding-bottom: 18px; margin-bottom: 20px; }
    .kicker { margin: 0 0 8px; font-size: 12px; letter-spacing: 0.22em; color: #c08a3f; }
    h1 { margin: 0 0 10px; font-size: 30px; color: #22313f; }
    .summary { margin: 0; line-height: 1.8; color: #55606a; }
    .metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 18px 0 24px; }
    .metric { padding: 12px 14px; border-radius: 14px; background: #f3e6d4; }
    .metric span { display: block; font-size: 12px; color: #715347; }
    .metric strong { display: block; margin-top: 6px; color: #22313f; }
    h2 { margin: 24px 0 12px; font-size: 20px; color: #22313f; }
    .notes { margin: 0; padding-left: 18px; line-height: 1.8; color: #55606a; }
    .stop { border: 1px solid #e7d8c0; border-radius: 18px; padding: 16px 18px; margin-bottom: 14px; background: #fffaf2; }
    .stop-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
    .order { margin: 0 0 6px; color: #a43b2f; font-size: 12px; }
    h3 { margin: 0 0 6px; font-size: 18px; color: #22313f; }
    .meta { margin: 0; color: #715347; font-size: 13px; }
    .pillbox { display: grid; gap: 8px; min-width: 120px; }
    .pillbox span { display: inline-flex; justify-content: center; padding: 6px 10px; border-radius: 999px; background: #a43b2f; color: #fffdf9; font-size: 12px; }
    p { line-height: 1.8; color: #55606a; }
    @media print {
      body { background: #fff; }
      .page { padding: 0; }
    }
  </style>
</head>
<body>
  <div class="page">
    <section class="hero">
      <p class="kicker">HERITAGE ITINERARY</p>
      <h1>${escapeHtml(route.title)}</h1>
      <p class="summary">${escapeHtml(route.description)}</p>
    </section>
    <section class="metrics">
      <div class="metric"><span>路线模式</span><strong>${escapeHtml(route.routeType === "custom" ? "自定义路线" : "主题模板路线")}</strong></div>
      <div class="metric"><span>建议时长</span><strong>${escapeHtml(route.durationLabel)}</strong></div>
      <div class="metric"><span>交通方式</span><strong>${escapeHtml(route.transportLabel)}</strong></div>
      <div class="metric"><span>预算预估</span><strong>¥${route.estimatedCost}</strong></div>
    </section>
    <section>
      <h2>出行提示</h2>
      <ul class="notes">${noteHtml}</ul>
    </section>
    <section>
      <h2>行程单</h2>
      ${stopHtml}
    </section>
  </div>
</body>
</html>`;
};

export const buildTrailItineraryMarkdown = (route: TrailViewModel) =>
  [
    `# ${route.title}`,
    "",
    route.description,
    "",
    `- 路线模式：${route.routeType === "custom" ? "自定义路线" : "主题模板路线"}`,
    `- 建议时长：${route.durationLabel}`,
    `- 交通方式：${route.transportLabel}`,
    `- 预算预估：¥${route.estimatedCost}`,
    "",
    "## 出行提示",
    ...route.notes.map((item) => `- ${item.title}：${item.content}`),
    "",
    "## 行程单",
    ...route.stops.flatMap((stop, index) => [
      `### 第 ${index + 1} 站 ${stop.project.name}`,
      `- 地区：${stop.project.regionName || "地区待补充"}`,
      `- 门类：${stop.project.categoryName || "门类待补充"}`,
      `- 建议停留：${stop.visitDurationLabel}`,
      `- 单站预算：¥${stop.estimatedSpend}`,
      `- 转场建议：${stop.transferTip}`,
      `- 传承人：${stop.inheritors.map((item) => item.name).join("、") || stop.project.inheritorNames || "待补充"}`,
      `- 配套提示：${stop.supportTips.join(" / ") || "现场服务信息待补充"}`,
      "",
    ]),
  ].join("\r\n");

export const exportTrailItineraryPdf = (route: TrailViewModel) => {
  if (typeof window === "undefined") {
    return;
  }

  const popup = window.open("", "_blank", "width=980,height=840");
  if (!popup) {
    throw new Error("无法打开打印窗口，请检查浏览器弹窗设置");
  }

  popup.document.open();
  popup.document.write(buildTrailItineraryHtml(route));
  popup.document.close();
  popup.focus();
  popup.setTimeout(() => popup.print(), 180);
};

export const downloadTrailItinerary = (route: TrailViewModel) => {
  if (typeof window === "undefined") {
    return;
  }

  const blob = new Blob([buildTrailItineraryMarkdown(route)], { type: "text/markdown;charset=utf-8" });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${route.title.replace(/[\\/:*?"<>|]/g, "-")}-itinerary.md`;
  link.click();
  window.URL.revokeObjectURL(url);
};
