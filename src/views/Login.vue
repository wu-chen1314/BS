<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="auth-stage heritage-float-card">
        <div class="card-inner">
          <AuthBrandPanel :focus-field="activeField" :mood="brandMood" />
          <AuthFormPanel ref="authFormRef" :form="form" :rules="rules" :is-login="isLogin" :loading="loading"
            :reg-countdown="regCountdown" :captcha-question="captchaQuestion" @submit="handleSubmit"
            @toggle-mode="toggleMode" @send-register-code="sendRegisterCode" @refresh-captcha="loadCaptcha"
            @open-forgot="openForgotDialog" @focus-field="handleFieldFocus" @blur-field="handleFieldBlur" />
        </div>
      </section>
    </div>

    <PasswordResetDialog ref="resetDialogRef" v-model:visible="forgotDialogVisible" :form="forgotForm"
      :rules="forgotRules" :countdown="forgotCountdown" :loading="resetLoading" @send-code="sendForgotCode"
      @submit="handleResetPassword" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage, type FormRules } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AuthBrandPanel from "@/components/auth/AuthBrandPanel.vue";
import AuthFormPanel from "@/components/auth/AuthFormPanel.vue";
import PasswordResetDialog from "@/components/auth/PasswordResetDialog.vue";
import {
  fetchCaptcha,
  fetchLockStatus,
  loginWithPassword,
  registerAccount,
  resetPassword,
  sendVerificationCode,
} from "@/services/auth";
import { resolveRequestErrorMessage } from "@/utils/request";
import { consumeNextAuthNotice } from "@/utils/logout";
import { showSuccess, showWarning, successText } from "@/utils/uiFeedback";
import { saveCurrentSession } from "@/utils/session";

type BrandMood = "calm" | "watching" | "sad" | "cheer";
type AuthFocusField =
  | "username"
  | "passwordHash"
  | "nickname"
  | "email"
  | "code"
  | "confirmPassword"
  | "captchaAnswer";

const focusFieldLabelMap: Record<AuthFocusField, string> = {
  username: "账号",
  passwordHash: "密码",
  nickname: "昵称",
  email: "邮箱",
  code: "验证码",
  confirmPassword: "确认密码",
  captchaAnswer: "口算题",
};

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const isLogin = ref(true);
const regCountdown = ref(0);
const forgotCountdown = ref(0);
const forgotDialogVisible = ref(false);
const resetLoading = ref(false);
const captchaQuestion = ref("");
const activeField = ref<AuthFocusField | null>(null);
const brandMood = ref<BrandMood>("calm");

const authFormRef = ref<InstanceType<typeof AuthFormPanel>>();
const resetDialogRef = ref<InstanceType<typeof PasswordResetDialog>>();

const emailPattern = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/;

const form = reactive({
  username: "",
  passwordHash: "",
  confirmPassword: "",
  nickname: "",
  email: "",
  code: "",
  captchaId: "",
  captchaAnswer: "",
});

const forgotForm = reactive({
  username: "",
  email: "",
  code: "",
  newPassword: "",
  confirmPassword: "",
});

let brandMoodTimer: number | null = null;
const countdownTimers = new Set<number>();

const modeLabel = computed(() => (isLogin.value ? "当前模式：登录" : "当前模式：注册"));
const pageTitle = computed(() => (isLogin.value ? "登录数字传承平台" : "创建新的传承账号"));
const pageDescription = computed(() =>
  isLogin.value
    ? "输入账号信息后回到刚才的页面继续操作，验证码和异常反馈会保持统一。"
    : "完成注册后就能进入平台继续浏览项目、路线、研学和知识问答。"
);
const brandStateLabel = computed(() => {
  switch (brandMood.value) {
    case "watching":
      return activeField.value ? `搭子正在关注${focusFieldLabelMap[activeField.value]}` : "搭子正在关注输入";
    case "sad":
      return "刚刚触发了校验提醒";
    case "cheer":
      return "当前状态顺利";
    default:
      return "搭子待命中";
  }
});

