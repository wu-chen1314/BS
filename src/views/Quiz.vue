<template>
  <div class="quiz-page">
    <section class="hero heritage-float-card">
      <div>
        <span class="section-kicker">互动学习</span>
        <h2>非遗知识竞答</h2>
        <p>题目、计分和历史成绩都来自后端接口。普通用户可以持续答题积累记录，管理员可直接维护题库。</p>
      </div>
      <div class="insight-grid" v-loading="insightsLoading" element-loading-text="正在加载竞答概览...">
        <article v-for="item in insightCards" :key="item.label" class="insight-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.help }}</small>
        </article>
      </div>
    </section>

    <section class="content">
      <div class="main heritage-float-card" v-loading="loading" element-loading-text="正在准备题目...">
        <div class="section-head">
          <div>
            <span class="section-kicker">答题主链</span>
            <h3>抽题、作答、提交都已闭环</h3>
          </div>
          <div class="head-actions">
            <p>每次提交都由后端重新计分，避免前端状态和最终结果脱节。</p>
            <el-button v-if="isAdmin" type="warning" round plain @click="openQuestionManager">题库管理</el-button>
          </div>
        </div>

        <div v-if="!started && !finished" class="start-screen">
          <el-icon :size="64" color="var(--heritage-primary)"><Trophy /></el-icon>
          <h4>开始一轮新的非遗挑战</h4>
          <p>当前题库会从后端随机抽取 {{ questions.length || 5 }} 题，成绩会自动写入历史记录。</p>
          <div class="pill-row">
            <span class="pill">题目实时拉取</span>
            <span class="pill">服务端计分</span>
            <span class="pill">历史成绩回看</span>
            <span v-if="isAdmin" class="pill admin">管理员可维护题库</span>
          </div>
          <div class="action-row">
            <el-button type="primary" size="large" round class="start-btn" :loading="loading" @click="startQuiz">开始挑战</el-button>
            <el-button round :loading="loading" @click="refreshQuestionSet">刷新题目</el-button>
          </div>
        </div>

        <div v-else-if="started && currentQuestion" class="quiz-box">
          <div class="progress"><div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div></div>
          <div class="meta-row">
            <span>第 {{ currentIndex + 1 }} 题 / 共 {{ questions.length }} 题</span>
            <span class="score-chip">当前已答对 {{ provisionalCorrectCount }} 题</span>
          </div>
          <div class="question-card">
            <div class="tag-row">
              <span class="question-tag">{{ currentQuestion.category || "综合" }}</span>
              <span class="question-tag subtle">{{ currentQuestion.difficulty || "medium" }}</span>
            </div>
            <h4>{{ currentQuestion.question }}</h4>
            <div class="options">
              <button
                v-for="(option, index) in currentQuestion.options"
                :key="index"
                class="option-btn"
                :class="{ selected: selectedAnswer === index, correct: showResult && index === currentQuestion.correctAnswer, wrong: showResult && selectedAnswer === index && index !== currentQuestion.correctAnswer }"
                :disabled="showResult"
                type="button"
                @click="selectAnswer(index)"
              >
                <span class="option-letter">{{ String.fromCharCode(65 + index) }}</span>
                <span class="option-text">{{ option }}</span>
                <el-icon v-if="showResult && index === currentQuestion.correctAnswer" class="result-ok"><Check /></el-icon>
                <el-icon v-if="showResult && selectedAnswer === index && index !== currentQuestion.correctAnswer" class="result-bad"><Close /></el-icon>
              </button>
            </div>
            <div v-if="showResult" class="explanation" :class="isCorrect ? 'ok' : 'bad'">
              <h5>{{ isCorrect ? "回答正确" : "回答错误" }}</h5>
              <p>{{ currentQuestion.explanation }}</p>
              <el-button type="primary" :loading="submitting" @click="nextQuestion">{{ currentIndex === questions.length - 1 ? "提交成绩" : "下一题" }}</el-button>
            </div>
          </div>
        </div>

        <div v-else-if="finished && attemptResult" class="result-screen">
          <el-icon :size="84" :color="attemptResult.score >= 80 ? 'var(--heritage-warning)' : 'var(--heritage-primary)'">
            <Medal v-if="attemptResult.score >= 80" />
            <Star v-else />
          </el-icon>
          <h4>本轮挑战完成</h4>
          <div v-if="achievementLabel" class="achievement" :class="{ perfect: isPerfectScore }">{{ achievementLabel }}</div>
          <div class="score-display"><span class="score-number">{{ attemptResult.score }}</span><span>分</span></div>
          <p>{{ resultMessage }}</p>
          <p v-if="achievementDescription" class="subtext">{{ achievementDescription }}</p>
          <div class="summary-row">
            <article class="summary-card"><span>答对题数</span><strong>{{ attemptResult.correctCount }} / {{ attemptResult.totalQuestions }}</strong></article>
            <article class="summary-card"><span>耗时</span><strong>{{ formatDuration(attemptResult.durationSeconds) }}</strong></article>
            <article class="summary-card"><span>提交时间</span><strong>{{ formatDateTime(attemptResult.submittedAt) }}</strong></article>
          </div>
          <div class="action-row">
            <el-button type="primary" size="large" round class="start-btn" :loading="loading" @click="restartQuiz">再来一轮</el-button>
            <el-button round @click="reviewLatestAttempt">查看最近结果</el-button>
          </div>
        </div>
      </div>

      <aside class="side">
        <section class="heritage-float-card side-card">
          <div class="section-head compact"><div><span class="section-kicker">最近成绩</span><h3>答题历史</h3></div></div>
          <div v-if="history.length" class="history-list">
            <article v-for="item in history" :key="item.id" class="history-card">
              <div class="history-top"><strong>{{ item.score }} 分</strong><span>{{ item.correctCount }}/{{ item.totalQuestions }}</span></div>
              <p>{{ formatDuration(item.durationSeconds) }} / {{ formatDateTime(item.createdAt) }}</p>
            </article>
          </div>
          <el-empty v-else description="当前还没有答题历史" :image-size="88" />
        </section>

        <section class="heritage-float-card side-card">
          <div class="section-head compact"><div><span class="section-kicker">本轮题库</span><h3>抽题状态</h3></div></div>
          <div class="bank-row"><span>已加载题目</span><strong>{{ questions.length }}</strong></div>
          <div class="bank-row"><span>最新得分</span><strong>{{ summary.latestScore ?? "--" }}</strong></div>
          <div class="bank-row"><span>最近答对</span><strong>{{ summary.latestCorrectCount ?? "--" }}</strong></div>
          <el-button class="full" round :loading="loading" @click="refreshQuestionSet">刷新题目集</el-button>
          <div v-if="isAdmin" class="admin-note"><strong>管理员提示</strong><p>新增、停用或调整排序后，下一次刷新会立即生效。</p></div>
        </section>
      </aside>
    </section>

    <el-dialog v-model="managerVisible" title="竞赛题库管理" width="1040px" destroy-on-close>
      <div class="manager-tools">
        <el-input v-model="managerKeyword" clearable placeholder="搜索题目、分类或解析" @keyup.enter="handleManagerSearch" />
        <el-select v-model="managerActiveFilter"><el-option label="全部状态" value="all" /><el-option label="仅启用" value="active" /><el-option label="仅停用" value="inactive" /></el-select>
        <el-button type="primary" round @click="handleManagerSearch">搜索</el-button>
        <el-button round @click="resetManagerFilters">重置</el-button>
        <el-button type="warning" round plain @click="openQuestionCreateDialog">新增题目</el-button>
      </div>
          <el-table :data="managedQuestions" v-loading="managerLoading" element-loading-text="正在加载题库..." empty-text="当前还没有题库数据">
        <el-table-column label="题目" min-width="320">
          <template #default="{ row }"><div class="q-cell"><strong>{{ row.question }}</strong><small>{{ row.explanation }}</small></div></template>
        </el-table-column>
        <el-table-column label="分类" prop="category" width="120" />
        <el-table-column label="难度" prop="difficulty" width="120" />
        <el-table-column label="排序" prop="sortOrder" width="90" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.active ? 'success' : 'info'" effect="plain">{{ row.active ? "启用" : "停用" }}</el-tag></template></el-table-column>
        <el-table-column label="更新时间" width="180"><template #default="{ row }">{{ formatDateTime(row.updatedAt || row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><el-button text type="primary" @click="openQuestionEditDialog(row)">编辑</el-button><el-button text type="danger" @click="handleDeleteQuestion(row.id)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="manager-page"><el-pagination background layout="total, prev, pager, next" :current-page="managerPageNum" :page-size="managerPageSize" :total="managerTotal" @current-change="handleManagerPageChange" /></div>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? '新增题目' : '编辑题目'" width="760px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="题目内容" required><el-input v-model="questionForm.question" type="textarea" :rows="3" maxlength="220" show-word-limit /></el-form-item>
        <div class="grid-2">
          <el-form-item v-for="(_, index) in questionForm.options" :key="index" :label="`选项 ${String.fromCharCode(65 + index)}`" required><el-input v-model="questionForm.options[index]" maxlength="120" show-word-limit /></el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="正确答案" required><el-select v-model="questionForm.correctAnswer"><el-option v-for="(_, index) in questionForm.options" :key="`answer-${index}`" :label="`选项 ${String.fromCharCode(65 + index)}`" :value="index" /></el-select></el-form-item>
          <el-form-item label="题目分类"><el-input v-model="questionForm.category" maxlength="30" /></el-form-item>
          <el-form-item label="难度"><el-select v-model="questionForm.difficulty"><el-option label="easy" value="easy" /><el-option label="medium" value="medium" /><el-option label="hard" value="hard" /></el-select></el-form-item>
          <el-form-item label="排序值"><el-input-number v-model="questionForm.sortOrder" :min="0" :max="9999" controls-position="right" /></el-form-item>
          <el-form-item label="启用状态"><el-switch v-model="questionForm.active" inline-prompt active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
        <el-form-item label="题目解析" required><el-input v-model="questionForm.explanation" type="textarea" :rows="4" maxlength="400" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button round @click="editorVisible = false">取消</el-button><el-button type="primary" round :loading="editorSaving" @click="handleSaveQuestion">{{ editorMode === "create" ? "保存并发布" : "保存修改" }}</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Check, Close, Medal, Star, Trophy } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { createQuizQuestion, deleteQuizQuestion, fetchQuizHistory, fetchQuizQuestionAdminPage, fetchQuizQuestions, fetchQuizSummary, submitQuizAttempt, updateQuizQuestion } from "@/services/quiz";
