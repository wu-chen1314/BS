import { mount } from "@vue/test-utils";
import ChatComposer from "./ChatComposer.vue";
import { elementStubs } from "@/test-utils/element-stubs";

describe("ChatComposer", () => {
  it("emits quick question and submit actions", async () => {
    const wrapper = mount(ChatComposer, {
      props: {
        modelValue: "测试问题",
        disabled: false,
        loading: false,
        quickQuestions: [
          { id: 1, emoji: "📚", question: "什么是非遗？" },
          { id: 2, emoji: "🗺️", question: "如何规划路线？" },
        ],
      },
      global: {
        stubs: elementStubs,
      },
    });

    await wrapper.get(".question-chip").trigger("click");
    expect(wrapper.emitted("select-suggestion")?.[0]).toEqual(["什么是非遗？"]);

    const buttons = wrapper.findAll("button");
    await buttons[buttons.length - 1].trigger("click");
    expect(wrapper.emitted("submit")).toHaveLength(1);
  });
});
