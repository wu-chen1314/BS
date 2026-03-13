import { mount } from "@vue/test-utils";
import ChatSidebar from "./ChatSidebar.vue";
import { elementStubs } from "@/test-utils/element-stubs";

const sessions = [
  { id: 1, title: "非遗总览", preview: "继续查看总览", updatedLabel: "03/08" },
  { id: 2, title: "技艺路线", preview: "继续查看路线", updatedLabel: "03/07" },
];

describe("ChatSidebar", () => {
  it("emits create and select in normal mode", async () => {
    const wrapper = mount(ChatSidebar, {
      props: {
        sessions,
        activeId: 1,
        isBatchMode: false,
        selectedIds: [],
      },
      global: {
        stubs: elementStubs,
      },
    });

    await wrapper.get(".chat-item").trigger("click");
    expect(wrapper.emitted("select-chat")?.[0]).toEqual([1]);

    const headerButtons = wrapper.findAll(".sidebar-header button");
    await headerButtons[headerButtons.length - 1].trigger("click");
    expect(wrapper.emitted("create-chat")).toHaveLength(1);
  });

  it("toggles selection in batch mode", async () => {
    const wrapper = mount(ChatSidebar, {
      props: {
        sessions,
        activeId: null,
        isBatchMode: true,
        selectedIds: [2],
      },
      global: {
        stubs: elementStubs,
      },
    });

    await wrapper.find(".chat-item").trigger("click");
    expect(wrapper.emitted("toggle-select")?.[0]).toEqual([1]);
  });
});