import type { QuizAttemptResult, QuizHistoryItem, QuizQuestion, QuizQuestionAdminPage, QuizQuestionEditorPayload, QuizQuestionRecord, QuizSummary } from "@/types/quiz";
import { getCurrentUser } from "@/utils/session";
import { confirmDangerAction, showSuccess, showWarning, successText } from "@/utils/uiFeedback";

type ManagerFilter = "all" | "active" | "inactive";
type EditorMode = "create" | "edit";

const currentUser = ref(getCurrentUser());
const isAdmin = computed(() => currentUser.value?.role === "admin");
const questions = ref<QuizQuestion[]>([]);
const history = ref<QuizHistoryItem[]>([]);
const summary = ref<QuizSummary>({ attemptCount: 0, bestScore: 0, averageScore: 0, latestScore: null, latestCorrectCount: null, latestAttemptAt: null, consistency: "尚未开始答题" });
const loading = ref(false);
const insightsLoading = ref(false);
const started = ref(false);
const finished = ref(false);
const submitting = ref(false);
const currentIndex = ref(0);
const selectedAnswer = ref<number | null>(null);
const showResult = ref(false);
const isCorrect = ref(false);
const provisionalCorrectCount = ref(0);
const answers = ref<Array<number | null>>([]);
const attemptResult = ref<QuizAttemptResult | null>(null);
const sessionStartedAt = ref(0);

