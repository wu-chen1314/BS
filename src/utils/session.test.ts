import { afterEach, describe, expect, it } from "vitest";
import {
  clearCurrentSession,
  getCurrentUser,
  getStoredToken,
  isSessionExpired,
  SESSION_CHANGED_EVENT,
  saveCurrentSession,
} from "@/utils/session";

describe("session utils", () => {
  afterEach(() => {
    clearCurrentSession();
  });

  it("restores token from token storage when user payload lacks token", () => {
    window.sessionStorage.setItem(
      "user",
      JSON.stringify({
        id: 1,
        username: "tester",
      })
    );
    window.sessionStorage.setItem("token", "abc-token");

    expect(getStoredToken()).toBe("abc-token");
    expect(getCurrentUser()).toMatchObject({
      id: 1,
      username: "tester",
      token: "abc-token",
    });
  });

  it("clears expired session before returning user", () => {
    saveCurrentSession(
      {
        id: 2,
        username: "expired-user",
      },
      "expired-token",
      Date.now() - 1000
    );

    expect(isSessionExpired()).toBe(true);
    expect(getCurrentUser()).toBeNull();
    expect(window.sessionStorage.getItem("user")).toBeNull();
    expect(window.sessionStorage.getItem("token")).toBeNull();
  });

  it("accepts relative expiration seconds from login response", () => {
    saveCurrentSession(
      {
        id: 3,
        username: "seconds-user",
      },
      "seconds-token",
      86400
    );

    expect(isSessionExpired()).toBe(false);
    expect(getCurrentUser()).toMatchObject({
      id: 3,
      username: "seconds-user",
      token: "seconds-token",
    });
  });

  it("accepts relative expiration milliseconds when provided", () => {
    saveCurrentSession(
      {
        id: 4,
        username: "milliseconds-user",
      },
      "milliseconds-token",
      86400000
    );

    expect(isSessionExpired()).toBe(false);
    expect(getStoredToken()).toBe("milliseconds-token");
  });

  it("emits a session change event when session is saved or cleared", () => {
    const events: string[] = [];
    const handleSessionChanged = () => {
      events.push(SESSION_CHANGED_EVENT);
    };

    window.addEventListener(SESSION_CHANGED_EVENT, handleSessionChanged);

    saveCurrentSession(
      {
        id: 5,
        username: "event-user",
      },
      "event-token",
      86400
    );
    clearCurrentSession();

    window.removeEventListener(SESSION_CHANGED_EVENT, handleSessionChanged);

    expect(events).toHaveLength(2);
  });
});
