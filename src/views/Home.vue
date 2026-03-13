<template>
  <div class="home-page">
    <!-- 数据统计面板 - 顶部 -->
    <section class="content-section overview-section heritage-float-card">
      <div class="section-header">
      </div>

      <HomeStatsPanel :current-count="projects.length" :total-count="total" :average-views="averageViews"
        :level-stats="levelStats" :status-stats="statusStats" :hot-projects="hotProjects"
        @open-project="openProjectById" />
    </section>

    <!-- 搜索与项目展示整合区域 -->
    <section class="content-section heritage-float-card">
      <!-- 搜索头部 -->
      <div class="search-header">
        <div class="search-title">
          <span class="section-kicker">非遗项目</span>
          <h2>搜索与探索</h2>
          <p>搜索非遗项目、传承人或关键词，探索丰富的非物质文化遗产资源</p>
        </div>
      </div>

      <div class="search-main">
        <!-- 主搜索栏 -->
        <div class="search-bar">
          <div class="search-input-wrapper">
            <el-input v-model="keyword" placeholder="搜索非遗项目、传承人或关键词..." clearable @keyup.enter="handleSearch"
              @clear="handleSearch" class="search-input">
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
            </el-input>
            <el-button type="primary" :loading="searchLoading" @click="handleSearch" class="search-button">
              <el-icon v-if="!searchLoading">
                <Search />
              </el-icon>
              <span>{{ searchLoading ? '搜索中...' : '搜索' }}</span>
            </el-button>
          </div>
        </div>

        <!-- 筛选选项 -->
        <div class="search-filters">
          <div class="filter-group">
            <span class="filter-label">保护级别：</span>
            <el-select v-model="protectLevel" placeholder="全部级别" clearable @change="handleSearch" class="filter-select">
              <el-option label="国家级" value="国家级" />
              <el-option label="省级" value="省级" />
              <el-option label="市级" value="市级" />
              <el-option label="县级" value="县级" />
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">项目状态：</span>
            <el-select v-model="projectStatus" placeholder="全部状态" clearable @change="handleSearch"
              class="filter-select">
              <el-option label="在传承" value="在传承" />
              <el-option label="濒危" value="濒危" />
              <el-option label="已失传" value="已失传" />
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">分类：</span>
            <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" class="filter-select">
              <el-option v-for="cat in CATEGORY_OPTIONS" :key="cat.value" :label="cat.label" :value="cat.value" />
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button @click="resetFilters" class="reset-button">
              <el-icon>
                <Refresh />
              </el-icon>
              重置
            </el-button>
            <el-button v-if="isAdmin" type="success" @click="openAddDialog" class="add-button">
              <el-icon>
                <Plus />
              </el-icon>
              新增项目
            </el-button>
          </div>
        </div>

        <!-- 搜索历史 -->
        <div v-if="showSearchHistory && searchHistory.length > 0" class="search-history">
          <div class="history-header">
            <span class="history-label">
              <el-icon>
                <Clock />
              </el-icon>
              搜索历史
            </span>
            <el-button type="danger" link size="small" @click="handleClearSearchHistory">
              <el-icon>
                <Delete />
              </el-icon>
              清空历史
            </el-button>
          </div>
          <div class="history-tags">
            <el-tag v-for="(item, index) in searchHistory" :key="index" size="small" class="history-tag"
              @click="handleSelectSearchHistory(item)">
              {{ item }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 结果信息 -->
      <div class="results-info">
        <span class="results-count">
          <el-icon>
            <Document />
          </el-icon>
          共 {{ total }} 个项目
        </span>
        <span v-if="keyword" class="search-keyword">
          搜索关键词："{{ keyword }}"
        </span>
      </div>

      <!-- 加载状态 -->
      <div v-loading="loading" element-loading-text="正在加载项目列表..." element-loading-spinner="el-icon-loading">
        <!-- 无结果提示 -->
        <el-empty v-if="!loading && projects.length === 0" :description="emptyDescription" :image-size="120">
          <template v-if="keyword">
            <el-button type="primary" @click="resetFilters">清除筛选条件</el-button>
          </template>
        </el-empty>

        <!-- 项目列表 -->
        <div v-else class="project-grid">
          <HomeProjectCard v-for="item in projects" :key="item.id" :project="item" :is-admin="isAdmin"
            :selected="selectedIds.includes(Number(item.id))" :is-favorited="favoritedIds.includes(Number(item.id))"
            @open="openProject" @edit="openEditDialog" @delete="handleDeleteProject" @toggle-select="toggleSelection"
            @toggle-favorite="handleToggleFavorite" />
        </div>
      </div>

      <!-- 分页控件 -->
      <div v-if="projects.length > 0" class="pagination-row">
        <el-pagination background layout="total, prev, pager, next, jumper" :current-page="pageNum"
          :page-size="pageSize" :total="total" @current-change="handlePageChange" />
      </div>
    </section>

    <HomeProjectDialog :visible="detailVisible" :project="activeProject" :comments="comments"
      :comment-draft="commentDraft" :submitting-comment="commentLoading" :is-admin="isAdmin"
      :current-user-id="currentUser?.id"
      :is-favorited="activeProject ? favoritedIds.includes(Number(activeProject.id)) : false"
      @update:visible="handleDetailVisibilityChange" @update:comment-draft="commentDraft = $event"
      @submit-comment="handleSubmitComment" @delete-comment="handleDeleteComment"
      @toggle-favorite="activeProject?.id && handleToggleFavorite(activeProject.id)" />

    <HomeProjectEditorDialog v-if="isAdmin" v-model:visible="editorVisible" :form="editorForm"
      :inheritor-ids="editorInheritorIds" :categories="CATEGORY_OPTIONS" :protect-levels="PROTECT_LEVEL_OPTIONS"
      :inheritor-options="inheritorOptions" :upload-action="uploadAction" :upload-headers="uploadHeaders"
      :saving="savingProject" @update:inheritor-ids="editorInheritorIds = $event" @save="handleSaveProject"
      @upload-cover-success="handleUploadCoverSuccess" @upload-video-success="handleUploadVideoSuccess" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import { useRoute } from "vue-router";
import { Search, Refresh, Plus, Clock, Delete, Document } from '@element-plus/icons-vue';
import HomeProjectCard from "@/components/home/HomeProjectCard.vue";
import HomeProjectDialog from "@/components/home/HomeProjectDialog.vue";
import HomeProjectEditorDialog from "@/components/home/HomeProjectEditorDialog.vue";
import HomeStatsPanel from "@/components/home/HomeStatsPanel.vue";
import { CATEGORY_OPTIONS, PROTECT_LEVEL_OPTIONS } from "@/constants/heritage";
import { useHeritageHubStore } from "@/stores/heritageHub";
import {
  addComment,
  checkFavorite,
  clearSearchHistory,
  deleteComment,
  deleteProject,
  deleteProjectsBatch,
  fetchComments,
  fetchInheritorPage,
  fetchProjectDetail,
  fetchProjectPage,
  fetchStatisticsByLevel,
  fetchStatisticsByStatus,
  increaseProjectViewCount,
  recordSearchKeyword,
  saveProject,
  toggleFavorite,
} from "@/services/project";
import type { InheritorRecord } from "@/types/inheritor";
import type { HeritageProject, ProjectComment, StatisticItem } from "@/types/project";
import { getAverageViewCount } from "@/utils/heritage";
import { getCurrentUser, type SessionUser } from "@/utils/session";
import { confirmDangerAction, selectionRequiredText, showSuccess, showWarning, successText } from "@/utils/uiFeedback";
import { buildApiUrl, getAuthHeaders } from "@/utils/url";

interface UploadResponsePayload {
  code: number;
  data?: string;
  msg?: string;
}

const route = useRoute();

const pageNum = ref(1);
const pageSize = ref(6);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const editorVisible = ref(false);
const savingProject = ref(false);
const commentLoading = ref(false);

const keyword = ref("");
const protectLevel = ref("");
const projectStatus = ref("");
const categoryId = ref("");
const showSearchHistory = ref(false);
const searchLoading = ref(false);
const commentDraft = ref("");

const selectedIds = ref<number[]>([]);
const projects = ref<HeritageProject[]>([]);
const levelStats = ref<StatisticItem[]>([]);
const statusStats = ref<StatisticItem[]>([]);
const comments = ref<ProjectComment[]>([]);
const inheritorOptions = ref<Array<{ value: number; label: string }>>([]);
const activeProject = ref<HeritageProject | null>(null);

const currentUser = ref<SessionUser | null>(getCurrentUser());
const isAdmin = computed(() => currentUser.value?.role === "admin");
const emptyDescription = computed(() =>
  keyword.value ? `未找到与"${keyword.value}"相关的项目` : '当前筛选下还没有找到非遗项目'
);
const hubStore = useHeritageHubStore();
const { hotProjects, projectFavoriteIds: favoritedIds, searchHistory } = storeToRefs(hubStore);
const averageViews = computed(() => getAverageViewCount(projects.value as Array<{ viewCount?: number | null }>));
const uploadAction = buildApiUrl("/file/upload");
const uploadHeaders = getAuthHeaders() as Record<string, string>;

const createEmptyProject = (): HeritageProject => ({
  name: "",
  categoryId: CATEGORY_OPTIONS[0]?.value,
  regionId: undefined,
  protectLevel: PROTECT_LEVEL_OPTIONS[0]?.value,
  status: "在传承",
  history: "",
  features: "",
  coverUrl: "",
  videoUrl: "",
  inheritorIds: [],
});

const editorForm = reactive<HeritageProject>(createEmptyProject());
const editorInheritorIds = ref<number[]>([]);

const syncProjectViewCount = (projectId: number, viewCount: number) => {
  projects.value = projects.value.map((item) =>
    Number(item.id) === projectId
      ? {
        ...item,
        viewCount,
      }
      : item
  );

  if (Number(activeProject.value?.id) === projectId && activeProject.value) {
    activeProject.value = {
      ...activeProject.value,
      viewCount,
    };
  }

  hubStore.setProjectViewCount(projectId, viewCount, activeProject.value);
};

const loadProjects = async () => {
  loading.value = true;
  try {
    const res = await fetchProjectPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: keyword.value.trim() || undefined,
      protectLevel: protectLevel.value || undefined,
      status: projectStatus.value || undefined,
      categoryId: categoryId.value || undefined,
    });
    const page = res.data.data;
    projects.value = page.records || [];
    total.value = Number(page.total || 0);
    selectedIds.value = [];
  } catch (error) {
    console.error("Failed to load projects", error);
  } finally {
    loading.value = false;
  }
};

