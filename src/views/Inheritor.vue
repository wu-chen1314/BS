<template>
  <div class="admin-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">INHERITOR ADMIN WORKSPACE</p>
        <h1>传承人管理工作台</h1>
        <p class="page-desc">
          把项目筛选、关键词搜索、批量操作和资料编辑统一放在一个更轻的管理界面里，方便沿着“项目关联 -> 传承人维护”这条链路连续处理。
        </p>
        <div class="hero-tags">
          <span class="pill">{{ activeFilterLabel }}</span>
          <span class="pill">{{ currentProjectId ? `项目过滤：${currentProjectLabel}` : "当前范围：全部项目" }}</span>
        </div>
      </div>

      <div class="hero-metrics">
        <article v-for="item in heroMetrics" :key="item.label" class="metric-card heritage-float-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.help }}</p>
        </article>
      </div>
    </section>

    <section class="workspace heritage-float-card">
      <div class="section-head">
        <div>
          <p class="section-kicker">INHERITOR DIRECTORY</p>
          <h2>传承人列表</h2>
          <p class="section-desc">支持按姓名检索、按项目上下文筛选、批量删除和直接编辑传承人资料。</p>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" round @click="openAddDialog">新增传承人</el-button>
          <el-button type="danger" :icon="Delete" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="filter-panel heritage-float-card">
        <div class="filter-block">
          <div class="filter-row">
            <el-input
              v-model="searchName"
              placeholder="搜索传承人姓名"
              clearable
              class="search-input"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button plain @click="resetKeyword">清空关键词</el-button>
          </div>
          <div v-if="currentProjectId" class="active-filter">
            <el-tag type="info" effect="plain">当前按项目筛选：{{ currentProjectLabel }}</el-tag>
          </div>
        </div>
      </div>

      <div class="table-shell" v-loading="loading" element-loading-text="正在加载传承人列表...">
        <el-table
          :data="tableData"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="56" align="center" />
          <el-table-column label="头像" width="96" align="center">
            <template #default="{ row }">
              <el-avatar
                shape="square"
                :size="52"
                :src="getAvatarUrl(row.avatarUrl) || fallbackAvatar"
              />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column prop="sex" label="性别" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.sex === '女' ? 'danger' : ''">{{ row.sex || "未填写" }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="传承级别" min-width="180" />
          <el-table-column prop="projectName" label="所属项目" min-width="180">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.projectName || "未关联项目" }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="简介" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑传承人' : '新增传承人'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" class="editor-form">
        <el-form-item label="头像">
          <div class="avatar-row">
            <el-upload
              class="avatar-uploader"
              :action="fileUploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <img v-if="form.avatarUrl" :src="getAvatarUrl(form.avatarUrl)" class="avatar-image" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <p class="field-tip">建议上传 1:1 方形头像，支持 JPG/PNG，大小不超过 5MB。</p>
          </div>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="传承级别">
          <el-select v-model="form.level" placeholder="请选择传承级别" class="full-width">
            <el-option
              v-for="item in levelOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="所属项目">
          <el-select v-model="form.projectId" placeholder="请选择所属项目" filterable class="full-width">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入传承人简介"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { Delete, Plus, Search } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { UploadProps } from "element-plus";
import {
  batchDeleteInheritors,
  deleteInheritor,
  fetchInheritorPage,
  fetchProjectOptions,
  saveInheritor,
} from "@/services/inheritor";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import type { InheritorRecord, ProjectOption } from "@/types/inheritor";
import { confirmDangerAction, selectionRequiredText, showSuccess, showWarning, successText } from "@/utils/uiFeedback";
import { buildApiUrl, buildStaticUrl, getAuthHeaders } from "@/utils/url";

interface UploadResponsePayload {
  code: number;
  data?: string;
  msg?: string;
}

const route = useRoute();
const fallbackAvatar = MATERIAL_PLACEHOLDERS.avatar;
const levelOptions = [
  "国家级非遗代表性传承人",
  "省级非遗代表性传承人",
  "市级非遗代表性传承人",
  "县级非遗代表性传承人",
];

