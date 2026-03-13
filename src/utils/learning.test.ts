import { describe, expect, it } from "vitest";
import { LEARNING_TRACKS } from "@/constants/heritage";
import { buildLearningRouteQuery, createLearningPlan, parseLearningPlanId } from "@/utils/learning";
import { buildLearningPlanMarkdown, buildTeacherSheetMarkdown } from "@/utils/learning-export";

const sampleProjects = [
  {
    id: 11,
    name: "昆曲",
    categoryId: 4,
    categoryName: "传统戏剧",
    regionName: "江苏省",
    history: "昆曲是中国古老戏曲艺术之一。",
    inheritorNames: "张老师",
  },
  {
    id: 12,
    name: "评弹",
    categoryId: 5,
    categoryName: "曲艺",
    regionName: "江苏省",
    history: "评弹兼具说唱表演特色。",
    inheritorNames: "李老师",
  },
] as const;

describe("learning utilities", () => {
  it("creates a stable learning plan with teacher sheet", () => {
    const track = LEARNING_TRACKS[0];
    const plan = createLearningPlan(track, [...sampleProjects], [{ name: "江苏省", value: 2 }]);

    expect(plan.trackId).toBe(track.id);
    expect(plan.projects).toHaveLength(2);
    expect(plan.modules).toHaveLength(4);
    expect(plan.teacherSheet.agenda).toHaveLength(4);

    const parsed = parseLearningPlanId(plan.id);
    expect(parsed?.trackId).toBe(track.id);
    expect(parsed?.regionName).toBe("江苏省");
  });

  it("builds route query and export content from current plan", () => {
    const track = LEARNING_TRACKS[1];
    const plan = createLearningPlan(track, [...sampleProjects], [{ name: "江苏省", value: 2 }]);
    const query = buildLearningRouteQuery(track, plan.highlightedRegion);
    const markdown = buildLearningPlanMarkdown(plan);
    const teacherMarkdown = buildTeacherSheetMarkdown(plan.teacherSheet);

    expect(query.mode).toBe("custom");
    expect(query.track).toBe(track.id);
    expect(markdown).toContain(plan.title);
    expect(teacherMarkdown).toContain(plan.teacherSheet.title);
  });
});