const loadOverviewPanels = async () => {
  const [levelRes, statusRes] = await Promise.allSettled([
    fetchStatisticsByLevel(),
    fetchStatisticsByStatus(),
  ]);

  if (levelRes.status === "fulfilled") {
    levelStats.value = levelRes.value.data.data || [];
  }
  if (statusRes.status === "fulfilled") {
    statusStats.value = statusRes.value.data.data || [];
  }
};

const loadInheritorOptions = async () => {
  if (!isAdmin.value) {
    inheritorOptions.value = [];
    return;
  }

  try {
    const pageSize = 200;
    let pageNum = 1;
    let total = 0;
    const options: Array<{ value: number; label: string }> = [];

    do {
      const res = await fetchInheritorPage({ pageNum, pageSize });
      const page = res.data.data;
      total = Number(page.total || 0);
      const records = (page.records || []) as InheritorRecord[];
      for (const item of records) {
        if (item.id != null && item.name) {
          options.push({
            value: Number(item.id),
            label: item.name,
          });
        }
      }
      pageNum += 1;
    } while (options.length < total && pageNum <= 20);

    inheritorOptions.value = options;
  } catch (error) {
    console.error("Failed to load inheritor options", error);
    inheritorOptions.value = [];
    ElMessage.error("传承人选项加载失败");
  }
};

