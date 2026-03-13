import { afterEach, describe, expect, it, vi } from "vitest";
import { getCurrentUser, saveCurrentSession } from "./session";
import {
  buildLogoutSubject,
  buildManualLogoutDetail,
  isDialogDismissError,
  performLogout,
} from "./logout";

describe("logout utils", () => {
  afterEach(() => {
    window.sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it("builds logout copy with the current account name when available", () => {
    expect(
      buildLogoutSubject({
        nickname: "非遗管理员",
        username: "admin",
      })
    ).toBe("账号「非遗管理员」");

    expect(
      buildLogoutSubject({
        username: "admin",
      })
    ).toBe("账号「admin」");

    expect(buildLogoutSubject()).toBe("当前账号");
  });

  it("describes the manual logout impact with the active role", () => {
    expect(
      buildManualLogoutDetail({
        role: "admin",
      })
    ).toContain("管理员身份会一并结束");

    expect(buildManualLogoutDetail()).toContain("当前身份会一并结束");
  });

  it("recognizes cancel and close actions from the confirm dialog", () => {
    expect(isDialogDismissError({ action: "cancel" })).toBe(true);
    expect(isDialogDismissError({ action: "close" })).toBe(true);
    expect(isDialogDismissError({ action: "confirm" })).toBe(false);
    expect(isDialogDismissError(new Error("boom"))).toBe(false);
  });

  it("clears the session and redirects only once for concurrent logout requests", async () => {
    saveCurrentSession(
      {
        id: 1,
        username: "logout-user",
      },
      "logout-token",
      3600
    );

    let resolveNavigation: (() => void) | null = null;
    const replace = vi.fn().mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveNavigation = resolve;
        })
    );

    const firstLogout = performLogout({
      router: { replace },
    });
    const secondLogout = performLogout({
      router: { replace },
    });

    resolveNavigation?.();
    await Promise.all([firstLogout, secondLogout]);

    expect(replace).toHaveBeenCalledTimes(1);
    expect(replace).toHaveBeenCalledWith("/login");
    expect(getCurrentUser()).toBeNull();
  });
});
