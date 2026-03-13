import { describe, expect, it } from "vitest";
import {
  buildConfirmDangerMessage,
  buildLoadingText,
  selectionRequiredText,
  successText,
} from "./uiFeedback";

describe("uiFeedback", () => {
  it("builds consistent loading text", () => {
    expect(buildLoadingText("项目列表")).toBe("正在加载项目列表...");
    expect(buildLoadingText("题库", "同步")).toBe("正在同步题库...");
  });

  it("builds consistent success text", () => {
    expect(successText.saved("项目")).toBe("项目已保存");
    expect(successText.batchDeleted("会话")).toBe("会话已批量删除");
    expect(successText.passwordReset()).toBe("密码重置成功，请重新登录");
  });

  it("builds consistent warning and confirm text", () => {
    expect(selectionRequiredText("项目", "删除")).toBe("请先选择需要删除的项目");
    expect(
      buildConfirmDangerMessage({
        detail: "删除后阅读入口会立即失效。",
        subject: "这条资讯",
      })
    ).toBe("确认删除这条资讯吗？删除后阅读入口会立即失效。");
  });
});