const loadComments = async (projectId: number) => {
  try {
    const res = await fetchComments(projectId);
    comments.value = res.data.data || [];
  } catch (error) {
    console.error("Failed to load comments", error);
    comments.value = [];
    ElMessage.error("评论加载失败");
  }
};

const handleDetailVisibilityChange = (value: boolean) => {
  detailVisible.value = value;
  if (value) {
    return;
  }

  activeProject.value = null;
  comments.value = [];
  commentDraft.value = "";
};

const openProjectById = async (id: number) => {
  try {
    const res = await fetchProjectDetail(id);
    activeProject.value = res.data.data;
    detailVisible.value = true;
    commentDraft.value = "";
    await loadComments(id);
    await refreshFavoriteState(id);

    try {
      const viewCountRes = await increaseProjectViewCount(id);
      const nextViewCount = Number(
        viewCountRes.data.data ?? Number(activeProject.value?.viewCount || 0) + 1
      );
      syncProjectViewCount(id, nextViewCount);
      await Promise.allSettled([loadOverviewPanels(), hubStore.refreshOverview()]);
    } catch (error) {
      console.error("Failed to increase view count", error);
    }
  } catch (error) {
    console.error("Failed to load project detail", error);
    ElMessage.error("加载项目详情失败");
  }
};

