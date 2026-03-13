import { beforeEach, describe, expect, it, vi } from "vitest";
import type { TrailStopView } from "@/types/trail";
import { requestCache } from "@/utils/cache";

const amapMocks = vi.hoisted(() => ({
  loadAmapSdk: vi.fn(),
}));

vi.mock("@/utils/amap", () => ({
  loadAmapSdk: amapMocks.loadAmapSdk,
}));

import {
  planApproximateLiveLegNavigation,
  planApproximateTrailNavigation,
  planTrailNavigation,
} from "./amap-navigation";

const stops: TrailStopView[] = [
  {
    project: {
      id: 1,
      name: "Stop One",
      longitude: 120.123456,
      latitude: 30.123456,
    },
    inheritors: [],
    visitDurationHours: 1,
    visitDurationLabel: "1h",
    transferTip: "Walk",
    estimatedSpend: 50,
    facilityTags: [],
    supportTips: [],
    historicalContext: "",
  },
  {
    project: {
      id: 2,
      name: "Stop Two",
      longitude: 121.123456,
      latitude: 31.123456,
    },
    inheritors: [],
    visitDurationHours: 1.5,
    visitDurationLabel: "1.5h",
    transferTip: "Drive",
    estimatedSpend: 80,
    facilityTags: [],
    supportTips: [],
    historicalContext: "",
  },
];

describe("amap navigation service", () => {
  beforeEach(() => {
    requestCache.clear();
    window.sessionStorage.clear();
    amapMocks.loadAmapSdk.mockReset();
  });

  it("builds an approximate route when third-party map services are unavailable", () => {
    const route = planApproximateTrailNavigation(stops, "car");

    expect(route.source).toBe("approximate");
    expect(route.transportMode).toBe("car");
    expect(route.legs).toHaveLength(1);
    expect(route.distance).toBeGreaterThan(0);
    expect(route.legs[0]).toMatchObject({
      source: "approximate",
      fromName: "Stop One",
      toName: "Stop Two",
    });
  });

  it("translates amap driving results into a typed navigation route", async () => {
    class DrivingService {
      search(
        _origin: [number, number],
        _destination: [number, number],
        callback: (status: string, result: unknown) => void
      ) {
        callback("complete", {
          routes: [
            {
              distance: 1500,
              time: 900,
              steps: [
                {
                  instruction: "Head north",
                  distance: 800,
                  time: 480,
                  path: [
                    [120.123456, 30.123456],
                    [120.523456, 30.523456],
                  ],
                },
                {
                  instruction: "Arrive at Stop Two",
                  distance: 700,
                  time: 420,
                  path: [
                    [120.523456, 30.523456],
                    [121.123456, 31.123456],
                  ],
                },
              ],
            },
          ],
        });
      }
    }

    amapMocks.loadAmapSdk.mockResolvedValue({
      Driving: DrivingService,
    });

    const route = await planTrailNavigation(stops, "car");

    expect(route.source).toBe("amap");
    expect(route.distance).toBe(1500);
    expect(route.duration).toBe(900);
    expect(route.legs[0].source).toBe("amap");
    expect(route.legs[0].instructions).toHaveLength(2);
    expect(route.legs[0].instructions[0].text).toBe("Head north");
    expect(route.polyline).toEqual([
      [120.123456, 30.123456],
      [120.523456, 30.523456],
      [121.123456, 31.123456],
    ]);
  });

  it("builds approximate live navigation data for browser geolocation fallback", () => {
    const liveLeg = planApproximateLiveLegNavigation({
      origin: {
        longitude: 120.223456,
        latitude: 30.223456,
      },
      destination: stops[1],
      transportMode: "public",
      fromName: "Current Position",
    });

    expect(liveLeg.source).toBe("approximate");
    expect(liveLeg.toName).toBe("Stop Two");
    expect(liveLeg.distance).toBeGreaterThan(0);
    expect(liveLeg.instructions[0].text).toContain("Stop Two");
  });
});