const clearBrandMoodTimer = () => {
  if (brandMoodTimer !== null) {
    window.clearTimeout(brandMoodTimer);
    brandMoodTimer = null;
  }
};

const clearCountdownTimers = () => {
  countdownTimers.forEach((timer) => window.clearInterval(timer));
  countdownTimers.clear();
};

const restoreBrandMood = () => {
  brandMood.value = activeField.value ? "watching" : "calm";
};

const nudgeBrandMood = (mood: Exclude<BrandMood, "watching" | "calm">, duration = 2200) => {
  clearBrandMoodTimer();
  brandMood.value = mood;
  brandMoodTimer = window.setTimeout(() => {
    restoreBrandMood();
    brandMoodTimer = null;
  }, duration);
};

const handleFieldFocus = (field: AuthFocusField) => {
  activeField.value = field;
  if (brandMood.value !== "sad" && brandMood.value !== "cheer") {
    brandMood.value = "watching";
  }
};

const handleFieldBlur = () => {
  activeField.value = null;
  if (brandMood.value !== "sad" && brandMood.value !== "cheer") {
    brandMood.value = "calm";
  }
};

const startCountdown = (target: typeof regCountdown | typeof forgotCountdown) => {
  target.value = 60;
  const timer = window.setInterval(() => {
    target.value -= 1;
    if (target.value <= 0) {
      window.clearInterval(timer);
      countdownTimers.delete(timer);
    }
  }, 1000);
  countdownTimers.add(timer);
};

const resolveRedirectPath = () => {
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/home";
  if (!redirect.startsWith("/") || redirect.startsWith("//") || redirect.startsWith("/login")) {
    return "/home";
  }
  return redirect;
};
const redirectLabel = computed(() => {
  const redirect = resolveRedirectPath();
  const labels: Record<string, string> = {
    "/home": "首页项目中心",
    "/curation": "主题策展",
    "/heritage-trail": "非遗旅游路线",
    "/learning-studio": "研学工坊",
    "/favorites": "我的收藏",
    "/profile": "个人中心",
  };
  return labels[redirect] || redirect;
});
const heroMetrics = computed(() => [
  {
    help: "当前登录舞台所在的表单模式",
    label: "表单模式",
    value: isLogin.value ? "登录" : "注册",
  },
  {
    help: "口算验证码的准备状态",
    label: "验证码状态",
    value: captchaQuestion.value ? "已就绪" : "加载中",
  },
  {
    help: "邮箱验证码倒计时状态",
    label: "验证码倒计时",
    value: regCountdown.value > 0 ? `${regCountdown.value}s` : forgotCountdown.value > 0 ? `${forgotCountdown.value}s` : "未启动",
  },
  {
    help: "认证成功后的跳转目标",
    label: "登录回跳",
    value: redirectLabel.value,
  },
]);

const sendEmailCode = async (email: string) => {
  try {
    await sendVerificationCode(email);
    showSuccess(successText.sent("验证码"));
    return true;
  } catch (error) {
    nudgeBrandMood("sad");
    ElMessage.error(resolveRequestErrorMessage(error, "验证码发送失败"));
    return false;
  }
};

const sendRegisterCode = async () => {
  if (!emailPattern.test(form.email.trim())) {
    nudgeBrandMood("sad");
    showWarning("请输入正确的邮箱地址");
    return;
  }

  if (await sendEmailCode(form.email.trim())) {
    startCountdown(regCountdown);
  }
};

const sendForgotCode = async () => {
  if (!emailPattern.test(forgotForm.email.trim())) {
    nudgeBrandMood("sad");
    showWarning("请输入正确的邮箱地址");
    return;
  }

  if (await sendEmailCode(forgotForm.email.trim())) {
    startCountdown(forgotCountdown);
  }
};

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (isLogin.value) {
    callback();
    return;
  }

  if (!value) {
    callback(new Error("请再次输入密码"));
    return;
  }

  if (value !== form.passwordHash) {
    callback(new Error("两次输入的密码不一致"));
    return;
  }

  callback();
};

