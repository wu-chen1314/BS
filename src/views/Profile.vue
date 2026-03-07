<template>
    <div class="person-container">
        <el-card class="box-card">
            <template #header>
                <div class="card-header">
                    <span>个人信息设置</span>
                </div>
            </template>

            <el-tabs v-model="activeTab">
                <!-- 基本资料标签页 -->
                <el-tab-pane label="基本资料" name="0">
                    <el-form :model="form" label-width="100px" style="max-width: 500px; margin: 20px auto;">

                        <el-form-item label="我的头像">
                            <el-upload class="avatar-uploader" :action="avatarUploadAction" :headers="uploadHeaders"
                                :show-file-list="false" :on-success="handleAvatarSuccess"
                                :before-upload="beforeAvatarUpload">
                                <img v-if="form.avatarUrl" :src="getAvatarUrl" class="avatar" />
                                <el-icon v-else class="avatar-uploader-icon">
                                    <Plus />
                                </el-icon>
                            </el-upload>

                        </el-form-item>

                        <el-form-item label="登录账号">
                            <el-input v-model="form.username" disabled></el-input>
                        </el-form-item>

                        <el-form-item label="用户昵称">
                            <el-input v-model="form.nickname" placeholder="请输入昵称"></el-input>
                        </el-form-item>

                        <el-form-item>
                            <el-button type="primary" @click="saveUserInfo" :loading="loading">保存修改</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>

                <!-- 修改密码标签页 -->
                <el-tab-pane label="修改密码" name="1">
                    <el-form :model="passwordForm" label-width="100px" style="max-width: 500px; margin: 20px auto;">
                        <el-form-item label="原密码">
                            <el-input v-model="passwordForm.oldPassword" type="password"
                                placeholder="请输入原密码"></el-input>
                        </el-form-item>
                        <el-form-item label="新密码">
                            <el-input v-model="passwordForm.newPassword" type="password"
                                placeholder="请输入新密码"></el-input>
                        </el-form-item>
                        <el-form-item label="确认新密码">
                            <el-input v-model="passwordForm.confirmPassword" type="password"
                                placeholder="请确认新密码"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="changePassword"
                                :loading="passwordLoading">修改密码</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { buildApiUrl, buildStaticUrl, getAuthHeaders } from '@/utils/url'

const activeTab = ref('0')
const loading = ref(false)
const passwordLoading = ref(false)
const avatarUploadAction = buildApiUrl('/file/upload/avatar')
const uploadHeaders = getAuthHeaders()

const form = reactive({
    id: null,
    username: '',
    nickname: '',
    avatarUrl: ''
})

const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const getAvatarUrl = computed(() => buildStaticUrl(form.avatarUrl))

onMounted(() => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
        const user = JSON.parse(userStr)
        loadData(user.id)
    }
})

const loadData = async (id) => {
    const res = await request.get(`/users/${id}`)
    if (res.data.code === 200) {
        const userData = res.data.data || {}
        if (!userData.avatarUrl && userData.avatar) {
            userData.avatarUrl = userData.avatar
        }
        Object.assign(form, userData)
    }
}

const handleAvatarSuccess = (response) => {
    if (response.code !== 200) {
        ElMessage.error('上传失败')
        return
    }

    form.avatarUrl = response.data
    ElMessage.success('上传成功')

    const oldUser = JSON.parse(sessionStorage.getItem('user') || '{}')
    const newUser = { ...oldUser, avatarUrl: form.avatarUrl }
    sessionStorage.setItem('user', JSON.stringify(newUser))
    window.dispatchEvent(new Event('storage'))
}

const beforeAvatarUpload = (rawFile) => {
    if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
        ElMessage.error('只能上传 JPG/PNG 图片')
        return false
    }
    return true
}

const saveUserInfo = async () => {
    loading.value = true
    try {
        const res = await request.put('/users/update', form)
        if (res.data.code !== 200) {
            ElMessage.error(res.data.msg || '保存失败')
            return
        }

        ElMessage.success('保存成功')
        const oldUser = JSON.parse(sessionStorage.getItem('user') || '{}')
        const newUser = { ...oldUser, ...form }
        sessionStorage.setItem('user', JSON.stringify(newUser))
        setTimeout(() => location.reload(), 500)
    } catch (error) {
        ElMessage.error('请求失败')
    } finally {
        loading.value = false
    }
}

const changePassword = async () => {
    if (!passwordForm.oldPassword) {
        ElMessage.error('请输入原密码')
        return
    }
    if (!passwordForm.newPassword) {
        ElMessage.error('请输入新密码')
        return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.error('两次输入的密码不一致')
        return
    }

    passwordLoading.value = true
    try {
        const res = await request.post('/users/change-password', {
            id: form.id,
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword
        })
        if (res.data.code !== 200) {
            ElMessage.error(res.data.msg || '密码修改失败')
            return
        }

        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
    } catch (error) {
        ElMessage.error('请求失败')
    } finally {
        passwordLoading.value = false
    }
}
</script>

<style scoped>
.person-container {
    padding: 20px;
}

.avatar-uploader .el-upload {
    border: 1px dashed var(--el-border-color);
    border-radius: 50%;
    /* 圆形 */
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
</style>