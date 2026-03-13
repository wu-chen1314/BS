import { describe, expect, it } from "vitest";
import {
  buildTrailRouteCacheKey,
  formatDistance,
  formatDuration,
  hasNavigableCoordinates,
  resolveNextStopIndex,
} from "@/utils/trail-navigation";
import type { TrailStopView } from "@/types/trail";

const stops: TrailStopView[] = [
  {
    project: {
      id: 1,
      name: "福州脱胎漆器髹饰技艺",
      regionName: "福建省",
      longitude: 119.296494,
      latitude: 26.074508,
    },
    inheritors: [],
    visitDurationHours: 1.5,
    visitDurationLabel: "约 1.5 小时",
    transferTip: "适合步行接驳",
    estimatedSpend: 80,
    facilityTags: [],
    supportTips: [],
    historicalContext: "",
  },
  {
    project: {
      id: 2,
      name: "川剧变脸",
      regionName: "四川省",
      longitude: 104.066541,
      latitude: 30.572269,
    },
    inheritors: [],
    visitDurationHours: 2,
    visitDurationLabel: "约 2 小时",
    transferTip: "适合公共交通转场",
    estimatedSpend: 120,
    facilityTags: [],
    supportTips: [],
    historicalContext: "",
  },
];

describe("trail navigation helpers", () => {
  it("detects when stops have complete coordinates", () => {
    expect(hasNavigableCoordinates(stops)).toBe(true);
    expect(
      hasNavigableCoordinates([
        {
          ...stops[0],
          project: {
            ...stops[0].project,
            longitude: null,
          },
        },
      ])
    ).toBe(false);
  });

  it("formats distance and duration into readable labels", () => {
    expect(formatDistance(860)).toBe("860 m");
    expect(formatDistance(1680)).toBe("1.7 km");
    expect(formatDuration(900)).toBe("15 分钟");
    expect(formatDuration(4200)).toBe("1 小时 10 分钟");
  });

  it("builds stable cache keys from route coordinates", () => {
    expect(buildTrailRouteCacheKey(stops, "public")).toContain("trail-route|public|planned|1:119.296494:26.074508");
  });

  it("advances to the next stop after arriving near the current target", () => {
    const nextIndex = resolveNextStopIndex(
      stops,
      {
        longitude: 119.2965,
        latitude: 26.0745,
      },
      0,
      80
    );

    expect(nextIndex).toBe(1);
  });
});
