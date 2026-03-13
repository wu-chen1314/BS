import { mount } from "@vue/test-utils";
import HomeProjectCard from "./HomeProjectCard.vue";
import { elementStubs } from "@/test-utils/element-stubs";

const project = {
  id: 12,
  name: "苏绣",
  categoryId: 8,
  categoryName: "传统技艺",
  regionName: "江苏",
  protectLevel: "国家级",
  status: "在传承",
  history: "<p>以精细针法见长的传统刺绣技艺。</p>",
  inheritorNames: "示例传承人",
  viewCount: 88,
};

describe("HomeProjectCard", () => {
  it("renders project summary and emits open", async () => {
    const wrapper = mount(HomeProjectCard, {
      props: {
        project,
        isAdmin: false,
        isFavorited: false,
        selected: false,
      },
      global: {
        stubs: elementStubs,
      },
    });

    expect(wrapper.text()).toContain("苏绣");
    expect(wrapper.text()).toContain("传统技艺");
    expect(wrapper.text()).toContain("示例传承人");

    await wrapper.get(".project-card").trigger("click");
    expect(wrapper.emitted("open")?.[0]).toEqual([project]);
  });

  it("emits favorite toggle for normal users", async () => {
    const wrapper = mount(HomeProjectCard, {
      props: {
        project,
        isAdmin: false,
        isFavorited: true,
        selected: false,
      },
      global: {
        stubs: elementStubs,
      },
    });

    await wrapper.get(".favorite-button").trigger("click");
    expect(wrapper.emitted("toggle-favorite")?.[0]).toEqual([12]);
  });
});