const managerVisible = ref(false);
const managerLoading = ref(false);
const managerKeyword = ref("");
const managerActiveFilter = ref<ManagerFilter>("all");
const managerPageNum = ref(1);
const managerPageSize = 8;
const managerTotal = ref(0);
const managedQuestions = ref<QuizQuestionRecord[]>([]);

const editorVisible = ref(false);
const editorSaving = ref(false);
const editorMode = ref<EditorMode>("create");
const editingQuestionId = ref<number | null>(null);
const questionForm = reactive<QuizQuestionEditorPayload>({ question: "", options: ["", "", "", ""], correctAnswer: 0, explanation: "", category: "综合", difficulty: "medium", sortOrder: 0, active: true });

const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const progressPercent = computed(() => (!questions.value.length ? 0 : Math.min(100, Math.round(((currentIndex.value + (showResult.value ? 1 : 0)) / questions.value.length) * 100))));
const isPerfectScore = computed(() => Boolean(attemptResult.value && attemptResult.value.totalQuestions > 0 && attemptResult.value.correctCount === attemptResult.value.totalQuestions));
const achievementLabel = computed(() => (isPerfectScore.value ? "非遗知识小能手" : (attemptResult.value?.score || 0) >= 80 ? "非遗学习进阶者" : ""));
const achievementDescription = computed(() => (isPerfectScore.value ? "你已解锁满分称号，可以继续去资讯动态和首页项目挑战更细的知识点。" : (attemptResult.value?.score || 0) >= 80 ? "再多做几轮题并结合资讯阅读，你就很接近“非遗知识小能手”了。" : ""));
const resultMessage = computed(() => {
  const score = attemptResult.value?.score || 0;
  if (score === 100) return "满分通关，当前这组题已经被你全部拿下。";
  if (score >= 80) return "表现非常稳，已经具备不错的非遗知识基础。";
  if (score >= 60) return "整体不错，再结合资讯和项目内容复习一轮会更扎实。";
  return "这一轮主要在建立基础，继续刷题和阅读内容会提升很快。";
});
const insightCards = computed(() => [
  { label: "挑战次数", value: summary.value.attemptCount, help: summary.value.consistency },
  { label: "历史最佳", value: summary.value.bestScore, help: "记录你的成绩上限" },
  { label: "平均得分", value: summary.value.averageScore, help: "观察整体稳定度变化" },
  { label: "最近一轮", value: summary.value.latestScore ?? "--", help: formatDateTime(summary.value.latestAttemptAt) },
]);