const createEmptyForm = (): InheritorRecord => ({
  id: undefined,
  name: "",
  avatarUrl: "",
  sex: "男",
  level: "",
  description: "",
  projectId: undefined,
});

const fileUploadAction = buildApiUrl("/file/upload");
const uploadHeaders = getAuthHeaders();

const tableData = ref<InheritorRecord[]>([]);
const projectOptions = ref<ProjectOption[]>([]);
const selectedRows = ref<InheritorRecord[]>([]);
const currentProjectId = ref<number | undefined>(undefined);
const loading = ref(false);
const saving = ref(false);
const searchName = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);

const form = reactive<InheritorRecord>(createEmptyForm());

const currentProjectLabel = computed(() => {
  if (!currentProjectId.value) {
    return "";
  }

  const matched = projectOptions.value.find((item) => item.id === currentProjectId.value);
  return matched?.name || `项目 ID ${currentProjectId.value}`;
});
const activeFilterLabel = computed(() =>
  searchName.value.trim() ? `搜索中：${searchName.value.trim()}` : "当前关键词：全部"
);
const heroMetrics = computed(() => [
  {
    help: "当前筛选结果中的传承人总数",
    label: "结果总量",
    value: total.value,
  },
  {
    help: "当前分页展示的传承人数",
    label: "本页记录",
    value: tableData.value.length,
  },
  {
    help: "当前已勾选的批量操作对象数",
    label: "已选条目",
    value: selectedRows.value.length,
  },
  {
    help: "当前可关联的项目选项总数",
    label: "可选项目",
    value: projectOptions.value.length,
  },
]);

const syncCurrentProjectId = (value: unknown) => {
  const projectId = Number(value);
  currentProjectId.value = Number.isFinite(projectId) && projectId > 0 ? projectId : undefined;
};

const getAvatarUrl = (avatarUrl?: string | null) => buildStaticUrl(avatarUrl);

const applyForm = (payload?: InheritorRecord | null) => {
  Object.assign(form, createEmptyForm(), payload || {});
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await fetchInheritorPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value.trim() || undefined,
      projectId: currentProjectId.value,
    });

    const page = res.data.data;
    tableData.value = (page.records || []) as InheritorRecord[];
    total.value = Number(page.total || 0);
  } catch (error) {
    console.error("Failed to load inheritors", error);
    tableData.value = [];
    total.value = 0;
    ElMessage.error("传承人列表加载失败");
  } finally {
    loading.value = false;
  }
};

const fetchProjects = async () => {
  try {
    const res = await fetchProjectOptions();
    projectOptions.value = ((res.data.data || []) as ProjectOption[]).map((item) => ({
      id: Number(item.id),
      name: item.name,
    }));
  } catch (error) {
    console.error("Failed to load project options", error);
    projectOptions.value = [];
    ElMessage.error("项目列表加载失败");
  }
};

const handleSearch = async () => {
  pageNum.value = 1;
  await fetchData();
};

const resetKeyword = async () => {
  searchName.value = "";
  await handleSearch();
};

const openAddDialog = () => {
  applyForm({
    projectId: currentProjectId.value,
  });
  dialogVisible.value = true;
};

const handleEdit = (row: InheritorRecord) => {
  applyForm(row);
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.name?.trim()) {
    showWarning("请输入姓名");
    return;
  }
  if (!form.level) {
    showWarning("请选择传承级别");
    return;
  }
  if (!form.projectId) {
    showWarning("请选择所属项目");
    return;
  }

  saving.value = true;
  try {
    await saveInheritor({
      id: form.id,
      name: form.name.trim(),
      avatarUrl: form.avatarUrl || undefined,
      sex: form.sex || undefined,
      level: form.level,
      description: form.description?.trim() || undefined,
      projectId: form.projectId,
    });
    showSuccess(form.id ? successText.updated("传承人信息") : successText.created("传承人"));
    dialogVisible.value = false;
    await fetchData();
  } catch (error) {
    console.error("Failed to save inheritor", error);
    ElMessage.error("保存传承人失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (row: InheritorRecord) => {
  if (!row.id) {
    return;
  }

  try {
    await confirmDangerAction({ subject: `传承人「${row.name}」` });
    await deleteInheritor(Number(row.id));
    showSuccess(successText.deleted("传承人"));

    if (tableData.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1;
    }
    await fetchData();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to delete inheritor", error);
      ElMessage.error("删除传承人失败");
    }
  }
};

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    showWarning(selectionRequiredText("传承人", "删除"));
    return;
  }

  try {
    const deletingCount = selectedRows.value.length;
    const deletingAllVisibleRows = deletingCount === tableData.value.length;
    await confirmDangerAction({
      detail: "此操作不可撤销。",
      subject: `选中的 ${deletingCount} 位传承人`,
    });
    await batchDeleteInheritors(selectedRows.value.map((item) => Number(item.id)));
    showSuccess(successText.batchDeleted("传承人"));
    selectedRows.value = [];

    if (deletingAllVisibleRows && pageNum.value > 1) {
      pageNum.value -= 1;
    }
    await fetchData();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to batch delete inheritors", error);
      ElMessage.error("批量删除失败");
    }
  }
};

