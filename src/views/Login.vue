<template>
    <div class="login-container">
        <div class="bg-pattern"></div>
        <div class="login-card-wrapper">
            <el-card class="login-card" :body-style="{ padding: 0 }" shadow="never">
                <div class="card-inner">
                    <div class="brand-side">
                        <div class="brand-content">
                            <div class="seal">非遗</div>
                            <h1>非遗传承</h1>
                            <p>让传统技艺活在当下</p>
                            <div class="divider"></div>
                            <div class="quote">让文化被看见，让传承继续发生。</div>
                        </div>
                    </div>

                    <div class="form-side">
                        <div class="form-header">
                            <h2>{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
                            <p>{{ isLogin ? '请输入您的账号信息' : '填写以下信息创建账号' }}</p>
                        </div>

                        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="login-form" @keyup.enter="handleSubmit">
                            <el-form-item prop="username">
                                <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" clearable />
                            </el-form-item>

                            <el-form-item prop="nickname" v-if="!isLogin">
                                <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="Edit" size="large" clearable />
                            </el-form-item>

                            <el-form-item prop="passwordHash">
                                <el-input v-model="form.passwordHash" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password clearable />
                            </el-form-item>

                            <el-form-item prop="confirmPassword" v-if="!isLogin">
                                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Key" size="large" show-password clearable />
                            </el-form-item>

                            <el-form-item prop="email" v-if="!isLogin">
                                <el-input v-model="form.email" placeholder="邮箱地址" :prefix-icon="Message" size="large" clearable />
                            </el-form-item>

                            <el-form-item prop="code" v-if="!isLogin">
                                <div style="display: flex; width: 100%; gap: 10px;">
                                    <el-input v-model="form.code" placeholder="验证码" :prefix-icon="Message" size="large" style="flex: 1;" />
                                    <el-button type="primary" plain @click="sendRegCode" :disabled="regCountdown > 0" style="width: 110px; height: 40px;">
                                        {{ regCountdown > 0 ? `${regCountdown}s` : '获取验证码' }}
                                    </el-button>
                                </div>
                            </el-form-item>

                            <div class="options-row" v-if="isLogin">
                                <el-checkbox v-model="remember">记住我</el-checkbox>
                                <el-link type="primary" :underline="false" @click="openForgotDialog">忘记密码？</el-link>
                            </div>
                            <div style="height: 10px;" v-else></div>

                            <el-form-item>
                                <el-button type="primary" :loading="loading" class="login-btn" size="large" @click="handleSubmit">
                                    {{ isLogin ? '登录' : '注册' }}
                                </el-button>
                            </el-form-item>

                            <div class="register-tip">
                                <span v-if="isLogin">还没有账号？<el-link type="primary" :underline="false" @click="toggleMode">立即注册</el-link></span>
                                <span v-else>已有账号？<el-link type="primary" :underline="false" @click="toggleMode">直接登录</el-link></span>
                            </div>
                        </el-form>
                    </div>
                </div>
            </el-card>
        </div>

        <el-dialog v-model="forgotDialogVisible" title="安全验证与密码重置" width="480px" center destroy-on-close>
            <div style="padding: 0 20px;">
                <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="80px">
                    <el-form-item label="登录账号" prop="username">
                        <el-input v-model="forgotForm.username" placeholder="请输入账号" :prefix-icon="User" />
                    </el-form-item>
                    <el-form-item label="绑定邮箱" prop="email">
                        <el-input v-model="forgotForm.email" placeholder="请输入绑定邮箱" :prefix-icon="Message" />
                    </el-form-item>
                    <el-form-item label="验证码" prop="code">
                        <div style="display: flex; width: 100%; gap: 10px;">
                            <el-input v-model="forgotForm.code" placeholder="输入验证码" />
                            <el-button type="primary" plain @click="sendForgotCode" :disabled="forgotCountdown > 0" style="width: 110px;">
                                {{ forgotCountdown > 0 ? `${forgotCountdown}s` : '获取验证码' }}
                            </el-button>
                        </div>
                    </el-form-item>
                    <el-form-item label="新密码" prop="newPassword">
                        <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="请输入新密码" :prefix-icon="Lock" />
                    </el-form-item>
                    <el-form-item label="确认密码" prop="confirmPassword">
                        <el-input v-model="forgotForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" :prefix-icon="Key" />
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="forgotDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleResetPassword" :loading="resetLoading" style="background: #8b5a2b; border-color: #8b5a2b;">
                        确认重置
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock, Edit, Key, Message } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const resetLoading = ref(false)
const remember = ref(false)
const isLogin = ref(true)
const regCountdown = ref(0)
const forgotCountdown = ref(0)

