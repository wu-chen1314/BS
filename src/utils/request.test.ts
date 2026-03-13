import { describe, expect, it } from "vitest";
import { RequestError, resolveRequestErrorMessage } from "./request";

describe("request helpers", () => {
  it("prefers normalized request errors for business failures", () => {
    const error = new RequestError({
      message: "验证码发送失败",
      code: 500,
      status: 200,
    });

    expect(resolveRequestErrorMessage(error)).toBe("验证码发送失败");
  });

  it("maps network and timeout transport errors to stable user-facing messages", () => {
    const networkError = {
      isAxiosError: true,
      message: "Network Error",
    };
    const timeoutError = {
      isAxiosError: true,
      code: "ECONNABORTED",
      message: "timeout of 10000ms exceeded",
    };

    expect(resolveRequestErrorMessage(networkError)).toBe("网络连接失败，请稍后重试");
    expect(resolveRequestErrorMessage(timeoutError)).toBe("服务响应超时，请稍后重试");
  });
});
