import { describe, expect, it } from "vitest";
import type { TrailViewModel } from "@/types/trail";
import { buildTrailItineraryHtml, buildTrailItineraryMarkdown } from "@/utils/trail-export";

const sampleRoute: TrailViewModel = {
  id: "custom~day~medium~public~8~%E7%A6%8F%E5%BB%BA~3~1",
  routeType: "custom",
  title: "闽都匠作非遗路线",
  subtitle: "围绕传统工艺与表演展开",
  description: "聚焦福建地区的传统技艺与展演体验。",
  durationLabel: "一日研学",
  transportLabel: "公共交通",
  budgetLabel: "适中",
  estimatedHours: 6,
  estimatedCost: 260,
  stopCount: 2,
  keywords: ["传统技艺", "福建"],
  notes: [
    {
      title: "交通提醒",
      content: "建议上午先走工艺点位，下午再安排剧场或展馆。",
    },
  ],
  stops: [
    {
      project: {
        id: 1,
        name: "福州脱胎漆器髹饰技艺",
        regionName: "福建省",
        categoryName: "传统技艺",
        history: "<p>历史悠久，工序繁复。</p>",
        inheritorNames: "林某",
      },
      inheritors: [{ id: 10, name: "林某", level: "省级代表性传承人" }],
      visitDurationHours: 1.6,
      visitDurationLabel: "约 1.5 小时",
      transferTip: "适合步行接驳",
      estimatedSpend: 80,
      facilityTags: ["讲解点"],
      supportTips: ["建议提前预约", "现场可体验工艺展示"],
      historicalContext: "适合安排工艺观摩。",
    },
    {
      project: {
        id: 2,
        name: "南音",
        regionName: "福建省",
        categoryName: "传统音乐",
        features: "<p>适合听赏与讲解。</p>",
        inheritorNames: "陈某",
      },
      inheritors: [],
      visitDurationHours: 2,
      visitDurationLabel: "约 2 小时",
      transferTip: "适合公共交通转场",
      estimatedSpend: 120,
      facilityTags: ["演艺空间"],
      supportTips: ["建议关注演出排期"],
      historicalContext: "适合安排听赏体验。",
    },
  ],
};

describe("trail export", () => {
  it("builds printable itinerary html with route details", () => {
    const html = buildTrailItineraryHtml(sampleRoute);

    expect(html).toContain("HERITAGE ITINERARY");
    expect(html).toContain("闽都匠作非遗路线");
    expect(html).toContain("福州脱胎漆器髹饰技艺");
    expect(html).toContain("南音");
    expect(html).toContain("行程单");
  });

  it("builds markdown itinerary with notes and stops", () => {
    const markdown = buildTrailItineraryMarkdown(sampleRoute);

    expect(markdown).toContain("# 闽都匠作非遗路线");
    expect(markdown).toContain("## 出行提示");
    expect(markdown).toContain("### 第 1 站 福州脱胎漆器髹饰技艺");
    expect(markdown).toContain("### 第 2 站 南音");
    expect(markdown).toContain("建议关注演出排期");
  });
});
