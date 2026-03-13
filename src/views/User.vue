<template>
  <div class="admin-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">USER ADMIN WORKSPACE</p>
        <h1>用户管理工作台</h1>
        <p class="page-desc">
          把搜索、分页、编辑和风险操作统一收口到一个更轻的管理界面里，只让列表区域刷新，避免整页闪动和控制入口分散。
        </p>
        <div class="hero-tags">
          <span class="pill">{{ activeKeywordLabel }}</span>
          <span class="pill">{{ currentUser?.role === "admin" ? "管理员权限已启用" : "受限查看" }}</span>
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
          <p class="section-kicker">USER DIRECTORY</p>
          <h2>账号列表</h2>
          <p class="section-desc">支持按关键词检索、分页浏览、编辑信息、重置密码和删除账号。</p>
        </div>
        <div class="toolbar-actions">
          <el-button plain @click="fetchData">刷新列表</el-button>
          <el-button type="success" round @click="openAddDialog">新增用户</el-button>
        </div>
      </div>

      <div class="filter-panel heritage-float-card">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名、昵称或邮箱"
          clearable
          class="search-input"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button plain @click="resetFilters">重置</el-button>
      </div>

      <div class="table-shell" v-loading="loading" element-loading-text="正在加载用户列表...">
        <el-table :data="tableData" border stripe>
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="140" />
          <el-table-column prop="role" label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : 'success'">
                {{ row.role === "admin" ? "管理员" : "普通用户" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? "正常" : "禁用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                link
                type="warning"
                size="small"
                :disabled="isCurrentUser(row)"
                @click="handleResetPassword(row)"
              >
                重置密码
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                :disabled="isCurrentUser(row)"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="currentPage"
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          @current-change="fetchData"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑用户' : '新增用户'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="form" label-width="88px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="Boolean(form.id)" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role" :disabled="isEditingSelf">
            <el-radio value="user">普通用户</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status" :disabled="isEditingSelf">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱地址" />
        </el-form-item>
        <p v-if="isEditingSelf" class="dialog-tip">当前登录用户只能编辑自己的基础资料，不能在这里修改自己的角色或状态。</p>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { deleteUser, fetchUserPage, resetUserPassword, saveUser } from "@/services/user-management";
import type { UserRecord } from "@/types/user-management";
import { getCurrentUser, type SessionUser } from "@/utils/session";
import { confirmDangerAction, showSuccess, showWarning, successText } from "@/utils/uiFeedback";

const pageSize = 10;

const createEmptyForm = (): Required<Pick<UserRecord, "nickname" | "email" | "role" | "status">> &
  Pick<UserRecord, "id" | "username"> => ({
  id: undefined,
  username: "",
  nickname: "",
  email: "",
  role: "user",
  status: 1,
});

const tableData = ref<UserRecord[]>([]);
const total = ref(0);
const currentPage = ref(1);
const searchKeyword = ref("");
const dialogVisible = ref(false);
const loading = ref(false);
const saving = ref(false);
const currentUser = ref<SessionUser | null>(null);

const form = reactive(createEmptyForm());

const isEditingSelf = computed(
  () => form.id != null && currentUser.value?.id != null && Number(form.id) === Number(currentUser.value.id)
);
const activeKeywordLabel = computed(() =>
  searchKeyword.value.trim() ? `搜索中：${searchKeyword.value.trim()}` : "当前范围：全部用户"
);
const heroMetrics = computed(() => [
  {
    help: "当前查询结果的总记录数",
    label: "结果总量",
    value: total.value,
  },
  {
    help: "当前分页展示的用户数",
    label: "本页记录",
    value: tableData.value.length,
  },
  {
    help: "当前分页中的管理员数量",
    label: "管理员数",
    value: tableData.value.filter((item) => item.role === "admin").length,
  },
  {
    help: "当前所在分页位置",
    label: "分页位置",
    value: `P${currentPage.value}`,
  },
]);

const applyForm = (payload?: UserRecord | null) => {
  Object.assign(form, createEmptyForm(), payload || {});
};

const isCurrentUser = (user: UserRecord) =>
  currentUser.value?.id != null && user.id != null && Number(currentUser.value.id) === Number(user.id);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await fetchUserPage({
      pageNum: currentPage.value,
      pageSize,
      keyword: searchKeyword.value.trim() || undefined,
    });

    const page = res.data.data;
    tableData.value = (page.records || []) as UserRecord[];
    total.value = Number(page.total || 0);
  } catch (error) {
    console.error("Failed to load users", error);
    tableData.value = [];
    total.value = 0;
    ElMessage.error("用户列表加载失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchData();
};

const resetFilters = async () => {
  searchKeyword.value = "";
  await handleSearch();
};

const openAddDialog = () => {
  applyForm();
  dialogVisible.value = true;
};

const handleEdit = (row: UserRecord) => {
  applyForm(row);
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.id && !form.username.trim()) {
    showWarning("请输入用户名");
    return;
  }

  saving.value = true;
  try {
    await saveUser({
      id: form.id,
      username: form.username.trim() || undefined,
      nickname: form.nickname.trim() || undefined,
      email: form.email.trim() || undefined,
      role: form.role,
      status: form.status,
    });
    showSuccess(successText.saved("用户信息"));
    dialogVisible.value = false;
    await fetchData();
  } catch (error) {
    console.error("Failed to save user", error);
    ElMessage.error("保存用户失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (user: UserRecord) => {
  if (!user.id) {
    return;
  }

  if (isCurrentUser(user)) {
    showWarning("不能删除当前登录用户");
    return;
  }

  try {
    await confirmDangerAction({ subject: `用户「${user.username || user.nickname || user.id}」` });
    await deleteUser(Number(user.id));
    showSuccess(successText.deleted("用户"));

    if (tableData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1;
    }
    await fetchData();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to delete user", error);
      ElMessage.error("删除用户失败");
    }
  }
};

const handleResetPassword = async (user: UserRecord) => {
  if (!user.id) {
    return;
  }

  try {
    await confirmDangerAction({
      action: "重置",
      confirmButtonText: "确认重置",
      detail: "重置后默认密码将变为 123456。",
      subject: `用户「${user.username || user.nickname || user.id}」的密码`,
    });
    await resetUserPassword(Number(user.id));
    showSuccess("密码已重置，默认密码为 123456");
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to reset password", error);
      ElMessage.error("重置密码失败");
    }
  }
};

onMounted(async () => {
  currentUser.value = getCurrentUser();
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
    radial-gradient(circle at top left, rgba(192, 57, 43, 0.12), transparent 28%),
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
.dialog-tip {
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
  background: rgba(192, 57, 43, 0.08);
  color: var(--heritage-primary);
  font-size: 13px;
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
  color: var(--heritage-ink-soft);
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
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.search-input {
  width: min(360px, 100%);
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

  .hero-metrics,
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .filter-panel,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
