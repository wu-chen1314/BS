import {
  filterProjectsByCategories,
  getAverageViewCount,
  getCategoryName,
  getProtectLevelType,
  sortRegionsByValue,
  stripHtml,
  summarizeRichText,
} from "./heritage";

describe("heritage helpers", () => {
  it("returns category labels with fallback", () => {
    expect(getCategoryName(8)).toBe("传统技艺");
    expect(getCategoryName(999)).toBe("其他类别");
  });

  it("maps protect level to tag type", () => {
    expect(getProtectLevelType("国家级")).toBe("danger");
    expect(getProtectLevelType("县级")).toBe("info");
    expect(getProtectLevelType("unknown")).toBe("info");
  });

  it("strips html and summarizes rich text", () => {
    expect(stripHtml("<p>非遗 <strong>项目</strong></p>")).toBe("非遗 项目");
    expect(summarizeRichText("<p>这是一段很长的说明文本，用来验证摘要逻辑是否能够正常截断。</p>", 12)).toBe(
      "这是一段很长的说明文本，..."
    );
    expect(summarizeRichText("", 12)).toBe("暂无内容简介，可进入详情页查看完整内容。");
  });

  it("filters projects by category and sorts region stats", () => {
    const projects = [
      { id: 1, categoryId: 8, viewCount: 20 },
      { id: 2, categoryId: 2, viewCount: 10 },
      { id: 3, categoryId: 8, viewCount: 30 },
    ];
    const filtered = filterProjectsByCategories(projects, [8]);
    expect(filtered.map((item) => item.id)).toEqual([1, 3]);
    expect(getAverageViewCount(filtered)).toBe(25);

    const regions = sortRegionsByValue([
      { name: "A", value: 2 },
      { name: "B", value: 8 },
      { name: "C", value: 5 },
    ]);
    expect(regions.map((item) => item.name)).toEqual(["B", "C", "A"]);
  });
});
