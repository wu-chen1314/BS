import { describe, expect, it } from "vitest";
import { buildTrailMap } from "@/utils/trail-map";
import type { TrailStopView } from "@/types/trail";

const sampleStops: TrailStopView[] = [
  {
    project: {
      id: 1,
      name: "福州脱胎漆器髹饰技艺",
      regionName: "福建省",
      categoryName: "传统技艺",
    },
    inheritors: [],
    visitDurationHours: 1.6,
    visitDurationLabel: "约 1.5 小时",
    transferTip: "适合步行接驳",
    estimatedSpend: 80,
    facilityTags: ["讲解点"],
    supportTips: ["建议提前预约"],
    historicalContext: "适合安排工艺观摩。",
  },
  {
    project: {
      id: 2,
      name: "川剧变脸",
      regionName: "四川省",
      categoryName: "传统戏剧",
    },
    inheritors: [],
    visitDurationHours: 2,
    visitDurationLabel: "约 2 小时",
    transferTip: "适合公共交通转场",
    estimatedSpend: 120,
    facilityTags: ["剧场"],
    supportTips: ["建议关注演出时段"],
    historicalContext: "适合安排表演体验。",
  },
];

describe("trail map", () => {
  it("builds anchored nodes and segments from stop regions", () => {
    const map = buildTrailMap(sampleStops);

    expect(map.mode).toBe("schematic");
    expect(map.nodes).toHaveLength(2);
    expect(map.nodes[0]).toMatchObject({
      id: 1,
      regionName: "福建省",
      durationLabel: "约 1.5 小时",
      x: 85,
      y: 58,
    });
    expect(map.nodes[1]).toMatchObject({
      id: 2,
      regionName: "四川省",
      x: 49,
      y: 46,
    });
    expect(map.segments).toEqual([
      {
        fromX: 85,
        fromY: 58,
        toX: 49,
        toY: 46,
      },
    ]);
  });

  it("switches to geographic mode when all stops have coordinates", () => {
    const map = buildTrailMap(
      sampleStops.map((stop, index) => ({
        ...stop,
        project: {
          ...stop.project,
          longitude: index === 0 ? 119.296494 : 104.066541,
          latitude: index === 0 ? 26.074508 : 30.572269,
        },
      }))
    );

    expect(map.mode).toBe("geographic");
    expect(map.nodes[0].x).toBeGreaterThan(map.nodes[1].x);
    expect(map.nodes[0].y).toBeGreaterThan(map.nodes[1].y);
    expect(map.nodes[0]).toMatchObject({
      longitude: 119.296494,
      latitude: 26.074508,
    });
  });
});
