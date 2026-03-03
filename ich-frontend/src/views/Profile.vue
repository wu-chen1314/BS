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
                            <el-upload class="avatar-uploader" action="http://localhost:8080/api/file/upload/avatar"
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
import axios from 'axios'
import request from '@/utils/request'

// 激活的标签页
const activeTab = ref('0');

// 1. 定义表单，字段名必须和后端实体类 SysUser 一致 
const form = reactive({
    id: null,
    username: '',
    nickname: '',
    avatarUrl: '' // ✨ 统一叫 avatarUrl 
})

// 密码表单
const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const loading = ref(false)
const passwordLoading = ref(false)

// 计算头像URL
const getAvatarUrl = computed(() => {
    if (form.avatarUrl) {
        return form.avatarUrl.startsWith('http') ? form.avatarUrl : `http://localhost:8080${form.avatarUrl}`;
    }
    return '';
});

// 2. 初始化：获取当前用户信息 
onMounted(() => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
        const user = JSON.parse(userStr)
        loadData(user.id)
    }
})

// 3. 回显数据 
const loadData = async (id) => {
    const res = await request.get(`/users/${id}`)
    console.log('【调试】后端返回数据:', res.data)

    if (res.data.code === 200) {
        const userData = res.data.data
        // 如果后端返回的是 avatar (没 Url)，这里做个兼容 
        if (!userData.avatarUrl && userData.avatar) {
            userData.avatarUrl = userData.avatar
        }
        Object.assign(form, userData)
        console.log('【调试】表单数据已更新:', form)
    }
}

// 4. 上传成功回调 
const handleAvatarSuccess = (response) => {
    console.log('【调试】上传结果:', response)
    if (response.code === 200) {
        // 把上传后的 URL 填进去，页面立马显示图片 
        form.avatarUrl = response.data
        ElMessage.success('上传成功')

        // 更新浏览器缓存，让右上角头像也立即更新
        const oldUser = JSON.parse(sessionStorage.getItem('user'))
        const newUser = { ...oldUser, avatarUrl: form.avatarUrl }
        sessionStorage.setItem('user', JSON.stringify(newUser))

        // 触发storage事件，通知其他组件更新
        window.dispatchEvent(new Event('storage'))
    } else {
        ElMessage.error('上传失败')
    }
}

const beforeAvatarUpload = (rawFile) => {
    if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
        ElMessage.error('只能上传 JPG/PNG 格式!')
        return false
    }
    return true
}

// 5. 保存修改 
const saveUserInfo = async () => {
    loading.value = true
    try {
        // 发送给后端更新 
        const res = await request.put('/users/update', form)
        if (res.data.code === 200) {
            ElMessage.success('保存成功')

            // 更新浏览器缓存，让右上角头像也变 
            const oldUser = JSON.parse(sessionStorage.getItem('user'))
            const newUser = { ...oldUser, ...form }
            sessionStorage.setItem('user', JSON.stringify(newUser))

            // 刷新页面 
            setTimeout(() => location.reload(), 500)
        } else {
            ElMessage.error(res.data.msg || '保存失败')
        }
    } catch (err) {
        ElMessage.error('请求失败')
    } finally {
        loading.value = false
    }
}

// 6. 修改密码
const changePassword = async () => {
    // 验证表单
    if (!passwordForm.oldPassword) {
        ElMessage.error('请输入原密码');
        return;
    }
    if (!passwordForm.newPassword) {
        ElMessage.error('请输入新密码');
        return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.error('两次输入的密码不一致');
        return;
    }

    passwordLoading.value = true;
    try {
        const res = await request.post('/users/change-password', {
            id: form.id,
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword
        });
        if (res.data.code === 200) {
            ElMessage.success('密码修改成功');
            // 清空密码表单
            passwordForm.oldPassword = '';
            passwordForm.newPassword = '';
            passwordForm.confirmPassword = '';
        } else {
            ElMessage.error(res.data.msg || '密码修改失败');
        }
    } catch (err) {
        ElMessage.error('请求失败');
    } finally {
        passwordLoading.value = false;
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