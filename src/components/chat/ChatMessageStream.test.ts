import { mount } from "@vue/test-utils";
import ChatMessageStream from "./ChatMessageStream.vue";
import { elementStubs } from "@/test-utils/element-stubs";

describe("ChatMessageStream", () => {
  it("escapes unsafe html while preserving markdown rendering", () => {
    const wrapper = mount(ChatMessageStream, {
      props: {
        messages: [
          {
            id: 1,
            role: "assistant",
            content: `<script>alert("xss")</script>\n\n**非遗**内容`,
            timestamp: "2026-03-11T08:00:00.000Z",
          },
        ],
        aiAvatar: "",
        userAvatar: "",
        suggestions: [],
      },
      global: {
        stubs: elementStubs,
      },
    });

    const html = wrapper.find(".message-text").html();
    expect(html).toContain("&lt;script&gt;alert");
    expect(html).toContain("<strong>非遗</strong>");
  });
});