const rules = reactive<FormRules>({
  username: [
    { required: true, message: "请输入账号", trigger: "blur" },
    { pattern: /^[a-zA-Z0-9_]+$/, message: "只能包含字母、数字和下划线", trigger: "blur" },
  ],
  passwordHash: [{ required: true, message: "请输入密码", trigger: "blur" }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "blur" }],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    { type: "email", message: "邮箱格式错误", trigger: ["blur", "change"] },
  ],
  code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  captchaAnswer: [{ required: true, message: "请输入计算结果", trigger: "blur" }],
});

const forgotRules = reactive<FormRules>({
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    { type: "email", message: "邮箱格式错误", trigger: ["blur", "change"] },
  ],
  code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  newPassword: [{ required: true, message: "请输入新密码", trigger: "blur" }],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
          return;
        }

        callback();
      },
      trigger: "blur",
    },
  ],
});

const resetRegisterForm = () => {
  form.username = "";
  form.passwordHash = "";
  form.confirmPassword = "";
  form.nickname = "";
  form.email = "";
  form.code = "";
  form.captchaAnswer = "";
};

const toggleMode = () => {
  isLogin.value = !isLogin.value;
  resetRegisterForm();
  clearBrandMoodTimer();
  brandMood.value = "calm";
  activeField.value = null;
  window.setTimeout(() => authFormRef.value?.clearValidate(), 0);
  loadCaptcha();
};

const loadCaptcha = async () => {
  try {
    const res = await fetchCaptcha();
    if (res.data.data) {
      captchaQuestion.value = res.data.data.question;
      form.captchaId = res.data.data.captchaId;
      form.captchaAnswer = "";
    }
  } catch (error) {
    console.error("Failed to load captcha", error);
    captchaQuestion.value = "";
    form.captchaId = "";
    form.captchaAnswer = "";
  }
};

const inspectLockStatus = async (username: string) => {
  if (!username.trim()) {
    return;
  }

  try {
    await fetchLockStatus(username.trim());
  } catch (error) {
    console.error("Failed to inspect lock status", error);
  }
};

const validateAuthForm = async () => {
  try {
    await authFormRef.value?.validate();
    return true;
  } catch {
    return false;
  }
};

const handleSubmit = async () => {
  const valid = await validateAuthForm();
  if (!valid) {
    nudgeBrandMood("sad");
    return;
  }

  loading.value = true;
  try {
    if (isLogin.value) {
      await inspectLockStatus(form.username);
      const res = await loginWithPassword({
        username: form.username.trim(),
        password: form.passwordHash,
        captchaId: form.captchaId,
        captchaAnswer: form.captchaAnswer,
      });

      const loginData = res.data.data;
      saveCurrentSession(loginData.user, loginData.token, loginData.expiresIn);
      nudgeBrandMood("cheer", 1200);
      showSuccess(successText.login());
      await router.push(resolveRedirectPath());
      return;
    }

    await registerAccount({
      username: form.username.trim(),
      passwordHash: form.passwordHash,
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      code: form.code.trim(),
      captchaId: form.captchaId || "",
      captchaAnswer: form.captchaAnswer || "",
    });

    nudgeBrandMood("cheer", 1600);
    showSuccess(successText.register());
    isLogin.value = true;
    form.passwordHash = "";
    form.confirmPassword = "";
    form.code = "";
    form.nickname = "";
    form.captchaAnswer = "";
    activeField.value = null;
    window.setTimeout(() => authFormRef.value?.clearValidate(), 0);
    loadCaptcha();
  } catch (error) {
    nudgeBrandMood("sad");
    ElMessage.error(resolveRequestErrorMessage(error, isLogin.value ? "登录失败" : "注册失败"));
    loadCaptcha();
  } finally {
    loading.value = false;
  }
};