const handleSelectionChange = (selection: InheritorRecord[]) => {
  selectedRows.value = selection;
};

const handleAvatarSuccess: UploadProps["onSuccess"] = (response) => {
  const payload = response as UploadResponsePayload;
  if (payload.code === 200 && payload.data) {
    form.avatarUrl = payload.data;
    showSuccess(successText.uploaded("头像"));
    return;
  }
  ElMessage.error(payload.msg || "头像上传失败");
};

const beforeAvatarUpload: UploadProps["beforeUpload"] = (rawFile) => {
  if (rawFile.type !== "image/jpeg" && rawFile.type !== "image/png") {
    ElMessage.error("头像只能上传 JPG 或 PNG 图片");
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error("图片大小不能超过 5MB");
    return false;
  }
  return true;
};

watch(
  () => route.query.projectId,
  async (value) => {
    syncCurrentProjectId(value);
    pageNum.value = 1;
    await fetchData();
  }
);

onMounted(async () => {
  syncCurrentProjectId(route.query.projectId);
  await fetchProjects();
  await fetchData();
});
</script>

<style scoped>
.admin-page {
  min-height: calc(100vh - 88px);
  display: grid;
  gap: 18px;
  padding: 12px 6px 28px;
}

.hero,
.workspace {
  border-radius: 26px;
  border: 1px solid var(--heritage-border);
  background: rgba(255, 251, 244, 0.92);
  box-shadow: var(--heritage-card-shadow-rest);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(192, 138, 63, 0.14), transparent 32%),
    linear-gradient(135deg, rgba(255, 249, 241, 0.96), rgba(255, 255, 255, 0.98));
}

.page-kicker,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--heritage-gold);
}

.hero-copy h1,
.section-head h2 {
  margin: 0;
  color: var(--heritage-ink);
}

.page-desc,
.section-desc,
.metric-card p,
.field-tip {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-tags,
.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tags {
  margin-top: 20px;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(192, 138, 63, 0.12);
  color: var(--heritage-primary);
  font-size: 12px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.metric-card span {
  color: var(--heritage-muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 32px;
  color: var(--heritage-primary);
}

.workspace {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-panel {
  margin-top: 18px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.filter-block {
  display: grid;
  gap: 12px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: min(320px, 100%);
}

.active-filter {
  display: flex;
  align-items: center;
}

.table-shell {
  margin-top: 18px;
}

.table-shell :deep(.el-table) {
  border-radius: 22px;
  overflow: hidden;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.editor-form :deep(.full-width) {
  width: 100%;
}

.avatar-row {
  display: flex;
  gap: 14px;
  align-items: center;
}

.avatar-uploader {
  flex-shrink: 0;
}

.avatar-image,
.avatar-uploader-icon {
  width: 100px;
  height: 100px;
  border-radius: 12px;
}

.avatar-image {
  object-fit: cover;
  display: block;
}

.avatar-uploader-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #8c939d;
  border: 1px dashed var(--el-border-color);
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-page {
    padding: 10px 0 20px;
  }

  .hero,
  .workspace {
    padding: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .section-head,
  .filter-row,
  .avatar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