const openProject = async (project: HeritageProject) => {
  if (!project.id) {
    return;
  }
  await openProjectById(Number(project.id));
};

const refreshFavoriteState = async (projectId: number) => {
  try {
    const res = await checkFavorite(projectId);
    hubStore.setProjectFavorite(projectId, Boolean(res.data.data));
  } catch (error) {
    console.error("Failed to refresh favorite state", error);
  }
};

const handleSearch = async () => {
  // 验证搜索查询
  if (keyword.value && keyword.value.trim().length === 0) {
    showWarning("请输入搜索关键词");
    return;
  }

  searchLoading.value = true;
  pageNum.value = 1;

  try {
    if (keyword.value.trim()) {
      try {
        await recordSearchKeyword(keyword.value.trim());
        hubStore.prependSearchKeyword(keyword.value.trim());
        await hubStore.syncSearchHistory();
      } catch (error) {
        console.error("Failed to record search keyword", error);
      }
    }
    await loadProjects();
  } finally {
    searchLoading.value = false;
  }
};

const resetFilters = async () => {
  keyword.value = "";
  protectLevel.value = "";
  projectStatus.value = "";
  categoryId.value = "";
  pageNum.value = 1;
  await loadProjects();
  showSuccess("已重置所有筛选条件");
};

const handleSelectSearchHistory = async (value: string) => {
  keyword.value = value;
  showSearchHistory.value = false;
  await handleSearch();
};

const handleClearSearchHistory = async () => {
  await clearSearchHistory();
  hubStore.clearSearchHistoryState();
  showSuccess(successText.cleared("搜索历史"));
};

const handlePageChange = async (value: number) => {
  pageNum.value = value;
  await loadProjects();
};

const toggleSelection = (id?: number) => {
  if (!id) {
    return;
  }
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id);
    return;
  }
  selectedIds.value = [...selectedIds.value, id];
};

const handleToggleFavorite = async (id?: number) => {
  if (!id) {
    return;
  }
  try {
    const res = await toggleFavorite(id);
    const added = Boolean(res.data.data);
    const linkedProject =
      (activeProject.value?.id === id ? activeProject.value : null) ||
      projects.value.find((item) => Number(item.id) === id) ||
      null;
    hubStore.setProjectFavorite(id, added, linkedProject);
    showSuccess(added ? "已加入收藏" : "已取消收藏");
  } catch (error) {
    console.error("Failed to toggle favorite", error);
  }
};

const handleSubmitComment = async () => {
  if (!activeProject.value?.id || !commentDraft.value.trim()) {
    showWarning("请输入评论内容");
    return;
  }
  commentLoading.value = true;
  try {
    await addComment({
      projectId: Number(activeProject.value.id),
      content: commentDraft.value.trim(),
    });
    commentDraft.value = "";
    await loadComments(Number(activeProject.value.id));
    showSuccess(successText.published("评论"));
  } catch (error) {
    console.error("Failed to submit comment", error);
  } finally {
    commentLoading.value = false;
  }
};

const handleDeleteComment = async (id: number) => {
  await deleteComment(id);
  if (activeProject.value?.id) {
    await loadComments(Number(activeProject.value.id));
  }
  showSuccess(successText.deleted("评论"));
};

const resetEditorForm = () => {
  Object.assign(editorForm, createEmptyProject());
  editorInheritorIds.value = [];
};

const openAddDialog = () => {
  resetEditorForm();
  editorVisible.value = true;
};

const openEditDialog = (project: HeritageProject) => {
  Object.assign(editorForm, {
    ...createEmptyProject(),
    ...project,
  });
  editorInheritorIds.value = [...(project.inheritorIds || [])];
  editorVisible.value = true;
};

const handleUploadCoverSuccess = (response: UploadResponsePayload) => {
  if (response.code === 200 && response.data) {
    editorForm.coverUrl = response.data;
    showSuccess(successText.uploaded("封面"));
    return;
  }
  ElMessage.error(response.msg || "封面上传失败");
};

const handleUploadVideoSuccess = (response: UploadResponsePayload) => {
  if (response.code === 200 && response.data) {
    editorForm.videoUrl = response.data;
    showSuccess(successText.uploaded("视频"));
    return;
  }
  ElMessage.error(response.msg || "视频上传失败");
};

