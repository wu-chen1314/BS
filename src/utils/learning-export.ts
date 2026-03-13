import type { LearningPlanViewModel, LearningTeacherSheet } from "@/types/learning";
import { stripHtml } from "@/utils/heritage";

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

const sanitizeFilename = (value: string) => value.replace(/[\\/:*?"<>|]/g, "-");

export const buildLearningPlanHtml = (plan: LearningPlanViewModel) => {
  const modulesHtml = plan.modules
    .map(
      (module, index) => `
        <section class="module">
          <p class="order">模块 ${index + 1}</p>
          <h3>${escapeHtml(module.title)}</h3>
          <p><strong>建议时长：</strong>${escapeHtml(module.duration)}</p>
          <p><strong>目标：</strong>${escapeHtml(module.objective)}</p>
          <p><strong>活动：</strong>${escapeHtml(module.activities.join(" / "))}</p>
          <p><strong>产出：</strong>${escapeHtml(module.outputs.join(" / "))}</p>
        </section>
      `
    )
    .join("");

  const projectHtml = plan.projects
    .slice(0, 4)
    .map(
      (project) => `
        <li>
          <strong>${escapeHtml(project.name)}</strong> - ${escapeHtml(project.regionName || "地区待补充")} - ${escapeHtml(
            stripHtml(project.history || project.features || "")
          )}
        </li>
      `
    )
    .join("");

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>${escapeHtml(plan.title)}</title>
  <style>
    body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; margin: 0; color: #243246; background: #f6efe3; }
    .page { max-width: 860px; margin: 0 auto; min-height: 100vh; background: #fffdf9; padding: 36px 44px 52px; }
    .hero { padding-bottom: 18px; border-bottom: 2px solid #a43b2f; }
    .kicker { margin: 0 0 10px; font-size: 12px; letter-spacing: 0.22em; color: #c08a3f; }
    h1, h2, h3, p { margin: 0; }
    h1 { font-size: 30px; margin-bottom: 10px; }
    .desc, li { line-height: 1.8; color: #55606a; }
    .metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 22px 0; }
    .metric, .module { border-radius: 16px; padding: 14px 16px; background: #fff7ef; border: 1px solid #ead9be; }
    .metric span, .order { display: block; font-size: 12px; color: #8f6a53; }
    .metric strong { display: block; margin-top: 6px; }
    .module { margin-bottom: 14px; }
    h2 { margin: 22px 0 12px; font-size: 20px; }
    ul { margin: 0; padding-left: 18px; }
    @media print {
      body { background: #fff; }
      .page { padding: 0; }
    }
  </style>
</head>
<body>
  <div class="page">
    <section class="hero">
      <p class="kicker">LEARNING STUDIO PLAN</p>
      <h1>${escapeHtml(plan.title)}</h1>
      <p class="desc">${escapeHtml(plan.goal)}</p>
    </section>
    <section class="metrics">
      <div class="metric"><span>适用对象</span><strong>${escapeHtml(plan.audience)}</strong></div>
      <div class="metric"><span>建议时长</span><strong>${escapeHtml(plan.duration)}</strong></div>
      <div class="metric"><span>联动主题</span><strong>${escapeHtml(plan.linkedThemeTitle)}</strong></div>
      <div class="metric"><span>重点地区</span><strong>${escapeHtml(plan.highlightedRegion)}</strong></div>
    </section>
    <section>
      <h2>模块设计</h2>
      ${modulesHtml}
    </section>
    <section>
      <h2>推荐项目</h2>
      <ul>${projectHtml}</ul>
    </section>
  </div>
</body>
</html>`;
};

export const buildTeacherSheetHtml = (sheet: LearningTeacherSheet) => {
  const agendaHtml = sheet.agenda
    .map(
      (item, index) => `
        <section class="agenda-item">
          <p class="order">环节 ${index + 1}</p>
          <h3>${escapeHtml(item.title)}</h3>
          <p><strong>时长：</strong>${escapeHtml(item.duration)}</p>
          <p><strong>目标：</strong>${escapeHtml(item.objective)}</p>
          <p><strong>教师任务：</strong>${escapeHtml(item.teacherTasks.join(" / "))}</p>
          <p><strong>学生任务：</strong>${escapeHtml(item.studentTasks.join(" / "))}</p>
        </section>
      `
    )
    .join("");

  const renderList = (items: string[]) => items.map((item) => `<li>${escapeHtml(item)}</li>`).join("");

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>${escapeHtml(sheet.title)}</title>
  <style>
    body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; margin: 0; color: #243246; background: #f3eee6; }
    .page { max-width: 860px; margin: 0 auto; min-height: 100vh; background: #fffdf9; padding: 36px 44px 52px; }
    .hero { padding-bottom: 18px; border-bottom: 2px solid #a43b2f; margin-bottom: 22px; }
    .kicker { margin: 0 0 10px; font-size: 12px; letter-spacing: 0.22em; color: #c08a3f; }
    h1, h2, h3, p { margin: 0; }
    h1 { font-size: 30px; margin-bottom: 10px; }
    .summary, li, .agenda-item p { line-height: 1.8; color: #55606a; }
    .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .card, .agenda-item { border-radius: 16px; padding: 16px; background: #fff7ef; border: 1px solid #ead9be; }
    h2 { margin: 22px 0 12px; font-size: 20px; }
    ul { margin: 0; padding-left: 18px; }
    .agenda-item { margin-bottom: 14px; }
    .order { display: block; font-size: 12px; color: #8f6a53; margin-bottom: 8px; }
    @media print {
      body { background: #fff; }
      .page { padding: 0; }
    }
  </style>
</head>
<body>
  <div class="page">
    <section class="hero">
      <p class="kicker">TEACHER ACTIVITY SHEET</p>
      <h1>${escapeHtml(sheet.title)}</h1>
      <p class="summary">${escapeHtml(sheet.summary)}</p>
    </section>
    <section class="grid">
      <div class="card"><h2>活动对象</h2><p>${escapeHtml(sheet.targetAudience)}</p></div>
      <div class="card"><h2>建议时长</h2><p>${escapeHtml(sheet.suggestedDuration)}</p></div>
      <div class="card"><h2>准备清单</h2><ul>${renderList(sheet.prepChecklist)}</ul></div>
      <div class="card"><h2>教学材料</h2><ul>${renderList(sheet.materials)}</ul></div>
    </section>
    <section>
      <h2>活动流程</h2>
      ${agendaHtml}
    </section>
    <section class="grid">
      <div class="card"><h2>讨论提示</h2><ul>${renderList(sheet.discussionPrompts)}</ul></div>
      <div class="card"><h2>评价要点</h2><ul>${renderList(sheet.assessmentPoints)}</ul></div>
    </section>
  </div>
</body>
</html>`;
};

export const buildLearningPlanMarkdown = (plan: LearningPlanViewModel) =>
  [
    `# ${plan.title}`,
    "",
    plan.goal,
    "",
    `- 适用对象：${plan.audience}`,
    `- 建议时长：${plan.duration}`,
    `- 联动主题：${plan.linkedThemeTitle}`,
    `- 重点地区：${plan.highlightedRegion}`,
    "",
    "## 模块设计",
    ...plan.modules.flatMap((module, index) => [
      `### 模块 ${index + 1} ${module.title}`,
      `- 时长：${module.duration}`,
      `- 目标：${module.objective}`,
      `- 活动：${module.activities.join(" / ")}`,
      `- 产出：${module.outputs.join(" / ")}`,
      "",
    ]),
  ].join("\r\n");

export const buildTeacherSheetMarkdown = (sheet: LearningTeacherSheet) =>
  [
    `# ${sheet.title}`,
    "",
    sheet.summary,
    "",
    `- 活动对象：${sheet.targetAudience}`,
    `- 建议时长：${sheet.suggestedDuration}`,
    "",
    "## 准备清单",
    ...sheet.prepChecklist.map((item) => `- ${item}`),
    "",
    "## 教学材料",
    ...sheet.materials.map((item) => `- ${item}`),
    "",
    "## 活动流程",
    ...sheet.agenda.flatMap((item, index) => [
      `### 环节 ${index + 1} ${item.title}`,
      `- 时长：${item.duration}`,
      `- 目标：${item.objective}`,
      `- 教师任务：${item.teacherTasks.join(" / ")}`,
      `- 学生任务：${item.studentTasks.join(" / ")}`,
      "",
    ]),
    "## 讨论提示",
    ...sheet.discussionPrompts.map((item) => `- ${item}`),
    "",
    "## 评价要点",
    ...sheet.assessmentPoints.map((item) => `- ${item}`),
  ].join("\r\n");

const openPrintWindow = (html: string, errorMessage: string) => {
  if (typeof window === "undefined") {
    return;
  }
  const popup = window.open("", "_blank", "width=980,height=860");
  if (!popup) {
    throw new Error(errorMessage);
  }
  popup.document.open();
  popup.document.write(html);
  popup.document.close();
  popup.focus();
  popup.setTimeout(() => popup.print(), 180);
};

const downloadText = (content: string, filename: string, mimeType: string) => {
  if (typeof window === "undefined") {
    return;
  }
  const blob = new Blob([content], { type: mimeType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
};

export const exportLearningPlanPdf = (plan: LearningPlanViewModel) => {
  openPrintWindow(buildLearningPlanHtml(plan), "无法打开导出窗口，请检查浏览器弹窗设置");
};

export const exportTeacherSheetPdf = (sheet: LearningTeacherSheet) => {
  openPrintWindow(buildTeacherSheetHtml(sheet), "无法打开活动单窗口，请检查浏览器弹窗设置");
};

export const downloadLearningPlan = (plan: LearningPlanViewModel) => {
  downloadText(
    buildLearningPlanMarkdown(plan),
    `${sanitizeFilename(plan.title)}-plan.md`,
    "text/markdown;charset=utf-8"
  );
};

export const downloadTeacherSheet = (sheet: LearningTeacherSheet) => {
  downloadText(
    buildTeacherSheetMarkdown(sheet),
    `${sanitizeFilename(sheet.title)}.md`,
    "text/markdown;charset=utf-8"
  );
};
