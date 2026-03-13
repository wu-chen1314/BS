import { beforeEach, describe, expect, it, vi } from "vitest";

const elementPlusMocks = vi.hoisted(() => ({
  info: vi.fn(),
  notification: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
}));

vi.mock("element-plus", () => ({
  ElMessage: {
    info: elementPlusMocks.info,
    success: elementPlusMocks.success,
    warning: elementPlusMocks.warning,
  },
  ElNotification: elementPlusMocks.notification,
}));

describe("errorHandler", () => {
  const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);

  beforeEach(() => {
    vi.resetModules();
    elementPlusMocks.info.mockReset();
    elementPlusMocks.notification.mockReset();
    elementPlusMocks.success.mockReset();
    elementPlusMocks.warning.mockReset();
    consoleErrorSpy.mockClear();
  });

  it("uses a VNode notification payload instead of unsafe html rendering", async () => {
    const { errorHandler } = await import("./errorHandler");

    errorHandler.handleError(
      {
        isAxiosError: true,
        response: {
          status: 500,
          data: {
            message: '<img src=x onerror="alert(1)">',
          },
        },
      },
      "Load"
    );

    expect(elementPlusMocks.notification).toHaveBeenCalledTimes(1);

    const payload = elementPlusMocks.notification.mock.calls[0]?.[0] as Record<string, unknown>;
    expect(payload.dangerouslyUseHTMLString).toBeUndefined();
    expect(payload.message).toMatchObject({
      __v_isVNode: true,
    });
  });
});
