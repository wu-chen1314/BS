<template>
  <div class="profile-page">
    <section class="hero heritage-float-card">
      <div class="hero-copy">
        <p class="page-kicker">PROFILE WORKSPACE</p>
        <h1>个人资料中心</h1>
        <p class="page-desc">
          把基础资料和账户安全统一放到同一套轻量工作台里，减少切页负担，让头像、昵称和密码维护的节奏更清晰。
        </p>
        <div class="hero-tags">
          <span class="pill">{{ currentTabLabel }}</span>
          <span class="pill">{{ securityStatus }}</span>
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

    <section class="content-grid">
      <article class="workspace heritage-float-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">ACCOUNT SETTINGS</p>
            <h2>资料与安全</h2>
            <p class="section-desc">支持头像上传、昵称更新和密码修改，保存逻辑保持不变。</p>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基本资料" name="0">
            <el-form :model="form" label-width="100px" class="profile-form">
              <el-form-item label="我的头像">
                <el-upload
                  class="avatar-uploader"
                  :action="avatarUploadAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
                >
                  <img v-if="avatarUrl" :src="avatarUrl" class="avatar" />
                  <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                </el-upload>
              </el-form-item>

              <el-form-item label="登录账号">
                <el-input v-model="form.username" disabled />
              </el-form-item>

              <el-form-item label="用户昵称">
                <el-input v-model="form.nickname" placeholder="请输入昵称" />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="loading" @click="saveUserInfo">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="1">
            <el-form :model="passwordForm" label-width="100px" class="profile-form">
              <el-form-item label="原密码">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请确认新密码"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordLoading" @click="changePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </article>

      <aside class="summary-column">
        <article class="summary-card heritage-float-card">
          <div class="summary-user">
            <el-avatar :size="72" :src="avatarPreview" />
            <div>
              <strong>{{ form.nickname || "未设置昵称" }}</strong>
              <p>{{ form.username || "账号待加载" }}</p>
              <span>{{ sessionUser?.role === "admin" ? "管理员账号" : "普通用户账号" }}</span>
            </div>
          </div>
        </article>

        <article class="summary-card heritage-float-card">
          <div class="section-head compact">
            <div>
              <p class="section-kicker">SECURITY NOTE</p>
              <h2>安全提醒</h2>
            </div>
          </div>
          <div class="guide-list">
            <div class="guide-item">
              <strong>头像与昵称</strong>
              <p>资料更新后会同步刷新当前登录态，首页和侧边导航会立即显示新信息。</p>
            </div>
            <div class="guide-item">
              <strong>密码修改</strong>
              <p>密码修改成功后会要求重新登录，这是正常的安全收口行为。</p>
            </div>
            <div class="guide-item">
              <strong>上传建议</strong>
              <p>头像建议使用 JPG 或 PNG，方形图片在各个页面里的兼容性会更稳定。</p>
            </div>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { useHeritageHubStore } from "@/stores/heritageHub";
import request from "@/utils/request";
import { performLogout } from "@/utils/logout";
import { showSuccess, successText } from "@/utils/uiFeedback";
import { getCurrentUser, saveCurrentSession, type SessionUser } from "@/utils/session";
import { buildApiUrl, buildStaticUrl, getAuthHeaders } from "@/utils/url";

interface ProfileFormState {
  id: number | null;
  username: string;
  nickname: string;
  avatarUrl: string;
}

