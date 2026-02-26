<template>
    <div class="app-main">
        <el-row :gutter="20">
            <el-col :span="8">
                <el-card align="center">
                    <el-avatar :size="100"
                        :src="form.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                    <h2 style="margin-bottom: 5px">{{ form.nickname || form.username }}</h2>
                    <el-tag :type="form.role === 'admin' ? 'danger' : 'success'">{{ form.role === 'admin' ? '管理员' :
                        '普通用户' }}</el-tag>
                    <div style="margin-top: 20px; text-align: left; color: #606266;">
                        <p><el-icon>
                                <User />
                            </el-icon> 账号：{{ form.username }}</p>
                        <p><el-icon>
                                <Message />
                            </el-icon> 邮箱：{{ form.email || '未绑定' }}</p>
                        <p><el-icon>
                                <Calendar />
                            </el-icon> 注册时间：{{ form.createdAt ? form.createdAt.replace('T', ' ') : '未知' }}</p>
                    </div>
                </el-card>
            </el-col>

            <el-col :span="16">
                <el-card>
                    <el-tabs v-model="activeTab">

                        <el-tab-pane label="基本资料" name="info">
                            <el-form label-width="80px" style="margin-top: 20px; max-width: 500px;">
                                <el-form-item label="昵称">
                                    <el-input v-model="form.nickname" />
                                </el-form-item>
                                <el-form-item label="邮箱">
                                    <el-input v-model="form.email" />
                                </el-form-item>
                                <el-form-item>
                                    <el-button type="primary" @click="updateProfile" :loading="loading">保存修改</el-button>
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>

                        <el-tab-pane label="修改密码" name="password">
                            <el-form label-width="100px" style="margin-top: 20px; max-width: 500px;" :model="pwdForm"
                                :rules="pwdRules" ref="pwdRef">
                                <el-form-item label="旧密码" prop="oldPassword">
                                    <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                                </el-form-item>
                                <el-form-item label="新密码" prop="newPassword">
                                    <el-input v-model="pwdForm.newPassword" type="password" show-password />
                                </el-form-item>
                                <el-form-item label="确认密码" prop="confirmPassword">
                                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                                </el-form-item>
                                <el-form-item>
                                    <el-button type="danger" @click="updatePassword" :loading="loading">确认修改</el-button>
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>

                    </el-tabs>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { User, Message, Calendar } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const activeTab = ref('info')
const loading = ref(false)
const pwdRef = ref()

const form = reactive({
    id: undefined,
    username: '',
    nickname: '',
    role: '',
    email: '',
    avatarUrl: '',
    createdAt: ''
})

const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

// 密码校验规则
const validatePass2 = (rule: any, value: any, callback: any) => {
    if (value !== pwdForm.newPassword) {
        callback(new Error('两次输入密码不一致!'))
    } else {
        callback()
    }
}

const pwdRules = {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }],
    confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
}

// 初始化
onMounted(() => {
    const str = localStorage.getItem('user')
    if (str) {
        Object.assign(form, JSON.parse(str))
    }
})

// 更新基本资料
const updateProfile = async () => {
    loading.value = true
    try {
        // 复用之前的 update 接口
        const res = await axios.put('http://localhost:8080/api/users/update', form)
        if (res.data.code === 200) {
            ElMessage.success('资料更新成功，请重新登录')
            localStorage.removeItem('user')
            setTimeout(() => location.href = '/login', 1000)
        } else {
            ElMessage.error(res.data.msg)
        }
    } finally {
        loading.value = false
    }
}

// 修改密码 (需要在后端 LoginController 或 UserController 加一个专属接口，或者这里为了演示，我们假设后端有一个 /api/users/password 接口)
// ⚠️ 注意：为了简化，我们这里直接调用 update 接口演示。
// 在真实毕设中，建议你在后端 SysUserController 加一个 @PutMapping("/password") 专门处理改密码
// 修改密码
const updatePassword = () => {
    // 1. 先校验表单（长度、两次输入是否一致）
    pwdRef.value.validate(async (valid: boolean) => {
        if (valid) {
            loading.value = true
            try {
                // 2. 发送真实请求给后端
                const res = await axios.post('http://localhost:8080/api/users/change-password', {
                    id: form.id, // 当前用户ID
                    oldPassword: pwdForm.oldPassword,
                    newPassword: pwdForm.newPassword
                })

                // 3. 处理结果
                if (res.data.code === 200) {
                    ElMessage.success('密码修改成功，请重新登录')
                    // 清除缓存，强制退出
                    localStorage.removeItem('user')
                    setTimeout(() => {
                        location.href = '/login'
                    }, 1500)
                } else {
                    // 如果后端返回“旧密码错误”，这里会显示出来
                    ElMessage.error(res.data.msg)
                }
            } catch (e) {
                ElMessage.error('网络错误，修改失败')
            } finally {
                loading.value = false
            }
        }
    })
}
</script>

<style scoped>
.app-main {
    padding: 20px;
}
</style>