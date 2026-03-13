<template>
  <div class="news-page">
    <section class="hero heritage-float-card">
      <div>
        <span class="section-kicker">内容主线</span>
        <h2>非遗资讯前沿</h2>
        <p>资讯模块已接入封面与视频素材，管理员可直接上传、编辑并发布，普通用户可继续阅读和查看详情。</p>
      </div>
      <div class="hero-stats">
        <article class="stat-card">
          <span>资讯总量</span>
          <strong>{{ dashboard.articleCount }}</strong>
        </article>
        <article class="stat-card">
          <span>累计阅读</span>
          <strong>{{ dashboard.totalViewCount }}</strong>
        </article>
        <article class="stat-card">
          <span>最近阅读</span>
          <strong>{{ dashboard.recentReads.length }}</strong>
        </article>
      </div>
    </section>

    <section class="toolbar heritage-float-card">
      <el-input v-model="keyword" clearable placeholder="搜索资讯标题、摘要或来源" @keyup.enter="handleSearch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="toolbar-actions">
        <el-button type="primary" round @click="handleSearch">搜索</el-button>
        <el-button round @click="resetFilters">重置</el-button>
        <el-button v-if="isAdmin" type="warning" round plain @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          发布资讯
        </el-button>
      </div>
    </section>

    <section class="tag-row">
      <button class="tag-chip" :class="{ active: activeTag === '' }" type="button" @click="applyTag('')">全部主题</button>
      <button v-for="tag in availableTags" :key="tag" class="tag-chip" :class="{ active: activeTag === tag }" type="button" @click="applyTag(tag)">
        {{ tag }}
      </button>
    </section>

    <section class="layout">
      <div class="news-column heritage-float-card" v-loading="loading" element-loading-text="正在加载资讯列表...">
        <div class="panel-head">
          <div>
            <span class="section-kicker">资讯列表</span>
            <h3>按主题浏览最新内容</h3>
          </div>
          <p>列表页展示封面缩略图，详情页展示封面原图和视频播放控件。</p>
        </div>

        <el-empty v-if="!loading && articles.length === 0" description="当前筛选条件下还没有资讯内容" :image-size="120" />

        <div v-else class="news-list">
          <article v-for="item in articles" :key="item.id" class="news-card">
            <div class="news-date">
              <span class="day">{{ formatDay(item.publishedAt) }}</span>
              <span class="month-year">{{ formatMonth(item.publishedAt) }}</span>
            </div>

            <div class="news-copy">
              <div class="news-meta">
                <el-tag size="small" :type="item.tagType || 'info'" effect="plain">{{ item.tag }}</el-tag>
                <span>{{ item.source }}</span>
                <span>/</span>
                <span class="news-views"><el-icon><View /></el-icon>{{ item.viewCount }}</span>
              </div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
              <div class="news-actions">
                <button class="read-more" type="button" @click="openArticle(item.id)">
                  阅读全文
                  <el-icon><ArrowRight /></el-icon>
                </button>
                <span v-if="item.videoUrl" class="media-chip">含视频素材</span>
              </div>
              <div v-if="isAdmin" class="admin-actions">
                <el-button text type="primary" @click.stop="openEditDialog(item)">编辑</el-button>
                <el-button text type="danger" @click.stop="handleDeleteArticle(item.id)">删除</el-button>
              </div>
            </div>

            <div v-if="getArticleCover(item)" class="news-image">
              <img :src="getArticleCover(item)" :alt="item.title" />
            </div>
          </article>
        </div>

        <div class="load-more">
          <el-button round :loading="loadingMore" :disabled="!hasMore" @click="loadMore">
            {{ hasMore ? "加载更多资讯" : "没有更多内容了" }}
          </el-button>
        </div>
      </div>

      <aside class="sidebar">
        <section class="sidebar-panel heritage-float-card">
          <span class="section-kicker">热门标签</span>
          <h3>内容热度</h3>
          <div v-if="dashboard.featuredTags.length" class="metric-list">
            <button v-for="item in dashboard.featuredTags" :key="item.name" class="metric-chip" type="button" @click="applyTag(item.name)">
              <span>{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </button>
          </div>
          <el-empty v-else description="当前还没有标签数据" :image-size="88" />
        </section>

        <section class="sidebar-panel heritage-float-card">
          <span class="section-kicker">热门文章</span>
          <h3>继续探索</h3>
          <div v-if="dashboard.hotArticles.length" class="metric-list">
            <button v-for="item in dashboard.hotArticles" :key="item.id" class="metric-chip hot-card" type="button" @click="openArticle(item.id)">
              <strong>{{ item.title }}</strong>
              <span>{{ item.viewCount }} 次阅读</span>
            </button>
          </div>
          <el-empty v-else description="当前还没有热门内容" :image-size="88" />
        </section>
      </aside>
    </section>
    <el-dialog :model-value="detailVisible" width="860px" destroy-on-close top="6vh" @close="closeDetail">
      <template #header>
        <div class="dialog-header">
          <span class="section-kicker">{{ activeArticle?.tag || "资讯详情" }}</span>
          <h3>{{ activeArticle?.title || "资讯详情" }}</h3>
          <p v-if="activeArticle">{{ activeArticle.source }} / {{ formatDateTime(activeArticle.publishedAt) }} / {{ activeArticle.viewCount }} 次阅读</p>
        </div>
      </template>

      <div v-loading="detailLoading" class="detail-body" element-loading-text="正在加载资讯详情...">
        <template v-if="activeArticle">
          <img v-if="getArticleCover(activeArticle)" class="detail-image" :src="getArticleCover(activeArticle)" :alt="activeArticle.title" />
          <p class="detail-summary">{{ activeArticle.summary }}</p>
          <video v-if="getArticleVideo(activeArticle)" class="detail-video" :src="getArticleVideo(activeArticle)" controls preload="metadata">
            当前浏览器暂不支持视频播放。
          </video>
          <div class="detail-content">
            <p v-for="(paragraph, index) in detailParagraphs" :key="`${activeArticle.id}-${index}`">{{ paragraph }}</p>
          </div>
        </template>
        <el-empty v-else description="资讯详情暂时加载失败，请稍后重试" :image-size="96" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="isAdmin && activeArticle" round @click="openEditDialog(activeArticle)">编辑当前资讯</el-button>
          <el-button round @click="closeDetail">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="editorVisible" width="900px" destroy-on-close :title="editorMode === 'create' ? '发布资讯' : '编辑资讯'" @closed="resetArticleForm">
      <el-form label-position="top" class="editor-form">
        <div class="editor-grid">
          <el-form-item label="资讯标题" required><el-input v-model="articleForm.title" maxlength="80" show-word-limit placeholder="请输入资讯标题" /></el-form-item>
          <el-form-item label="主题标签"><el-input v-model="articleForm.tag" maxlength="20" placeholder="如：活动预告、传承课堂" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="articleForm.source" maxlength="40" placeholder="如：平台内容中心" /></el-form-item>
          <el-form-item label="标签样式">
            <el-select v-model="articleForm.tagType" placeholder="请选择标签样式">
              <el-option label="默认" value="info" />
              <el-option label="成功" value="success" />
              <el-option label="提醒" value="warning" />
              <el-option label="高亮" value="primary" />
              <el-option label="风险" value="danger" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布时间">
            <el-date-picker v-model="articleForm.publishedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择发布时间" />
          </el-form-item>
        </div>

        <div class="media-grid">
          <section class="media-card">
            <div class="media-head">
              <span class="section-kicker">封面照片</span>
              <h4>上传资讯封面</h4>
              <p>支持 `jpg / jpeg / png / gif / webp`，单张不超过 5 MB。</p>
            </div>
            <el-upload class="media-uploader" :action="coverUploadAction" :headers="uploadHeaders" :show-file-list="false" accept=".jpg,.jpeg,.png,.gif,.webp" :before-upload="beforeCoverUpload" :on-success="handleCoverUploadSuccess" :on-error="handleCoverUploadError">
              <div class="upload-box" :class="{ uploading: coverUploading }">
                <img v-if="coverPreviewUrl" class="preview-image" :src="coverPreviewUrl" alt="资讯封面预览" />
                <div v-else class="upload-placeholder">
                  <el-icon><Picture /></el-icon>
                  <strong>{{ coverUploading ? "封面上传中..." : "点击上传封面照片" }}</strong>
                  <span>上传后会实时显示封面预览</span>
                </div>
              </div>
            </el-upload>
            <div class="media-actions">
              <el-button v-if="articleForm.coverImageUrl" round plain type="danger" @click="removeCoverAsset">删除封面</el-button>
            </div>
          </section>

          <section class="media-card">
            <div class="media-head">
              <span class="section-kicker">资讯视频</span>
              <h4>上传视频素材</h4>
              <p>支持 `mp4 / webm / mov`，单个文件不超过 80 MB。</p>
            </div>
            <el-upload class="media-uploader" :action="videoUploadAction" :headers="uploadHeaders" :show-file-list="false" accept=".mp4,.webm,.mov" :before-upload="beforeVideoUpload" :on-success="handleVideoUploadSuccess" :on-error="handleVideoUploadError">
              <div class="upload-box" :class="{ uploading: videoUploading }">
                <video v-if="videoPreviewUrl" class="preview-video" :src="videoPreviewUrl" controls preload="metadata">当前浏览器暂不支持视频播放。</video>
                <div v-else class="upload-placeholder">
                  <el-icon><VideoCamera /></el-icon>
                  <strong>{{ videoUploading ? "视频上传中..." : "点击上传资讯视频" }}</strong>
                  <span>上传后会实时显示视频预览</span>
                </div>
              </div>
            </el-upload>
            <div class="media-actions">
              <el-button v-if="articleForm.videoUrl" round plain type="danger" @click="removeVideoAsset">删除视频</el-button>
            </div>
          </section>
        </div>

        <el-form-item label="摘要">
          <el-input v-model="articleForm.summary" type="textarea" :rows="3" maxlength="160" show-word-limit placeholder="可选，不填写时将根据正文自动生成摘要" />
        </el-form-item>
        <el-form-item label="正文内容" required>
          <el-input v-model="articleForm.content" type="textarea" :rows="10" maxlength="5000" show-word-limit placeholder="请输入资讯正文，可按段落粘贴内容" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button round @click="editorVisible = false">取消</el-button>
          <el-button type="primary" round :loading="editorSaving" @click="handleSaveArticle">{{ editorMode === "create" ? "发布" : "保存修改" }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowRight, Picture, Plus, Search, VideoCamera, View } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { UploadProps, UploadRawFile } from "element-plus";
import { createNewsArticle, deleteNewsArticle, fetchNewsArticleDetail, fetchNewsArticles, fetchNewsDashboard, recordNewsArticleRead, updateNewsArticle } from "@/services/news";
import type { NewsArticleCard, NewsArticleDetail, NewsArticleEditorPayload, NewsArticlePage, NewsDashboard, NewsReadHistoryItem } from "@/types/news";
import { getCurrentUser } from "@/utils/session";
import { buildApiUrl, buildStaticUrl, getAuthHeaders } from "@/utils/url";
import { confirmDangerAction, showSuccess, successText } from "@/utils/uiFeedback";

type EditorMode = "create" | "edit";
interface UploadResponse { code?: number; data?: string; msg?: string }

const route = useRoute();
const router = useRouter();
const pageSize = 4;
const coverUploadAction = buildApiUrl("/file/upload/news-cover");
const videoUploadAction = buildApiUrl("/file/upload/news-video");
const keyword = ref("");
const activeTag = ref("");
const pageNum = ref(1);
const total = ref(0);
const hasMore = ref(false);
const loading = ref(false);
const loadingMore = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const editorVisible = ref(false);
const editorSaving = ref(false);
const coverUploading = ref(false);
const videoUploading = ref(false);
const editorMode = ref<EditorMode>("create");
const editingArticleId = ref<number | null>(null);
const currentUser = ref(getCurrentUser());
const isAdmin = computed(() => currentUser.value?.role === "admin");
const uploadHeaders = computed(() => getAuthHeaders());
const articles = ref<NewsArticleCard[]>([]);
const availableTags = ref<string[]>([]);
const activeArticle = ref<NewsArticleDetail | null>(null);
const dashboard = ref<NewsDashboard>({ articleCount: 0, totalViewCount: 0, featuredTags: [], hotArticles: [], recentReads: [] });
const articleForm = reactive<NewsArticleEditorPayload>({ title: "", summary: "", content: "", source: "平台内容中心", tag: "", tagType: "info", coverImageUrl: "", videoUrl: "", publishedAt: "" });
const detailParagraphs = computed(() => activeArticle.value?.content?.split(/\n{2,}/).map((item) => item.trim()).filter(Boolean) || []);
const coverPreviewUrl = computed(() => buildStaticUrl(articleForm.coverImageUrl || ""));
const videoPreviewUrl = computed(() => buildStaticUrl(articleForm.videoUrl || ""));

const normalizeCard = (article?: Partial<NewsArticleCard> | null): NewsArticleCard => {
  const coverImageUrl = article?.coverImageUrl || article?.image || "";
  return { id: Number(article?.id || 0), title: article?.title || "", summary: article?.summary || "", source: article?.source || "", tag: article?.tag || "", tagType: article?.tagType || "info", image: coverImageUrl, coverImageUrl, videoUrl: article?.videoUrl || "", publishedAt: article?.publishedAt || "", viewCount: Number(article?.viewCount || 0) };
};

const normalizeDetail = (article?: Partial<NewsArticleDetail> | null): NewsArticleDetail => ({ ...normalizeCard(article), content: article?.content || "" });
const normalizeHistory = (article?: Partial<NewsReadHistoryItem> | null): NewsReadHistoryItem => ({ ...normalizeCard(article), lastReadAt: article?.lastReadAt || null });
const getArticleCover = (article?: Partial<NewsArticleCard> | Partial<NewsArticleDetail> | null) => buildStaticUrl(article?.coverImageUrl || article?.image || "");
const getArticleVideo = (article?: Partial<NewsArticleCard> | Partial<NewsArticleDetail> | null) => buildStaticUrl(article?.videoUrl || "");
const formatDay = (value: string) => { const date = new Date(value); return Number.isNaN(date.getTime()) ? "--" : `${date.getDate()}`.padStart(2, "0"); };
const formatMonth = (value: string) => { const date = new Date(value); return Number.isNaN(date.getTime()) ? "--" : `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, "0")}`; };
const formatDateTime = (value?: string | null) => { if (!value) return "--"; const date = new Date(value); return Number.isNaN(date.getTime()) ? "--" : `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, "0")}-${`${date.getDate()}`.padStart(2, "0")} ${`${date.getHours()}`.padStart(2, "0")}:${`${date.getMinutes()}`.padStart(2, "0")}`; };

const resetArticleForm = () => {
  Object.assign(articleForm, { title: "", summary: "", content: "", source: "平台内容中心", tag: "", tagType: "info", coverImageUrl: "", videoUrl: "", publishedAt: "" });
  editingArticleId.value = null;
  editorMode.value = "create";
  coverUploading.value = false;
  videoUploading.value = false;
};

const fillArticleForm = (article?: Partial<NewsArticleDetail> | null) => {
  resetArticleForm();
  if (!article) return;
  editingArticleId.value = Number(article.id || 0) || null;
  editorMode.value = "edit";
  Object.assign(articleForm, { title: article.title || "", summary: article.summary || "", content: article.content || "", source: article.source || "平台内容中心", tag: article.tag || "", tagType: article.tagType || "info", coverImageUrl: article.coverImageUrl || article.image || "", videoUrl: article.videoUrl || "", publishedAt: article.publishedAt || "" });
};

const updateArticleViewCount = (articleId: number, viewCount: number) => {
  articles.value = articles.value.map((item) => item.id === articleId ? { ...item, viewCount } : item);
};

const upsertArticleInList = (detail: NewsArticleDetail) => {
  const nextCard = normalizeCard(detail);
  const existingIndex = articles.value.findIndex((item) => item.id === detail.id);
  if (existingIndex >= 0) { articles.value.splice(existingIndex, 1, nextCard); return; }
  articles.value = [nextCard, ...articles.value].slice(0, Math.max(pageSize, articles.value.length));
};

const loadDashboard = async () => {
  try {
    const res = await fetchNewsDashboard();
    const data = res.data.data as NewsDashboard;
    dashboard.value = { articleCount: Number(data.articleCount || 0), totalViewCount: Number(data.totalViewCount || 0), featuredTags: data.featuredTags || [], hotArticles: (data.hotArticles || []).map((item) => normalizeCard(item)), recentReads: (data.recentReads || []).map((item) => normalizeHistory(item)) };
  } catch (error) { console.error("Failed to load news dashboard", error); }
};

const loadArticles = async ({ append = false } = {}) => {
  const targetLoading = append ? loadingMore : loading;
  targetLoading.value = true;
  try {
    const res = await fetchNewsArticles({ pageNum: pageNum.value, pageSize, keyword: keyword.value.trim(), tag: activeTag.value });
    const page = res.data.data as NewsArticlePage;
    const records = (page.records || []).map((item) => normalizeCard(item));
    articles.value = append ? [...articles.value, ...records] : records;
    total.value = Number(page.total || 0);
    hasMore.value = Boolean(page.hasMore);
    availableTags.value = page.availableTags || [];
  } catch (error) {
    console.error("Failed to load news articles", error);
    if (!append) { articles.value = []; total.value = 0; hasMore.value = false; }
  } finally { targetLoading.value = false; }
};

const syncArticleQuery = async (articleId?: number | null) => {
  const nextQuery = { ...route.query };
  if (articleId && articleId > 0) nextQuery.id = `${articleId}`; else delete nextQuery.id;
  await router.replace({ path: route.path, query: nextQuery });
};

const openArticle = async (articleId: number) => { await syncArticleQuery(articleId); };
const closeDetail = async () => { detailVisible.value = false; activeArticle.value = null; await syncArticleQuery(null); };

const loadArticleDetail = async (articleId: number) => {
  detailLoading.value = true;
  try {
    const [detailResult, readResult] = await Promise.allSettled([fetchNewsArticleDetail(articleId), recordNewsArticleRead(articleId)]);
    if (detailResult.status !== "fulfilled") { activeArticle.value = null; detailVisible.value = false; return; }
    const detail = normalizeDetail(detailResult.value.data.data as NewsArticleDetail);
    const nextViewCount = readResult.status === "fulfilled" ? Number(readResult.value.data.data?.viewCount || detail.viewCount || 0) : Number(detail.viewCount || 0);
    activeArticle.value = { ...detail, viewCount: nextViewCount };
    updateArticleViewCount(articleId, nextViewCount);
    detailVisible.value = true;
    await loadDashboard();
  } catch (error) {
    console.error("Failed to open article detail", error);
    ElMessage.error("资讯详情加载失败");
  } finally { detailLoading.value = false; }
};

const openCreateDialog = () => { resetArticleForm(); editorVisible.value = true; };

const openEditDialog = async (article: Partial<NewsArticleCard> | Partial<NewsArticleDetail>) => {
  try {
    if (!article.id) return;
    if ("content" in article && article.content) { fillArticleForm(article as NewsArticleDetail); editorVisible.value = true; return; }
    const detailRes = await fetchNewsArticleDetail(Number(article.id));
    fillArticleForm(normalizeDetail(detailRes.data.data as NewsArticleDetail));
    editorVisible.value = true;
  } catch (error) {
    console.error("Failed to load article for editing", error);
    ElMessage.error("资讯编辑内容加载失败");
  }
};

const validateFile = (file: UploadRawFile, extensions: string[], maxSizeMb: number, subject: string) => {
  const extension = file.name?.includes(".") ? file.name.slice(file.name.lastIndexOf(".")).toLowerCase() : "";
  if (!extensions.includes(extension)) { ElMessage.warning(`${subject}仅支持 ${extensions.join(" / ")} 格式`); return false; }
  if (file.size > maxSizeMb * 1024 * 1024) { ElMessage.warning(`${subject}大小不能超过 ${maxSizeMb} MB`); return false; }
  return true;
};

const beforeCoverUpload: UploadProps["beforeUpload"] = (file) => { const passed = validateFile(file as UploadRawFile, [".jpg", ".jpeg", ".png", ".gif", ".webp"], 5, "封面照片"); coverUploading.value = passed; return passed; };
const beforeVideoUpload: UploadProps["beforeUpload"] = (file) => { const passed = validateFile(file as UploadRawFile, [".mp4", ".webm", ".mov"], 80, "资讯视频"); videoUploading.value = passed; return passed; };
const resolveUploadErrorMessage = (error: unknown, fallback: string) => {
  const response = (error as { response?: unknown } | undefined)?.response;
  if (typeof response === "string") {
    try {
      const parsed = JSON.parse(response) as { msg?: string };
      if (parsed?.msg) return parsed.msg;
    } catch {
    }
  }
  const message = (error as { message?: string } | undefined)?.message;
  if (typeof message === "string" && message.trim()) return message.trim();
  return fallback;
};
const applyUpload = (response: UploadResponse, field: "coverImageUrl" | "videoUrl", subject: string) => {
  if (!response || response.code !== 200 || !response.data) { ElMessage.error(response?.msg || `${subject}上传失败，请稍后重试`); return; }
  articleForm[field] = response.data;
  showSuccess(successText.uploaded(subject));
};
const handleCoverUploadSuccess = (response: UploadResponse) => { coverUploading.value = false; applyUpload(response, "coverImageUrl", "资讯封面"); };
const handleVideoUploadSuccess = (response: UploadResponse) => { videoUploading.value = false; applyUpload(response, "videoUrl", "资讯视频"); };
const handleCoverUploadError = (error: unknown) => { coverUploading.value = false; ElMessage.error(resolveUploadErrorMessage(error, "封面上传失败，请稍后重试")); };
const handleVideoUploadError = (error: unknown) => { videoUploading.value = false; ElMessage.error(resolveUploadErrorMessage(error, "视频上传失败，请稍后重试")); };
const removeCoverAsset = () => { articleForm.coverImageUrl = ""; showSuccess(successText.cleared("资讯封面")); };
const removeVideoAsset = () => { articleForm.videoUrl = ""; showSuccess(successText.cleared("资讯视频")); };

const handleSaveArticle = async () => {
  if (!articleForm.title?.trim()) { ElMessage.warning("请输入资讯标题"); return; }
  if (!articleForm.content?.trim()) { ElMessage.warning("请输入资讯正文"); return; }
  if (coverUploading.value || videoUploading.value) { ElMessage.warning("请等待素材上传完成后再保存资讯"); return; }
  editorSaving.value = true;
  try {
    const payload: NewsArticleEditorPayload = { title: articleForm.title.trim(), summary: articleForm.summary?.trim(), content: articleForm.content.trim(), source: articleForm.source?.trim(), tag: articleForm.tag?.trim(), tagType: articleForm.tagType, coverImageUrl: articleForm.coverImageUrl?.trim(), videoUrl: articleForm.videoUrl?.trim(), publishedAt: articleForm.publishedAt || undefined };
    const res = editorMode.value === "create" || !editingArticleId.value ? await createNewsArticle(payload) : await updateNewsArticle(editingArticleId.value, payload);
    const detail = normalizeDetail(res.data.data as NewsArticleDetail);
    upsertArticleInList(detail);
    if (activeArticle.value?.id === detail.id) activeArticle.value = detail;
    editorVisible.value = false;
    pageNum.value = 1;
    await Promise.all([loadArticles(), loadDashboard()]);
    showSuccess(editorMode.value === "create" ? successText.published("资讯") : successText.updated("资讯"));
  } catch (error) {
    console.error("Failed to save article", error);
    ElMessage.error(editorMode.value === "create" ? "资讯发布失败" : "资讯更新失败");
  } finally { editorSaving.value = false; }
};

const handleDeleteArticle = async (articleId: number) => {
  try {
    await confirmDangerAction({ detail: "删除后资讯详情、封面和视频入口都会失效，请确认当前内容已经不再需要。", subject: "这条资讯" });
  } catch { return; }
  try {
    await deleteNewsArticle(articleId);
    articles.value = articles.value.filter((item) => item.id !== articleId);
    total.value = Math.max(0, total.value - 1);
    if (activeArticle.value?.id === articleId) await closeDetail();
    await Promise.all([loadArticles(), loadDashboard()]);
    showSuccess(successText.deleted("资讯"));
  } catch (error) {
    console.error("Failed to delete article", error);
    ElMessage.error("资讯删除失败");
  }
};

const handleSearch = async () => { pageNum.value = 1; await loadArticles(); };
const resetFilters = async () => { keyword.value = ""; activeTag.value = ""; pageNum.value = 1; await loadArticles(); };
const applyTag = async (tag: string) => { if (activeTag.value === tag) return; activeTag.value = tag; pageNum.value = 1; await loadArticles(); };
const loadMore = async () => { if (!hasMore.value || loadingMore.value) return; pageNum.value += 1; await loadArticles({ append: true }); };

watch(() => route.query.id, async (value) => {
  const articleId = Number(value);
  if (Number.isFinite(articleId) && articleId > 0) { await loadArticleDetail(articleId); return; }
  detailVisible.value = false;
  activeArticle.value = null;
}, { immediate: true });

onMounted(async () => { currentUser.value = getCurrentUser(); await Promise.all([loadArticles(), loadDashboard()]); });
</script>

<style scoped>
.news-page { display: grid; gap: 24px; padding: 24px; min-height: calc(100vh - 64px); background: linear-gradient(180deg, var(--heritage-paper-soft), var(--heritage-paper)); }
.heritage-float-card { border-radius: 28px; background: var(--heritage-glass-bg); border: 1px solid var(--heritage-glass-border); box-shadow: var(--heritage-glass-shadow); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); }
.hero, .toolbar, .news-column, .sidebar-panel { padding: 24px; }
.hero { display: grid; gap: 20px; grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr); }
.hero h2, .panel-head h3, .dialog-header h3 { margin: 0; font-size: 32px; font-weight: 800; color: transparent; background: linear-gradient(135deg, var(--heritage-primary), var(--heritage-ink)); -webkit-background-clip: text; background-clip: text; }
.hero p, .panel-head p, .dialog-header p, .media-head p { margin: 12px 0 0; color: var(--heritage-ink-soft); line-height: 1.8; }
.section-kicker, .media-chip { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 999px; background: rgba(164, 59, 47, 0.1); color: var(--heritage-primary); font-size: 12px; font-weight: 700; }
.hero-stats { display: grid; gap: 14px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.stat-card { display: grid; gap: 8px; padding: 18px; border-radius: 20px; background: rgba(255, 255, 255, 0.72); border: 1px solid rgba(164, 59, 47, 0.08); }
.stat-card span { color: var(--heritage-muted); font-size: 13px; }
.stat-card strong { font-size: 28px; color: var(--heritage-ink); }
.toolbar { display: flex; gap: 16px; align-items: center; }
.toolbar-actions, .tag-row, .news-actions, .admin-actions, .dialog-footer, .media-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.tag-row { margin-top: -8px; }
.tag-chip { padding: 8px 16px; border-radius: 999px; border: 1px solid rgba(164, 59, 47, 0.16); background: rgba(255, 255, 255, 0.72); color: var(--heritage-ink-soft); cursor: pointer; }
.tag-chip.active, .tag-chip:hover { border-color: transparent; background: linear-gradient(135deg, var(--heritage-primary), var(--heritage-primary-soft)); color: #fff; }
.layout { display: grid; gap: 24px; grid-template-columns: minmax(0, 1.75fr) minmax(280px, 0.9fr); align-items: start; }
.news-list, .sidebar { display: grid; gap: 20px; }
.panel-head { display: flex; justify-content: space-between; gap: 24px; align-items: flex-end; margin-bottom: 24px; }
.news-card { display: flex; gap: 20px; padding: 22px; border-radius: 22px; background: rgba(255, 255, 255, 0.7); border: 1px solid rgba(164, 59, 47, 0.08); }
.news-date { display: grid; place-items: center; min-width: 86px; height: 86px; border-radius: 20px; background: linear-gradient(135deg, var(--heritage-primary), #dd9a54); color: #fff; }
.day { font-size: 30px; font-weight: 800; line-height: 1; }
.month-year { font-size: 12px; }
.news-copy { flex: 1; min-width: 0; display: grid; gap: 10px; }
.news-copy h4 { margin: 0; font-size: 22px; line-height: 1.45; color: var(--heritage-ink); }
.news-copy p, .metric-chip span, .hot-card span { margin: 0; color: var(--heritage-ink-soft); line-height: 1.8; }
.news-meta, .news-views { display: flex; gap: 10px; align-items: center; color: var(--heritage-muted); font-size: 13px; flex-wrap: wrap; }
.read-more { display: inline-flex; align-items: center; gap: 6px; padding: 0; border: none; background: transparent; color: var(--heritage-primary); font-weight: 700; cursor: pointer; }
.news-image { width: 220px; height: 148px; border-radius: 18px; overflow: hidden; flex-shrink: 0; }
.news-image img, .detail-image, .preview-image, .preview-video { width: 100%; height: 100%; object-fit: cover; }
.load-more { display: flex; justify-content: center; margin-top: 28px; }
.metric-list { display: grid; gap: 12px; margin-top: 16px; }
.metric-chip { width: 100%; text-align: left; padding: 14px 16px; border-radius: 18px; border: 1px solid rgba(164, 59, 47, 0.08); background: rgba(255, 255, 255, 0.72); cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.hot-card { align-items: flex-start; flex-direction: column; }
.detail-body { min-height: 220px; }
.detail-image { height: 320px; margin-bottom: 20px; border-radius: 22px; }
.detail-summary { margin: 0 0 18px; padding: 18px 20px; border-radius: 18px; background: rgba(164, 59, 47, 0.06); color: var(--heritage-ink); line-height: 1.8; }
.detail-video { width: 100%; margin-bottom: 18px; border-radius: 18px; background: #000; }
.detail-content { display: grid; gap: 14px; color: var(--heritage-ink-soft); line-height: 1.9; }
.detail-content p { margin: 0; }
.editor-form { display: grid; gap: 4px; }
.editor-grid, .media-grid { display: grid; gap: 16px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.media-card { display: grid; gap: 14px; padding: 18px; border-radius: 24px; background: rgba(255, 255, 255, 0.7); border: 1px solid rgba(164, 59, 47, 0.08); }
.media-head h4 { margin: 10px 0 0; font-size: 20px; color: var(--heritage-ink); }
.media-uploader, .media-uploader :deep(.el-upload) { width: 100%; display: block; }
.upload-box { display: grid; place-items: center; width: 100%; min-height: 240px; border-radius: 20px; border: 1px dashed rgba(164, 59, 47, 0.22); background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(245, 239, 231, 0.9)); overflow: hidden; }
.upload-box.uploading { border-color: rgba(164, 59, 47, 0.42); }
.upload-placeholder { display: grid; gap: 10px; justify-items: center; padding: 20px; text-align: center; color: var(--heritage-ink-soft); }
.upload-placeholder .el-icon { font-size: 34px; color: var(--heritage-primary); }
.upload-placeholder strong { color: var(--heritage-ink); font-size: 16px; }
@media (max-width: 1100px) { .hero, .layout, .editor-grid, .media-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .news-page { padding: 16px; } .hero, .toolbar, .news-column, .sidebar-panel, .news-card { padding: 20px 16px; } .toolbar, .panel-head, .news-card { flex-direction: column; align-items: stretch; } .hero-stats { grid-template-columns: 1fr; } .news-image, .detail-image { width: 100%; height: 220px; } }
</style>
