import { mount } from "@vue/test-utils";
import AuthBrandPanel from "./AuthBrandPanel.vue";

describe("AuthBrandPanel", () => {
  it("renders the mascot stage copy", () => {
    const wrapper = mount(AuthBrandPanel);

    expect(wrapper.text()).toContain("守艺搭子陪你登录");
    expect(wrapper.findAll(".mascot-card")).toHaveLength(3);
    expect(wrapper.find(".status-pill").text()).toContain("准备就绪");
  });

  it("switches to sad mood and updates the stage hint", async () => {
    const wrapper = mount(AuthBrandPanel, {
      props: {
        focusField: "passwordHash",
        mood: "sad",
      },
    });

    expect(wrapper.find(".status-pill").text()).toContain("刚刚好像输错了");
    expect(wrapper.findAll(".mascot-card.mood-sad")).toHaveLength(3);
    expect(wrapper.text()).toContain("失落表情提醒");
  });
});