const handleSaveProject = async () => {
  if (!editorForm.name?.trim()) {
    showWarning("请输入项目名称");
    return;
  }
  savingProject.value = true;
  try {
    await saveProject({
      ...editorForm,
      inheritorIds: editorInheritorIds.value,
    });
    editorVisible.value = false;
    await Promise.allSettled([loadProjects(), loadOverviewPanels(), hubStore.refreshOverview()]);
    showSuccess(successText.saved("项目"));
  } catch (error) {
    console.error("Failed to save project", error);
  } finally {
    savingProject.value = false;
  }
};

const handleDeleteProject = async (id?: number) => {
  if (!id) {
    return;
  }

  try {
    await confirmDangerAction({ subject: "这个项目" });
  } catch {
    return;
  }

  try {
    await deleteProject(id);
    hubStore.removeProject(id);
    if (Number(activeProject.value?.id) === id) {
      handleDetailVisibilityChange(false);
    }
    showSuccess(successText.deleted("项目"));
    await Promise.allSettled([loadProjects(), loadOverviewPanels(), hubStore.refreshOverview()]);
  } catch (error) {
    console.error("Failed to delete project", error);
  }
};

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    showWarning(selectionRequiredText("项目", "删除"));
    return;
  }

  try {
    await confirmDangerAction({ subject: `选中的 ${selectedIds.value.length} 个项目` });
  } catch {
    return;
  }

  try {
    await deleteProjectsBatch(selectedIds.value);
    selectedIds.value.forEach((selectedId) => hubStore.removeProject(selectedId));
    if (activeProject.value?.id && selectedIds.value.includes(Number(activeProject.value.id))) {
      handleDetailVisibilityChange(false);
    }
    showSuccess(successText.batchDeleted("项目"));
    await Promise.allSettled([loadProjects(), loadOverviewPanels(), hubStore.refreshOverview()]);
  } catch (error) {
    console.error("Failed to batch delete projects", error);
  }
};

const csvEscape = (value: unknown) => `"${String(value ?? "").replace(/"/g, '""')}"`;

const exportCurrentPage = () => {
  const header = ["项目名称", "类别", "地区", "保护级别", "传承状态", "传承人", "浏览量"];
  const rows = projects.value.map((item) => [
    item.name,
    item.categoryName || "",
    item.regionName || "",
    item.protectLevel || "",
    item.status || "",
    item.inheritorNames || "",
    item.viewCount || 0,
  ]);
  const csv = [header, ...rows].map((row) => row.map(csvEscape).join(",")).join("\r\n");
  const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8;" });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `heritage-projects-page-${pageNum.value}.csv`;
  link.click();
  window.URL.revokeObjectURL(url);
};

watch(
  () => route.query.id,
  async (value) => {
    const projectId = Number(value);
    if (Number.isFinite(projectId) && projectId > 0) {
      await openProjectById(projectId);
      return;
    }
    handleDetailVisibilityChange(false);
  },
  { immediate: true }
);

onMounted(async () => {
  currentUser.value = getCurrentUser();
  await Promise.allSettled([
    loadProjects(),
    loadOverviewPanels(),
    hubStore.ensureOverview(),
    hubStore.syncProjectFavoriteIds(),
    hubStore.syncSearchHistory(),
    loadInheritorOptions(),
  ]);
});
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  position: relative;
  overflow: hidden;
}