const formRef = ref<FormInstance>()
const forgotFormRef = ref<FormInstance>()
const forgotDialogVisible = ref(false)

const form = reactive({
    username: '',
    passwordHash: '',
    confirmPassword: '',
    nickname: '',
    email: '',
    code: ''
})

const forgotForm = reactive({
    username: '',
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
    if (isLogin.value) return callback()
    if (!value) return callback(new Error('请再次输入密码'))
    if (value !== form.passwordHash) return callback(new Error('两次输入的密码不一致'))
    callback()
}

const validateForgotConfirmPassword = (_rule: any, value: string, callback: any) => {
    if (!value) return callback(new Error('请确认新密码'))
    if (value !== forgotForm.newPassword) return callback(new Error('两次输入的密码不一致'))
    callback()
}

const rules = reactive<FormRules>({
    username: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字和下划线', trigger: 'blur' }
    ],
    passwordHash: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
    nickname: [{ required: !isLogin.value, message: '请输入昵称', trigger: 'blur' }],
    email: [
        { required: !isLogin.value, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
    ],
    code: [{ required: !isLogin.value, message: '请输入验证码', trigger: 'blur' }]
})

const forgotRules = reactive<FormRules>({
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
    ],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
    confirmPassword: [{ validator: validateForgotConfirmPassword, trigger: 'blur' }]
})

const sendEmailApi = async (email: string) => {
    try {
        const res = await request.post('/auth/send-code', { email })
        if (res.data.code === 200) {
            ElMessage.success('验证码已发送，请查收邮箱')
            return true
        }
        ElMessage.error(res.data.msg || '发送失败')
        return false
    } catch {
        ElMessage.error('请求失败，请检查后端是否已启动')
        return false
    }
}

const startCountdown = (target: typeof regCountdown | typeof forgotCountdown) => {
    target.value = 60
    const timer = setInterval(() => {
        target.value--
        if (target.value <= 0) {
            clearInterval(timer)
        }
    }, 1000)
}

const sendRegCode = async () => {
    if (!form.email || !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(form.email)) {
        return ElMessage.warning('请先输入正确的邮箱地址')
    }
    if (await sendEmailApi(form.email)) {
        startCountdown(regCountdown)
    }
}

const sendForgotCode = async () => {
    if (!forgotForm.email || !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(forgotForm.email)) {
        return ElMessage.warning('请先输入正确的邮箱地址')
    }
    if (await sendEmailApi(forgotForm.email)) {
        startCountdown(forgotCountdown)
    }
}

const toggleMode = () => {
    isLogin.value = !isLogin.value
    form.username = ''
    form.passwordHash = ''
    form.confirmPassword = ''
    form.nickname = ''
    form.email = ''
    form.code = ''
    setTimeout(() => formRef.value?.clearValidate(), 0)
}

const handleSubmit = async () => {
    await formRef.value?.validate(async (valid) => {
        if (!valid) return
        loading.value = true
        try {
            const url = isLogin.value
                ? '/login'
                : `/auth/register?code=${form.code}`

            const payload = isLogin.value
                ? { username: form.username, password: form.passwordHash }
                : {
                    username: form.username,
                    passwordHash: form.passwordHash,
                    nickname: form.nickname,
                    email: form.email
                }

            const res = await request.post(url, payload)
            if (res.data.code !== 200) {
                ElMessage.error(res.data.msg || '操作失败')
                return
            }

            if (isLogin.value) {
                const loginData = res.data.data
                const userWithToken = {
                    ...loginData.user,
                    token: loginData.token
                }
                sessionStorage.setItem('user', JSON.stringify(userWithToken))
                sessionStorage.setItem('token', loginData.token)
                sessionStorage.setItem('tokenExpires', String(loginData.expiresIn))
                ElMessage.success('登录成功')
                router.push('/home')
                return
            }

            const registeredUsername = form.username
            ElMessage.success('注册成功，请使用新账号登录')
            isLogin.value = true
            form.username = registeredUsername
            form.passwordHash = ''
            form.confirmPassword = ''
            form.nickname = ''
            form.email = ''
            form.code = ''
            setTimeout(() => formRef.value?.clearValidate(), 0)
        } catch (error: any) {
            const message = error?.response?.data?.msg || error?.response?.data?.message
            ElMessage.error(message || '请求失败，请稍后重试')
        } finally {
            loading.value = false
        }
    })
}

const openForgotDialog = () => {
    forgotDialogVisible.value = true
    forgotForm.username = form.username
    forgotForm.email = ''
    forgotForm.code = ''
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
    setTimeout(() => forgotFormRef.value?.clearValidate(), 0)
}

