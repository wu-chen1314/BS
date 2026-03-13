import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import HomeProjectDialog from "./HomeProjectDialog.vue";
import { elementStubs } from "@/test-utils/element-stubs";
import type { HeritageProject } from "@/types/project";

const project: HeritageProject = {
  id: 7,
  name: "Shadow Play",
  categoryId: 8,
  categoryName: "Craft",
  regionName: "Sichuan",
  protectLevel: "National",
  status: "Active",
  history:
    '<section><img src="/safe.png" onerror="alert(1)"><a href="javascript:alert(2)">bad</a><strong>kept</strong></section>',
  features: "Traditional performance art",
  inheritorNames: "Sample Inheritor",
  viewCount: 42,
};

describe("HomeProjectDialog", () => {
  it("sanitizes rich project content before rendering", () => {
    const wrapper = mount(HomeProjectDialog, {
      props: {
        visible: true,
        project,
        comments: [],
        commentDraft: "",
        submittingComment: false,
        isAdmin: false,
        currentUserId: 1,
        isFavorited: false,
      },
      global: {
        stubs: elementStubs,
      },
    });

    const html = wrapper.get(".rich-text").html();

    expect(html).toContain('<img src="/safe.png">');
    expect(html).toContain("<strong>kept</strong>");
    expect(html).not.toContain("onerror");
    expect(html).not.toContain("javascript:");
    expect(html).not.toContain("<script");
  });
});