.home-page::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(164, 59, 47, 0.03) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.home-page::after {
  content: '';
  position: absolute;
  bottom: -40%;
  left: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(59, 87, 121, 0.03) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.content-section {
  border-radius: 32px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.04),
    0 2px 8px rgba(0, 0, 0, 0.02),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

.content-section:hover {
  box-shadow:
    0 12px 48px rgba(0, 0, 0, 0.06),
    0 4px 16px rgba(0, 0, 0, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.overview-section {
  display: grid;
  gap: 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  align-items: flex-start;
  margin-bottom: 32px;
  position: relative;
}

.section-header::after {
  content: '';
  position: absolute;
  bottom: -16px;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg,
      rgba(164, 59, 47, 0.3) 0%,
      rgba(164, 59, 47, 0.1) 50%,
      transparent 100%);
  border-radius: 2px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(164, 59, 47, 0.08), rgba(164, 59, 47, 0.12));
  color: var(--heritage-primary);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(164, 59, 47, 0.15);
  animation: pulse-glow 3s ease-in-out infinite;
}

@keyframes pulse-glow {

  0%,
  100% {
    box-shadow: 0 2px 8px rgba(164, 59, 47, 0.15);
  }

  50% {
    box-shadow: 0 2px 16px rgba(164, 59, 47, 0.25);
  }
}

.section-header h2 {
  margin: 0 0 8px 0;
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: transparent;
  background: linear-gradient(135deg,
      #a43b2f 0%,
      #8b2f25 25%,
      #5a5765 75%,
      #3b5779 100%);
  -webkit-background-clip: text;
  background-clip: text;
  background-size: 200% 100%;
  animation: gradient-shift 8s ease infinite;
  line-height: 1.2;
}

@keyframes gradient-shift {

  0%,
  100% {
    background-position: 0% 50%;
  }

  50% {
    background-position: 100% 50%;
  }
}

.section-header p {
  margin: 0;
  max-width: 560px;
  color: #6b7280;
  line-height: 1.8;
  font-size: 15px;
  font-weight: 400;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-top: 24px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-top: 32px;
  border-top: 2px solid rgba(229, 231, 235, 0.6);
}

.pagination-row :deep(.el-pagination) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-row :deep(.el-pagination__total) {
  color: #6b7280;
  font-weight: 500;
  font-size: 14px;
}

.pagination-row :deep(.el-pagination button) {
  border-radius: 8px;
  transition: all 0.2s ease;
}

.pagination-row :deep(.el-pagination button:hover) {
  background: linear-gradient(135deg, #a43b2f 0%, #8b2f25 100%);
  color: white;
  transform: translateY(-1px);
}

.pagination-row :deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  min-width: 36px;
  height: 36px;
  line-height: 36px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.pagination-row :deep(.el-pagination .el-pager li:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(164, 59, 47, 0.2);
}

.pagination-row :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #a43b2f 0%, #8b2f25 100%);
  color: white;
  box-shadow:
    0 4px 16px rgba(164, 59, 47, 0.3),
    0 2px 8px rgba(164, 59, 47, 0.2);
  transform: translateY(-2px);
}

.pagination-row :deep(.el-pagination .btn-prev),
.pagination-row :deep(.el-pagination .btn-next) {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 1.5px solid rgba(229, 231, 235, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pagination-row :deep(.el-pagination .btn-prev:hover),
.pagination-row :deep(.el-pagination .btn-next:hover) {
  background: linear-gradient(135deg, #a43b2f 0%, #8b2f25 100%);
  border-color: transparent;
}

.search-section {
  margin-bottom: 0;
}

.search-header {
  margin-bottom: 32px;
}

.search-title {
  text-align: center;
}

.search-title h2 {
  margin: 12px 0 8px;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: transparent;
  background: linear-gradient(135deg,
      #a43b2f 0%,
      #8b2f25 25%,
      #5a5765 75%,
      #3b5779 100%);
  -webkit-background-clip: text;
  background-clip: text;
  background-size: 200% 100%;
  animation: gradient-shift 8s ease infinite;
}

.search-title p {
  margin: 0;
  color: #6b7280;
  font-size: 15px;
  line-height: 1.7;
}

.search-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-bar {
  width: 100%;
}

.search-input-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  padding: 14px 20px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 1px 3px rgba(0, 0, 0, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 1.5px solid rgba(229, 231, 235, 0.8);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 6px 24px rgba(0, 0, 0, 0.08),
    0 2px 6px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  border-color: rgba(164, 59, 47, 0.3);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 4px rgba(164, 59, 47, 0.1),
    0 8px 32px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border-color: var(--heritage-primary);
}

.search-button {
  height: 52px;
  padding: 0 40px;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #a43b2f 0%, #8b2f25 100%);
  border: none;
  box-shadow:
    0 4px 16px rgba(164, 59, 47, 0.3),
    0 2px 8px rgba(164, 59, 47, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.search-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.2) 50%,
      transparent 100%);
  transition: left 0.6s ease;
}

.search-button:hover::before {
  left: 100%;
}

.search-button:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(164, 59, 47, 0.4),
    0 4px 12px rgba(164, 59, 47, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.search-button:active {
  transform: translateY(0);
  box-shadow:
    0 2px 8px rgba(164, 59, 47, 0.3),
    0 1px 4px rgba(164, 59, 47, 0.2);
}

.search-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
  padding-top: 24px;
  margin-top: 8px;
  background: linear-gradient(180deg,
      rgba(249, 250, 251, 0.5) 0%,
      transparent 100%);
  border-top: 1px solid rgba(229, 231, 235, 0.6);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.2s ease;
}

.filter-group:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-label {
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.filter-select {
  width: 150px;
}

.filter-select :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 1.5px solid rgba(229, 231, 235, 0.8);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  transition: all 0.2s ease;
}

.filter-select :deep(.el-input__wrapper:hover) {
  border-color: rgba(164, 59, 47, 0.3);
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.filter-select :deep(.el-input__wrapper.is-focus) {
  border-color: var(--heritage-primary);
  box-shadow:
    0 0 0 3px rgba(164, 59, 47, 0.1),
    0 4px 16px rgba(0, 0, 0, 0.08);
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

.reset-button,
.add-button {
  border-radius: 12px;
  padding: 10px 20px;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.reset-button {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 1.5px solid rgba(229, 231, 235, 0.8);
  color: #374151;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.reset-button:hover {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  border-color: rgba(164, 59, 47, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.add-button {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: white;
  box-shadow:
    0 4px 16px rgba(16, 185, 129, 0.3),
    0 2px 8px rgba(16, 185, 129, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.add-button:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-2px);
  box-shadow:
    0 6px 24px rgba(16, 185, 129, 0.4),
    0 4px 12px rgba(16, 185, 129, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.search-history {
  padding-top: 20px;
  margin-top: 8px;
  background: linear-gradient(180deg,
      rgba(249, 250, 251, 0.4) 0%,
      transparent 100%);
  border-top: 1px solid rgba(229, 231, 235, 0.5);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
  font-weight: 600;
  letter-spacing: 0.3px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(243, 244, 246, 0.6);
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 1px solid rgba(229, 231, 235, 0.8);
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.history-tag:hover {
  transform: translateY(-3px) scale(1.02);
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  border-color: rgba(164, 59, 47, 0.3);
  box-shadow:
    0 6px 16px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(164, 59, 47, 0.1);
}

.history-tag:active {
  transform: translateY(-1px) scale(1);
}

.results-info {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #6b7280;
  padding: 16px 24px;
  margin: 24px 0;
  border-radius: 12px;
  background: linear-gradient(135deg,
      rgba(249, 250, 251, 0.6) 0%,
      rgba(243, 244, 246, 0.4) 100%);
  border: 1px solid rgba(229, 231, 235, 0.5);
  animation: fade-in 0.5s ease-out;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.results-count {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
  font-size: 15px;
}

.search-keyword {
  color: var(--heritage-primary);
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(164, 59, 47, 0.08);
  font-size: 13px;
  animation: pulse-subtle 2s ease-in-out infinite;
}

@keyframes pulse-subtle {

  0%,
  100% {
    background: rgba(164, 59, 47, 0.08);
  }

  50% {
    background: rgba(164, 59, 47, 0.15);
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 12px;
  }

  .content-section {
    padding: 24px 20px;
    border-radius: 24px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .section-header h2 {
    font-size: 26px;
  }

  .section-header p {
    font-size: 14px;
  }

  .search-title h2 {
    font-size: 24px;
  }

  .search-title p {
    font-size: 14px;
  }

  .search-input-wrapper {
    flex-direction: column;
    gap: 12px;
  }

  .search-input {
    width: 100%;
  }

  .search-button {
    width: 100%;
    justify-content: center;
    height: 48px;
  }

  .search-filters {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding-top: 16px;
  }

  .filter-group {
    width: 100%;
    justify-content: space-between;
  }

  .filter-select {
    flex: 1;
    width: auto;
  }

  .filter-actions {
    margin-left: 0;
    width: 100%;
    justify-content: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .reset-button,
  .add-button {
    width: 100%;
    justify-content: center;
  }

  .results-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 16px;
  }

  .project-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }

  .pagination-row {
    margin-top: 32px;
    padding-top: 24px;
  }

  .pagination-row :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .content-section {
    padding: 20px 16px;
    border-radius: 20px;
  }

  .section-header h2 {
    font-size: 22px;
  }

  .search-title h2 {
    font-size: 20px;
  }

  .filter-label {
    font-size: 13px;
  }

  .history-label {
    font-size: 13px;
  }

  .project-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