const handleResetPassword = async () => {
    await forgotFormRef.value?.validate(async (valid) => {
        if (!valid) return
        resetLoading.value = true
        try {
            const payload = {
                username: forgotForm.username,
                email: forgotForm.email,
                code: forgotForm.code,
                newPassword: forgotForm.newPassword
            }
            const res = await request.post('/auth/reset-password', payload)
            if (res.data.code === 200) {
                ElMessage.success('密码重置成功，请重新登录')
                forgotDialogVisible.value = false
                form.username = forgotForm.username
                form.passwordHash = ''
            } else {
                ElMessage.error(res.data.msg || '重置失败')
            }
        } catch {
            ElMessage.error('服务连接失败')
        } finally {
            resetLoading.value = false
        }
    })
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@300;400;500;600;700&display=swap');

.login-container {
    position: relative;
    min-height: 100vh;
    background: linear-gradient(135deg, #f5f0e8 0%, #e6d8c8 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Noto Serif SC', 'Times New Roman', serif;
    padding: 20px;
    overflow: hidden;
}

/* 鍔ㄦ€佽儗鏅浘妗?*/
.bg-pattern {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image:
        radial-gradient(circle at 20% 30%, rgba(200, 180, 150, 0.15) 0%, transparent 40%),
        radial-gradient(circle at 80% 70%, rgba(150, 120, 90, 0.1) 0%, transparent 40%),
        linear-gradient(45deg, rgba(180, 160, 130, 0.03) 25%, transparent 25%, transparent 75%, rgba(180, 160, 130, 0.03) 75%, rgba(180, 160, 130, 0.03)),
        linear-gradient(45deg, rgba(180, 160, 130, 0.03) 25%, transparent 25%, transparent 75%, rgba(180, 160, 130, 0.03) 75%, rgba(180, 160, 130, 0.03));
    background-size: 100% 100%, 100% 100%, 60px 60px, 60px 60px;
    background-position: 0 0, 0 0, 0 0, 30px 30px;
    animation: bgGradient 15s ease infinite alternate;
    pointer-events: none;
}

@keyframes bgGradient {
    0% {
        transform: scale(1);
    }

    100% {
        transform: scale(1.05);
    }
}

.login-card-wrapper {
    width: 1050px;
    max-width: 100%;
    position: relative;
    z-index: 2;
    animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    opacity: 0;
    transform: translateY(30px);
}

@keyframes slideUp {
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.login-card {
    border: 1px solid rgba(255, 255, 255, 0.6) !important;
    border-radius: 24px !important;
    overflow: hidden;
    box-shadow: 0 40px 80px -20px rgba(90, 64, 48, 0.25), 0 0 40px rgba(255, 255, 255, 0.5) inset !important;
    background: rgba(255, 250, 242, 0.9) !important;
    backdrop-filter: blur(20px) saturate(1.2);
}

.card-inner {
    display: flex;
    min-height: 600px;
    background: transparent;
}

.brand-side {
    flex: 1.25;
    background: linear-gradient(135deg, rgba(124, 94, 74, 0.95) 0%, rgba(90, 64, 48, 0.98) 100%);
    color: #f5e6d3;
    padding: 56px 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
}

.brand-side::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 200%;
    height: 200%;
    background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" opacity="0.08"><path d="M20 30 Q 40 10 60 30 T 100 30" stroke="%23f5e6d3" fill="none" stroke-width="1"/><circle cx="30" cy="60" r="8" fill="%23f5e6d3"/><circle cx="70" cy="60" r="8" fill="%23f5e6d3"/></svg>');
    background-size: 150px 150px;
    background-repeat: repeat;
    pointer-events: none;
    animation: drift 60s linear infinite;
}

@keyframes drift {
    0% {
        transform: translate(0, 0);
    }

    100% {
        transform: translate(-50%, -50%);
    }
}

.brand-content {
    position: relative;
    z-index: 2;
    max-width: 320px;
    text-align: center;
    animation: fadeIn 1s 0.3s ease forwards;
    opacity: 0;
}

@keyframes fadeIn {
    to {
        opacity: 1;
    }
}

.seal {
    display: inline-block;
    border: 2px solid #e6c9a8;
    color: #e6c9a8;
    padding: 8px 24px;
    font-size: 20px;
    font-weight: 600;
    letter-spacing: 6px;
    border-radius: 4px;
    margin-bottom: 40px;
    background: rgba(230, 201, 168, 0.05);
    position: relative;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
    transform: rotate(-2deg);
    transition: transform 0.3s ease;
}

.seal:hover {
    transform: rotate(0deg) scale(1.05);
}

.seal::after {
    content: '';
    position: absolute;
    inset: -6px;
    border: 1px solid rgba(230, 201, 168, 0.4);
    border-radius: 6px;
}

.brand-content h1 {
    font-size: 48px;
    font-weight: 700;
    margin: 0 0 16px;
    color: #f9e1c0;
    text-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    line-height: 1.2;
    letter-spacing: 4px;
}

.brand-content p {
    font-size: 20px;
    color: #dccbb8;
    margin-bottom: 40px;
    font-weight: 300;
    letter-spacing: 2px;
}

.divider {
    width: 80px;
    height: 1px;
    background: linear-gradient(90deg, transparent, #e6c9a8, transparent);
    margin: 40px auto;
}

.quote {
    font-size: 18px;
    color: #cbb59b;
    line-height: 2;
    letter-spacing: 2px;
    font-style: italic;
    font-weight: 300;
}

.form-side {
    flex: 1;
    padding: 64px 56px;
    background: transparent;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.form-header {
    margin-bottom: 48px;
    animation: fadeInRight 0.6s 0.2s ease forwards;
    opacity: 0;
    transform: translateX(20px);
}

@keyframes fadeInRight {
    to {
        opacity: 1;
        transform: translateX(0);
    }
}

.form-header h2 {
    font-size: 36px;
    font-weight: 600;
    color: #4a3424;
    margin: 0 0 12px;
    letter-spacing: 2px;
}

.form-header p {
    font-size: 16px;
    color: #8b7a69;
    margin: 0;
}

.login-form {
    animation: fadeInRight 0.6s 0.4s ease forwards;
    opacity: 0;
    transform: translateX(20px);
}

:deep(.el-input__wrapper) {
    background-color: transparent !important;
    box-shadow: none !important;
    border: none !important;
    border-bottom: 1.5px solid #d9cfc1 !important;
    border-radius: 0 !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
    border-bottom-color: #a07d5a !important;
}

:deep(.el-input__wrapper.is-focus) {
    border-bottom-color: #8b5a2b !important;
    box-shadow: 0 4px 10px -8px rgba(139, 90, 43, 0.4) !important;
}

:deep(.el-input__inner) {
    color: #3e2e22 !important;
    font-size: 17px;
    padding-left: 36px !important;
    height: 44px;
}

:deep(.el-input__inner::placeholder) {
    color: #baa48e !important;
    font-weight: 400;
}

:deep(.el-input__prefix) {
    left: 4px !important;
    width: 24px;
    color: #baa48e !important;
    font-size: 20px;
    transition: color 0.3s;
}

:deep(.el-input__wrapper.is-focus .el-input__prefix) {
    color: #8b5a2b !important;
}

.options-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 16px 0 36px;
    color: #5f4e3c;
    font-size: 15px;
}

:deep(.el-checkbox__label) {
    color: #5f4e3c !important;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #8b5a2b;
    border-color: #8b5a2b;
}

:deep(.el-link) {
    color: #8b5a2b !important;
    font-weight: 500;
    transition: all 0.3s;
}

:deep(.el-link:hover) {
    color: #b07d4a !important;
    transform: translateY(-1px);
}

.login-btn {
    width: 100%;
    height: 56px !important;
    background: linear-gradient(135deg, #8b5a2b 0%, #6b4420 100%) !important;
    border: none !important;
    border-radius: 28px !important;
    color: #fffaf2 !important;
    font-weight: 600 !important;
    font-size: 20px !important;
    letter-spacing: 12px;
    text-indent: 12px;
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) !important;
    box-shadow: 0 10px 24px -6px rgba(139, 90, 43, 0.6);
    position: relative;
    overflow: hidden;
}

.login-btn::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 50%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transform: skewX(-20deg);
    transition: 0.5s;
}

.login-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 32px -8px rgba(139, 90, 43, 0.8);
}

.login-btn:hover::after {
    left: 150%;
}

.login-btn:active {
    transform: translateY(1px);
    box-shadow: 0 6px 16px -4px rgba(139, 90, 43, 0.6);
}

.register-tip {
    margin-top: 32px;
    text-align: center;
    color: #8b7a69;
    font-size: 15px;
}

.register-tip .el-link {
    font-size: 16px;
    margin-left: 8px;
    font-weight: 600;
}

@media (max-width: 992px) {
    .login-card-wrapper {
        width: 100%;
        max-width: 500px;
    }

    .card-inner {
        flex-direction: column;
        min-height: auto;
    }

    .brand-side {
        padding: 48px 32px;
        min-height: 240px;
        flex: auto;
    }

    .form-side {
        padding: 48px 32px;
    }

    .brand-content h1 {
        font-size: 36px;
    }
}
</style>