const openForgotDialog = () => {
  forgotDialogVisible.value = true;
  forgotForm.username = form.username;
  forgotForm.email = "";
  forgotForm.code = "";
  forgotForm.newPassword = "";
  forgotForm.confirmPassword = "";
  window.setTimeout(() => resetDialogRef.value?.clearValidate(), 0);
};

const handleResetPassword = async () => {
  const valid = await resetDialogRef.value?.validate().then(() => true).catch(() => false);
  if (!valid) {
    nudgeBrandMood("sad");
    return;
  }

  resetLoading.value = true;
  try {
    await resetPassword({
      username: forgotForm.username.trim(),
      email: forgotForm.email.trim(),
      code: forgotForm.code.trim(),
      newPassword: forgotForm.newPassword,
    });

    showSuccess(successText.passwordReset());
    forgotDialogVisible.value = false;
    form.username = forgotForm.username.trim();
    form.passwordHash = "";
    nudgeBrandMood("cheer", 1400);
  } catch (error) {
    nudgeBrandMood("sad");
    ElMessage.error(resolveRequestErrorMessage(error, "密码重置失败"));
  } finally {
    resetLoading.value = false;
  }
};

onMounted(() => {
  loadCaptcha();
  const authNotice = consumeNextAuthNotice();
  if (authNotice) {
    showSuccess(authNotice);
  }
});

onBeforeUnmount(() => {
  clearBrandMoodTimer();
  clearCountdownTimers();
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at 16% 20%, rgba(255, 182, 141, 0.18), transparent 22%),
    radial-gradient(circle at 84% 18%, rgba(89, 185, 175, 0.12), transparent 18%),
    radial-gradient(circle at 72% 86%, rgba(255, 214, 137, 0.12), transparent 22%),
    linear-gradient(135deg, #fff7ef 0%, #f6ede2 48%, #efe4d8 100%);
  font-family: "Noto Serif SC", "Songti SC", "STSong", "SimSun", serif;
}

.login-shell {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.hero,
.auth-stage {
  border-radius: 30px;
  border: 1px solid rgba(191, 160, 126, 0.22);
  background: rgba(255, 251, 245, 0.86);
  box-shadow:
    0 34px 70px -28px rgba(73, 47, 33, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.45) inset;
  backdrop-filter: blur(18px) saturate(1.08);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(242, 181, 74, 0.15), transparent 34%),
    linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(255, 251, 244, 0.98));
}

.page-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--heritage-gold);
}

.hero-copy h1 {
  margin: 0;
  color: var(--heritage-ink);
  font-size: 42px;
}

.page-desc,
.metric-card p {
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
  color: var(--heritage-muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 30px;
  color: var(--heritage-primary);
}

.auth-stage {
  overflow: hidden;
}

.card-inner {
  display: flex;
  min-height: 680px;
}

:deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  border-bottom: 1.5px solid rgba(190, 169, 145, 0.72) !important;
  border-radius: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

:deep(.el-input__wrapper:hover) {
  border-bottom-color: rgba(164, 59, 47, 0.5) !important;
}

:deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--heritage-primary) !important;
  box-shadow: 0 8px 18px -16px rgba(164, 59, 47, 0.5) !important;
}

:deep(.el-input__inner) {
  height: 46px;
  padding-left: 36px !important;
  color: #3f2e22 !important;
  font-size: 16px;
}

:deep(.el-input__inner::placeholder) {
  color: #b09a86 !important;
}

:deep(.el-input__prefix) {
  left: 4px !important;
  width: 24px;
  color: #baa48e !important;
  font-size: 20px;
}

:deep(.el-input__wrapper.is-focus .el-input__prefix) {
  color: var(--heritage-primary) !important;
}

:deep(.el-link) {
  color: var(--heritage-primary) !important;
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .login-page {
    padding: 12px;
  }

  .card-inner {
    flex-direction: column;
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