interface PasswordFormState {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

interface UploadResponsePayload {
  code: number;
  data?: string;
  msg?: string;
}

const router = useRouter();
const hubStore = useHeritageHubStore();

const activeTab = ref("0");
const loading = ref(false);
const passwordLoading = ref(false);
const sessionUser = ref<SessionUser | null>(null);
const avatarUploadAction = buildApiUrl("/file/upload/avatar");
const uploadHeaders = getAuthHeaders();

const form = reactive<ProfileFormState>({
  id: null,
  username: "",
  nickname: "",
  avatarUrl: "",
});

const passwordForm = reactive<PasswordFormState>({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const avatarUrl = computed(() => buildStaticUrl(form.avatarUrl));
const avatarPreview = computed(() => avatarUrl.value || MATERIAL_PLACEHOLDERS.avatar);
const currentTabLabel = computed(() => (activeTab.value === "0" ? "当前工作区：基本资料" : "当前工作区：密码安全"));
const profileCompleteness = computed(() => {
  const completed = [Boolean(form.username.trim()), Boolean(form.nickname.trim()), Boolean(form.avatarUrl.trim())].filter(Boolean).length;
  return Math.round((completed / 3) * 100);
});
const securityStatus = computed(() =>
  activeTab.value === "1" ? "密码修改后需重新登录" : "资料可随时更新"
);
const heroMetrics = computed(() => [
  {
    help: "账号资料字段的当前完整度",
    label: "资料完整度",
    value: `${profileCompleteness.value}%`,
  },
  {
    help: "当前登录账号的身份角色",
    label: "账号身份",
    value: sessionUser.value?.role === "admin" ? "管理员" : "普通用户",
  },
  {
    help: "头像资源是否已经配置",
    label: "头像状态",
    value: form.avatarUrl ? "已上传" : "待上传",
  },
  {
    help: "当前正在维护的设置模块",
    label: "当前页签",
    value: activeTab.value === "0" ? "基本资料" : "修改密码",
  },
]);

const syncSessionUser = (patch: Partial<SessionUser>) => {
  const current = sessionUser.value || getCurrentUser();
  if (!current) {
    return;
  }

  const nextUser: SessionUser = {
    ...current,
    ...patch,
  };
  saveCurrentSession(nextUser, current.token);
  sessionUser.value = nextUser;
};

const loadData = async (id: number) => {
  try {
    const res = await request.get(`/users/${id}`);
    const userData = res.data.data || {};
    Object.assign(form, {
      id: Number(userData.id || id),
      username: userData.username || "",
      nickname: userData.nickname || "",
      avatarUrl: userData.avatarUrl || userData.avatar || "",
    });
  } catch (error) {
    console.error("Failed to load profile", error);
    ElMessage.error("个人资料加载失败");
  }
};

const handleAvatarSuccess = (response: UploadResponsePayload) => {
  if (response.code !== 200 || !response.data) {
    ElMessage.error(response.msg || "上传失败");
    return;
  }

  form.avatarUrl = response.data;
  syncSessionUser({
    id: form.id ?? undefined,
    username: form.username,
    nickname: form.nickname,
    avatarUrl: form.avatarUrl,
  });
  showSuccess(successText.uploaded("头像"));
};

const beforeAvatarUpload = (rawFile: File) => {
  if (rawFile.type !== "image/jpeg" && rawFile.type !== "image/png") {
    ElMessage.error("只能上传 JPG/PNG 图片");
    return false;
  }
  return true;
};

const saveUserInfo = async () => {
  if (!form.id) {
    ElMessage.error("当前用户信息无效，请重新登录");
    return;
  }

  loading.value = true;
  try {
    await request.put("/users/update", {
      id: form.id,
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl || undefined,
    });

    syncSessionUser({
      id: form.id,
      username: form.username,
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl,
    });
    showSuccess(successText.saved("个人资料"));
  } catch (error) {
    console.error("Failed to save profile", error);
    ElMessage.error("保存失败");
  } finally {
    loading.value = false;
  }
};

const changePassword = async () => {
  if (!form.id) {
    ElMessage.error("当前用户信息无效，请重新登录");
    return;
  }
  if (!passwordForm.oldPassword) {
    ElMessage.error("请输入原密码");
    return;
  }
  if (!passwordForm.newPassword) {
    ElMessage.error("请输入新密码");
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error("两次输入的密码不一致");
    return;
  }

  passwordLoading.value = true;
  try {
    await request.post("/users/change-password", {
      id: form.id,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });

    showSuccess(successText.passwordChanged());
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
    await performLogout({
      clearState: () => hubStore.clearAllCaches(),
      hardRedirect: true,
      router,
    });
  } catch (error) {
    console.error("Failed to change password", error);
    ElMessage.error("密码修改失败");
  } finally {
    passwordLoading.value = false;
  }
};

onMounted(async () => {
  sessionUser.value = getCurrentUser();
  if (!sessionUser.value?.id) {
    ElMessage.error("登录状态已失效，请重新登录");
    router.push("/login");
    return;
  }

  await loadData(Number(sessionUser.value.id));
});
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 88px);
  display: grid;
  gap: 18px;
  padding: 12px 6px 28px;
}

.hero,
.workspace,
.summary-card {
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
.guide-item p,
.summary-user p,
.summary-user span {
  margin: 0;
  line-height: 1.8;
  color: var(--heritage-ink-soft);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.42fr) minmax(290px, 0.82fr);
  gap: 18px;
  align-items: start;
}

.workspace,
.summary-card {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.section-head.compact {
  align-items: flex-start;
}

.profile-tabs {
  margin-top: 18px;
}

.profile-form {
  max-width: 520px;
  margin: 18px auto 0;
}

.summary-column {
  display: grid;
  gap: 18px;
}

.summary-user {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.summary-user strong,
.guide-item strong {
  display: block;
  color: var(--heritage-ink);
}

.guide-list {
  display: grid;
  gap: 12px;
}

.guide-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 246, 240, 0.92);
  border: 1px solid rgba(113, 72, 44, 0.08);
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
  border: 1px dashed #ccc;
  border-radius: 50%;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

@media (max-width: 1080px) {
  .hero,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 10px 0 20px;
  }

  .hero,
  .workspace,
  .summary-card {
    padding: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .summary-user {
    grid-template-columns: 1fr;
    justify-items: start;
  }
}
</style>
