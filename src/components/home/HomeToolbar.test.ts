import { mount } from "@vue/test-utils";
import HomeToolbar from "./HomeToolbar.vue";
import { elementStubs } from "@/test-utils/element-stubs";

describe("HomeToolbar", () => {
  it("renders admin actions and emits toolbar events", async () => {
    const wrapper = mount(HomeToolbar, {
      props: {
        keyword: "刺绣",
        protectLevel: "国家级",
        history: ["刺绣", "戏曲"],
        showHistory: true,
        isAdmin: true,
        selectedCount: 2,
        protectLevels: [
          { value: "国家级", label: "国家级" },
          { value: "省级", label: "省级" },
        ],
      },
      global: {
        stubs: elementStubs,
      },
    });

    expect(wrapper.text()).toContain("新增项目");
    expect(wrapper.text()).toContain("搜索历史");

    await wrapper.get(".history-item").trigger("mousedown");
    expect(wrapper.emitted("select-history")?.[0]).toEqual(["刺绣"]);

    const actionButtons = wrapper.findAll("button");
    await actionButtons[actionButtons.length - 1].trigger("click");
    expect(wrapper.emitted("export")).toHaveLength(1);
  });
});
