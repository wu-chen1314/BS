import { describe, expect, it } from "vitest";
import { evaluateAmapAvailability, resolveAmapErrorMessage } from "./amap";

describe("amap utils", () => {
  it("marks the sdk unavailable when key is missing", () => {
    const availability = evaluateAmapAvailability(
      {
        key: "",
        securityJsCode: "",
        version: "2.0",
      },
      {
        hasWindow: true,
      }
    );

    expect(availability.available).toBe(false);
    expect(availability.status).toBe("missing-key");
    expect(availability.message).toContain("VITE_AMAP_KEY");
  });

  it("marks the sdk ready when browser and key are both present", () => {
    const availability = evaluateAmapAvailability(
      {
        key: "demo-key",
        securityJsCode: "demo-code",
        version: "2.0",
      },
      {
        hasWindow: true,
      }
    );

    expect(availability.available).toBe(true);
    expect(availability.status).toBe("ready");
    expect(availability.message).toBe("");
  });

  it("normalizes unexpected loader errors into a readable message", () => {
    expect(resolveAmapErrorMessage(new Error("SDK init failed"))).toBe("SDK init failed");
    expect(resolveAmapErrorMessage(null)).toContain("高德地图服务初始化失败");
  });
});