const normalizeActive = () => managerActiveFilter.value === "active" ? true : managerActiveFilter.value === "inactive" ? false : null;
const formatDateTime = (value?: string | null) => {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, "0")}-${`${date.getDate()}`.padStart(2, "0")} ${`${date.getHours()}`.padStart(2, "0")}:${`${date.getMinutes()}`.padStart(2, "0")}`;
};
const formatDuration = (seconds?: number | null) => {
  const safe = Math.max(0, Number(seconds || 0));
  const minutes = Math.floor(safe / 60);
  const remain = safe % 60;
  return minutes === 0 ? `${remain} 秒` : `${minutes} 分 ${remain} 秒`;
};

const resetQuizState = () => { started.value = false; finished.value = false; currentIndex.value = 0; selectedAnswer.value = null; showResult.value = false; isCorrect.value = false; provisionalCorrectCount.value = 0; answers.value = []; attemptResult.value = null; sessionStartedAt.value = 0; };
const resetQuestionForm = () => { editorMode.value = "create"; editingQuestionId.value = null; Object.assign(questionForm, { question: "", options: ["", "", "", ""], correctAnswer: 0, explanation: "", category: "综合", difficulty: "medium", sortOrder: 0, active: true }); };
const fillQuestionForm = (row: QuizQuestionRecord) => { editorMode.value = "edit"; editingQuestionId.value = row.id; Object.assign(questionForm, { question: row.question, options: [...row.options, "", "", ""].slice(0, 4), correctAnswer: row.correctAnswer, explanation: row.explanation, category: row.category || "综合", difficulty: row.difficulty || "medium", sortOrder: row.sortOrder ?? 0, active: row.active }); };

const loadInsights = async () => {
  insightsLoading.value = true;
  try {
    const [summaryRes, historyRes] = await Promise.all([fetchQuizSummary(), fetchQuizHistory()]);
    summary.value = summaryRes.data.data;
    history.value = historyRes.data.data || [];
  } catch (error) { console.error("Failed to load quiz insights", error); } finally { insightsLoading.value = false; }
};
const refreshQuestionSet = async () => {
  loading.value = true;
  try {
    const res = await fetchQuizQuestions();
    questions.value = res.data.data || [];
    resetQuizState();
  } catch (error) { console.error("Failed to load quiz questions", error); questions.value = []; } finally { loading.value = false; }
};
const maybeRefreshQuestionSetAfterManage = async () => { if (!started.value) await refreshQuestionSet(); };
const loadManagedQuestions = async (resetPage = false) => {
  if (!isAdmin.value) return;
  if (resetPage) managerPageNum.value = 1;
  managerLoading.value = true;
  try {
    const res = await fetchQuizQuestionAdminPage({ pageNum: managerPageNum.value, pageSize: managerPageSize, keyword: managerKeyword.value.trim(), active: normalizeActive() });
    const page = res.data.data as QuizQuestionAdminPage;
    managedQuestions.value = page.records || [];
    managerTotal.value = Number(page.total || 0);
  } catch (error) { console.error("Failed to load managed questions", error); managedQuestions.value = []; managerTotal.value = 0; ElMessage.error("题库加载失败"); } finally { managerLoading.value = false; }
};

const openQuestionManager = async () => { managerVisible.value = true; await loadManagedQuestions(true); };
const handleManagerSearch = async () => { await loadManagedQuestions(true); };
const resetManagerFilters = async () => { managerKeyword.value = ""; managerActiveFilter.value = "all"; await loadManagedQuestions(true); };
const handleManagerPageChange = async (page: number) => { managerPageNum.value = page; await loadManagedQuestions(); };
const openQuestionCreateDialog = () => { resetQuestionForm(); editorVisible.value = true; };
const openQuestionEditDialog = (row: QuizQuestionRecord) => { fillQuestionForm(row); editorVisible.value = true; };

const handleSaveQuestion = async () => {
  const title = questionForm.question.trim();
  const optionValues = questionForm.options.map((item) => item.trim());
if (!title) return showWarning("请输入题目内容");
if (optionValues.some((item) => !item)) return showWarning("请完整填写 4 个选项");
if (!questionForm.explanation?.trim()) return showWarning("请输入题目解析");
  editorSaving.value = true;
  try {
    const payload: QuizQuestionEditorPayload = { question: title, options: optionValues, correctAnswer: questionForm.correctAnswer, explanation: questionForm.explanation.trim(), category: questionForm.category?.trim() || "综合", difficulty: questionForm.difficulty || "medium", sortOrder: Number(questionForm.sortOrder || 0), active: Boolean(questionForm.active) };
    if (editorMode.value === "create" || !editingQuestionId.value) await createQuizQuestion(payload); else await updateQuizQuestion(editingQuestionId.value, payload);
    editorVisible.value = false;
    await Promise.allSettled([loadManagedQuestions(), maybeRefreshQuestionSetAfterManage()]);
    showSuccess(editorMode.value === "create" ? successText.created("题目") : successText.updated("题目"));
  } catch (error) { console.error("Failed to save question", error); ElMessage.error(editorMode.value === "create" ? "题目新增失败" : "题目更新失败"); } finally { editorSaving.value = false; }
};
const handleDeleteQuestion = async (id: number) => {
try { await confirmDangerAction({ detail: "删除后不会再出现在后续抽题中。", subject: "这道题目" }); } catch { return; }
try { await deleteQuizQuestion(id); await Promise.allSettled([loadManagedQuestions(), maybeRefreshQuestionSetAfterManage()]); showSuccess(successText.deleted("题目")); } catch (error) { console.error("Failed to delete question", error); ElMessage.error("题目删除失败"); }
};

const startQuiz = async () => {
  if (!questions.value.length) await refreshQuestionSet();
if (!questions.value.length) return showWarning("当前没有可用题目，请稍后重试");
  resetQuizState();
  started.value = true;
  answers.value = new Array(questions.value.length).fill(null);
  sessionStartedAt.value = Date.now();
};
const selectAnswer = (index: number) => {
  if (showResult.value || !currentQuestion.value) return;
  selectedAnswer.value = index;
  answers.value[currentIndex.value] = index;
  isCorrect.value = index === currentQuestion.value.correctAnswer;
  if (isCorrect.value) provisionalCorrectCount.value += 1;
  showResult.value = true;
};
const finalizeAttempt = async () => {
  if (!questions.value.length) return;
  submitting.value = true;
  try {
    const durationSeconds = sessionStartedAt.value > 0 ? Math.round((Date.now() - sessionStartedAt.value) / 1000) : 0;
    const res = await submitQuizAttempt({ durationSeconds, answers: questions.value.map((question, index) => ({ questionId: question.id, selectedIndex: answers.value[index] ?? null })) });
    attemptResult.value = res.data.data;
    started.value = false;
    finished.value = true;
    showResult.value = false;
    await loadInsights();
  } catch (error) { console.error("Failed to submit quiz attempt", error); ElMessage.error("成绩提交失败，请稍后重试"); } finally { submitting.value = false; }
};
const nextQuestion = async () => {
  if (currentIndex.value < questions.value.length - 1) { currentIndex.value += 1; selectedAnswer.value = answers.value[currentIndex.value] ?? null; showResult.value = false; isCorrect.value = false; return; }
  await finalizeAttempt();
};
const restartQuiz = async () => { await refreshQuestionSet(); await startQuiz(); };
const reviewLatestAttempt = () => {
  if (!history.value.length) return ElMessage.info("当前还没有可回看的历史记录");
  const latest = history.value[0];
  const title = latest.score === 100 ? "非遗知识小能手" : latest.score >= 80 ? "非遗学习进阶者" : "继续加油";
  ElMessage.info(`最近一轮成绩：${latest.score} 分，答对 ${latest.correctCount}/${latest.totalQuestions}，当前评价：${title}`);
};

onMounted(async () => { currentUser.value = getCurrentUser(); await Promise.all([refreshQuestionSet(), loadInsights()]); });
</script>

<style scoped>
.quiz-page,.side{display:grid;gap:24px}.quiz-page{padding:24px;min-height:calc(100vh - 64px);background:radial-gradient(circle at top left,rgba(164,59,47,.08),transparent 26%),radial-gradient(circle at 82% 14%,rgba(212,167,98,.18),transparent 20%),linear-gradient(180deg,var(--heritage-paper-soft),var(--heritage-paper))}.heritage-float-card{border-radius:28px;background:var(--heritage-glass-bg);border:1px solid var(--heritage-glass-border);box-shadow:var(--heritage-glass-shadow);backdrop-filter:blur(16px);-webkit-backdrop-filter:blur(16px)}.hero,.main,.side-card{padding:28px}.hero,.content,.insight-grid,.options,.history-list,.manager-tools,.grid-2{display:grid;gap:16px}.hero{grid-template-columns:minmax(0,1.2fr) minmax(320px,1fr)}.insight-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.content{grid-template-columns:minmax(0,1.6fr) minmax(300px,.9fr);align-items:start}.section-kicker{display:inline-flex;padding:4px 10px;border-radius:999px;background:rgba(164,59,47,.1);color:var(--heritage-primary);font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase}.hero h2,.section-head h3{margin:0;font-size:32px;font-weight:800;color:transparent;background:linear-gradient(135deg,var(--heritage-primary),var(--heritage-ink));-webkit-background-clip:text;background-clip:text}.hero p,.section-head p,.admin-note p{margin:12px 0 0;color:var(--heritage-ink-soft);line-height:1.8}.insight-card,.summary-card,.question-card,.history-card,.bank-row,.admin-note{padding:18px;border-radius:20px;background:rgba(255,255,255,.74);border:1px solid rgba(164,59,47,.08)}.insight-card span,.summary-card span,.history-card p,.q-cell small,.bank-row span{font-size:13px;color:var(--heritage-muted)}.insight-card strong,.summary-card strong,.history-card strong,.bank-row strong{font-size:28px;color:var(--heritage-ink)}.section-head{display:flex;justify-content:space-between;gap:24px;align-items:flex-end;margin-bottom:24px}.section-head.compact{margin-bottom:18px}.head-actions{display:grid;gap:12px;justify-items:end}.start-screen,.result-screen{display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:26px 0 8px}.start-screen h4,.result-screen h4{margin:12px 0;font-size:28px;color:var(--heritage-ink)}.pill-row,.action-row,.summary-row{display:flex;flex-wrap:wrap;justify-content:center;gap:12px}.pill{padding:8px 14px;border-radius:999px;background:rgba(164,59,47,.08);color:var(--heritage-primary);font-size:13px;font-weight:600}.pill.admin{background:rgba(212,167,98,.18);color:var(--heritage-warning)}.start-btn{padding:12px 40px;border-radius:999px;border:none;box-shadow:0 12px 28px rgba(164,59,47,.22)}.progress{height:10px;border-radius:999px;background:rgba(0,0,0,.06);overflow:hidden}.progress-fill{height:100%;background:linear-gradient(90deg,#dd9a54,var(--heritage-primary));transition:width .35s ease}.meta-row{display:flex;justify-content:space-between;gap:12px;align-items:center;color:var(--heritage-muted);font-weight:600}.score-chip{padding:8px 12px;border-radius:999px;background:rgba(164,59,47,.08);color:var(--heritage-primary)}.tag-row{display:flex;gap:10px;margin-bottom:18px}.question-tag{padding:6px 12px;border-radius:999px;background:rgba(164,59,47,.1);color:var(--heritage-primary);font-size:13px;font-weight:700}.question-tag.subtle{background:rgba(88,75,53,.08);color:var(--heritage-ink-soft)}.question-card h4{margin:0 0 20px;font-size:24px;line-height:1.55;color:var(--heritage-ink)}.option-btn{display:flex;align-items:center;gap:16px;width:100%;padding:16px 18px;border-radius:16px;border:2px solid rgba(88,75,53,.12);background:rgba(255,255,255,.88);text-align:left;cursor:pointer;transition:.18s}.option-btn.selected{border-color:var(--heritage-primary);background:rgba(164,59,47,.06)}.option-btn.correct{border-color:var(--heritage-jade);background:rgba(82,190,128,.1)}.option-btn.wrong{border-color:var(--heritage-primary);background:rgba(164,59,47,.1)}.option-letter{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:rgba(0,0,0,.05);font-weight:700}.option-btn.selected .option-letter{background:var(--heritage-primary);color:#fff}.option-btn.correct .option-letter{background:var(--heritage-jade);color:#fff}.option-text{flex:1;color:var(--heritage-ink);line-height:1.6}.result-ok{color:var(--heritage-jade)}.result-bad{color:var(--heritage-primary)}.explanation{margin-top:22px;padding:20px;border-radius:18px}.explanation.ok{background:rgba(82,190,128,.1);border-left:4px solid var(--heritage-jade)}.explanation.bad{background:rgba(164,59,47,.08);border-left:4px solid var(--heritage-primary)}.achievement{display:inline-flex;margin-bottom:18px;padding:8px 18px;border-radius:999px;background:rgba(164,59,47,.08);color:var(--heritage-primary);font-weight:700}.achievement.perfect{background:linear-gradient(135deg,rgba(212,167,98,.18),rgba(164,59,47,.16));color:var(--heritage-warning);box-shadow:0 12px 24px rgba(164,59,47,.12)}.score-display{display:flex;align-items:baseline;gap:6px;margin-bottom:18px}.score-number{font-size:72px;font-weight:800;color:transparent;background:linear-gradient(135deg,var(--heritage-primary),var(--heritage-gold));-webkit-background-clip:text;background-clip:text}.subtext{margin-top:-12px;color:var(--heritage-ink-soft)}.full{width:100%;margin-top:16px}.admin-note{margin-top:16px}.admin-note strong,.q-cell strong{color:var(--heritage-ink)}.manager-tools{grid-template-columns:minmax(0,1.5fr) 180px auto auto auto;margin-bottom:18px}.manager-page,.dialog-footer{display:flex;justify-content:center;margin-top:20px}.dialog-footer{justify-content:flex-end;gap:12px;margin-top:0}.q-cell{display:grid;gap:6px}.grid-2{grid-template-columns:repeat(2,minmax(0,1fr))}.history-top{display:flex;justify-content:space-between;gap:10px;align-items:center}@media (max-width:1100px){.hero,.content,.manager-tools,.grid-2{grid-template-columns:1fr}}@media (max-width:768px){.quiz-page{padding:16px}.hero,.main,.side-card{padding:20px 16px}.insight-grid{grid-template-columns:1fr}.section-head,.meta-row{flex-direction:column;align-items:stretch}.head-actions{justify-items:stretch}}
</style>
