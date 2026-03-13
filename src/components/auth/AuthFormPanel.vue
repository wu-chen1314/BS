<template>
  <div class="form-side">
    <div class="form-header">
      <h2>{{ isLogin ? "欢迎回来" : "创建账号" }}</h2>
      <p>{{ isLogin ? "输入账号信息继续使用平台" : "填完信息后就能和守艺搭子一起出发" }}</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="login-form" @keyup.enter="emit('submit')">
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          placeholder="用户名（字母 / 数字 / 下划线）"
          :prefix-icon="User"
          size="large"
          clearable
          @focus="handleFocus('username')"
          @blur="handleBlur"
        />
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="nickname">
        <el-input
          v-model="form.nickname"
          placeholder="昵称"
          :prefix-icon="Edit"
          size="large"
          clearable
          @focus="handleFocus('nickname')"
          @blur="handleBlur"
        />
      </el-form-item>

      <el-form-item prop="passwordHash">
        <el-input
          v-model="form.passwordHash"
          type="password"
          placeholder="密码（6 - 30 位）"
          :prefix-icon="Lock"
          size="large"
          show-password
          clearable
          @focus="handleFocus('passwordHash')"
          @blur="handleBlur"
        />
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          :prefix-icon="Key"
          size="large"
          show-password
          clearable
          @focus="handleFocus('confirmPassword')"
          @blur="handleBlur"
        />
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="email">
        <el-input
          v-model="form.email"
          placeholder="邮箱地址"
          :prefix-icon="Message"
          size="large"
          clearable
          @focus="handleFocus('email')"
          @blur="handleBlur"
        />
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="code">
        <div class="inline-row">
          <el-input
            v-model="form.code"
            placeholder="邮箱验证码"
            :prefix-icon="Message"
            size="large"
            class="flex-1"
            @focus="handleFocus('code')"
            @blur="handleBlur"
          />
          <el-button type="primary" plain @click="emit('send-register-code')" :disabled="regCountdown > 0" class="code-btn">
            {{ regCountdown > 0 ? `${regCountdown}s 后重发` : "获取验证码" }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item prop="captchaAnswer">
        <div class="inline-row align-center">
          <el-input
            v-model="form.captchaAnswer"
            :placeholder="captchaPlaceholder"
            size="large"
            class="flex-1"
            @focus="handleFocus('captchaAnswer')"
            @blur="handleBlur"
          />
          <el-button text type="primary" @click="emit('refresh-captcha')" class="switch-btn">换一题</el-button>
        </div>
      </el-form-item>

      <div v-if="isLogin" class="options-row">
        <span class="options-tip">搭子们会盯着表单，别紧张，慢慢输。</span>
        <el-link type="primary" :underline="false" @click="emit('open-forgot')">忘记密码</el-link>
      </div>
      <div v-else class="spacer"></div>

      <el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" size="large" @click="emit('submit')">
          {{ isLogin ? "登录" : "注册" }}
        </el-button>
      </el-form-item>

      <div class="register-tip">
        <span v-if="isLogin">
          还没有账号？
          <el-link type="primary" :underline="false" @click="emit('toggle-mode')">立即注册</el-link>
        </span>
        <span v-else>
          已有账号？
          <el-link type="primary" :underline="false" @click="emit('toggle-mode')">直接登录</el-link>
        </span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { Edit, Key, Lock, Message, User } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";

export type AuthFocusField =
  | "username"
  | "passwordHash"
  | "nickname"
  | "email"
  | "code"
  | "confirmPassword"
  | "captchaAnswer";

interface AuthFormModel {
  username: string;
  passwordHash: string;
  confirmPassword: string;
  nickname: string;
  email: string;
  code: string;
  captchaAnswer: string;
}

const props = defineProps<{
  form: AuthFormModel;
  rules: FormRules;
  isLogin: boolean;
  loading: boolean;
  regCountdown: number;
  captchaQuestion: string;
}>();

const emit = defineEmits<{
  (e: "submit"): void;
  (e: "toggle-mode"): void;
  (e: "send-register-code"): void;
  (e: "refresh-captcha"): void;
  (e: "open-forgot"): void;
  (e: "focus-field", field: AuthFocusField): void;
  (e: "blur-field"): void;
}>();

const formRef = ref<FormInstance>();

const captchaPlaceholder = computed(() =>
  props.captchaQuestion ? `请计算：${props.captchaQuestion}` : "正在加载口算验证码..."
);

const handleFocus = (field: AuthFocusField) => emit("focus-field", field);
const handleBlur = () => emit("blur-field");

const validate = () => formRef.value?.validate();
const clearValidate = () => formRef.value?.clearValidate();

defineExpose({
  validate,
  clearValidate,
});
</script>

<style scoped>
.form-side {
  flex: 1;
  padding: 64px 56px;
  background: transparent;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  margin: 0 0 12px;
  color: var(--heritage-ink);
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.form-header p {
  margin: 0;
  color: var(--heritage-muted);
  font-size: 16px;
}

.inline-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.align-center {
  align-items: center;
}

.flex-1 {
  flex: 1;
}

.code-btn {
  width: 124px;
  height: 40px;
}

.switch-btn {
  width: 86px;
}

.options-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 18px 0 34px;
  color: var(--heritage-muted);
  font-size: 15px;
}

.options-tip {
  color: var(--heritage-muted);
}

.spacer {
  height: 12px;
}

.login-btn {
  width: 100%;
  height: 56px !important;
  border: none !important;
  border-radius: 28px !important;
  background: linear-gradient(135deg, var(--heritage-primary) 0%, var(--heritage-gold) 100%) !important;
  color: var(--heritage-paper-soft) !important;
  font-size: 20px !important;
  font-weight: 700 !important;
  letter-spacing: 0.35em;
  text-indent: 0.35em;
  box-shadow: 0 16px 28px -10px rgba(164, 59, 47, 0.4);
}

.register-tip {
  margin-top: 28px;
  text-align: center;
  color: var(--heritage-muted);
  font-size: 15px;
}

.register-tip .el-link {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 992px) {
  .form-side {
    padding: 36px 24px 42px;
  }

  .form-header h2 {
    font-size: 32px;
  }
}
</style>
