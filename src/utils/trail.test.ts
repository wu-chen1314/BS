import { describe, expect, it } from "vitest";
import { buildCustomTrail, buildCustomTrailId, createDefaultTrailPlanner, parseCustomTrailId } from "@/utils/trail";

describe("trail utils", () => {
  it("round trips custom trail ids", () => {
    const planner = {
      ...createDefaultTrailPlanner(),
      interestIds: [8, 7, 2],
      durationKey: "6h" as const,
      budgetLevel: "high" as const,
      transportMode: "car" as const,
      regionKeyword: "fujian",
      maxStops: 5,
      preferHot: false,
    };

    const trailId = buildCustomTrailId(planner);
    expect(parseCustomTrailId(trailId)).toEqual({
      ...planner,
      interestIds: [2, 7, 8],
    });
  });

  it("builds a custom route with practical fields", () => {
    const route = buildCustomTrail(
      [
        {
          id: 1,
          name: "Lacquerware",
          categoryId: 8,
          regionName: "Fujian",
          history: "<p>Historic craft.</p>",
          features: "<p>Detailed process.</p>",
          coverUrl: "/files/a.jpg",
          viewCount: 4200,
          inheritorNames: "Lin",
          address: "Fuzhou lane",
          openingHours: "09:00-17:00",
          longitude: 119.2965,
          latitude: 26.0745,
        },
        {
          id: 2,
          name: "Nanyin",
          categoryId: 2,
          regionName: "Fujian",
          history: "<p>Local music memory.</p>",
          features: "<p>Good for live explanation.</p>",
          viewCount: 3600,
          longitude: 118.0894,
          latitude: 24.4798,
        },
      ],
      {
        1: [{ id: 11, name: "Lin", level: "Provincial inheritor", projectId: 1 }],
      },
      {
        ...createDefaultTrailPlanner(),
        interestIds: [8, 2],
        regionKeyword: "fujian",
      }
    );

    expect(route.stopCount).toBeGreaterThan(0);
    expect(route.estimatedHours).toBeGreaterThan(0);
    expect(route.stops[0].facilityTags.length).toBeGreaterThan(0);
    expect(route.stops[0].historicalContext).toContain("Lin");
  });

  it("prefers coordinate-ready projects when enough navigable stops exist", () => {
    const route = buildCustomTrail(
      [
        {
          id: 1,
          name: "High score without coordinates",
          categoryId: 8,
          regionName: "Fujian",
          history: "<p>Detailed history.</p>",
          features: "<p>Detailed features.</p>",
          coverUrl: "/files/hot.jpg",
          inheritorNames: "Popular inheritor",
          address: "Downtown",
          openingHours: "09:00-18:00",
          viewCount: 9800,
        },
        {
          id: 2,
          name: "Coordinate stop A",
          categoryId: 8,
          regionName: "Fujian",
          viewCount: 500,
          longitude: 119.3062,
          latitude: 26.0753,
        },
        {
          id: 3,
          name: "Coordinate stop B",
          categoryId: 2,
          regionName: "Fujian",
          viewCount: 400,
          longitude: 118.1102,
          latitude: 24.4905,
        },
      ],
      {},
      {
        ...createDefaultTrailPlanner(),
        interestIds: [8, 2],
        regionKeyword: "fujian",
        maxStops: 2,
      }
    );

    expect(route.stops).toHaveLength(2);
    expect(route.stops.map((stop) => stop.project.name)).toEqual(["Coordinate stop A", "Coordinate stop B"]);
    expect(route.stops.every((stop) => stop.project.longitude && stop.project.latitude)).toBe(true);
  });
});
