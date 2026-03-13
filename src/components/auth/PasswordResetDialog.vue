<template>
  <el-dialog v-model="dialogVisible" title="安全验证与密码重置" width="480px" center destroy-on-close>
    <div class="dialog-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="绑定邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入账号绑定邮箱" :prefix-icon="Message" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="inline-row">
            <el-input v-model="form.code" placeholder="输入收到的 6 位验证码" />
            <el-button type="primary" plain @click="emit('send-code')" :disabled="countdown > 0" class="code-btn">
              {{ countdown > 0 ? `${countdown}s` : "获取验证码" }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" :prefix-icon="Lock" />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            :prefix-icon="Key"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="emit('submit')">确认重置</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { Key, Lock, Message, User } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";

interface ResetFormModel {
  username: string;
  email: string;
  code: string;
  newPassword: string;
  confirmPassword: string;
}

const props = defineProps<{
  visible: boolean;
  form: ResetFormModel;
  rules: FormRules;
  countdown: number;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "update:visible", value: boolean): void;
  (e: "send-code"): void;
  (e: "submit"): void;
}>();

const formRef = ref<FormInstance>();

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit("update:visible", value),
});

const validate = () => formRef.value?.validate();
const clearValidate = () => formRef.value?.clearValidate();

defineExpose({
  validate,
  clearValidate,
});
</script>

<style scoped>
.dialog-body {
  padding: 0 20px;
}

.inline-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.code-btn {
  width: 110px;
}
</style>
