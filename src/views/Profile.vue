<template>
  <div class="profile-page">
    <el-card class="profile-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>个人中心</h2>
            <p>维护头像、昵称和账号安全信息。</p>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本资料" name="profile">
          <el-form class="form-panel" :model="form" label-width="88px">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                :action="avatarUploadUrl"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
              >
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>

            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="saveUserInfo">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form class="form-panel" :model="passwordForm" label-width="88px">
            <el-form-item label="旧密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
            </el-form-item>

            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>

            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="changePassword">更新密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadProps } from 'element-plus'
import request from '@/utils/request'
import { DEFAULT_AVATAR_URL, toApiUrl, toServerUrl } from '@/utils/url'

const activeTab = ref('profile')
const loading = ref(false)
const passwordLoading = ref(false)
const avatarUploadUrl = toApiUrl('/file/upload/avatar')

const form = reactive({
  id: null as number | null,
  username: '',
  nickname: '',
  avatarUrl: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const avatarPreview = computed(() => {
  if (form.avatarUrl) {
    return toServerUrl(form.avatarUrl)
  }
  return DEFAULT_AVATAR_URL
})

const loadData = async (id: number) => {
  const res = await request.get(`/users/${id}`)
  const userData = res.data.data
  Object.assign(form, {
    id: userData.id,
    username: userData.username,
    nickname: userData.nickname,
    avatarUrl: userData.avatarUrl || userData.avatar || '',
  })
}

const persistSessionUser = () => {
  const userStr = sessionStorage.getItem('user')
  if (!userStr) {
    return
  }

  try {
    const currentUser = JSON.parse(userStr)
    sessionStorage.setItem(
      'user',
      JSON.stringify({
        ...currentUser,
        nickname: form.nickname,
        avatarUrl: form.avatarUrl,
      }),
    )
    window.dispatchEvent(new Event('storage'))
  } catch (error) {
    console.error('Failed to update session user:', error)
  }
}

const handleAvatarSuccess: UploadProps['onSuccess'] = (response: any) => {
  if (response.code === 200) {
    form.avatarUrl = response.data
    persistSessionUser()
    ElMessage.success('头像上传成功')
    return
  }

  ElMessage.error(response.msg || '头像上传失败')
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(rawFile.type)) {
    ElMessage.error('仅支持 JPG、PNG、WEBP 图片')
    return false
  }
  return true
}

const saveUserInfo = async () => {
  if (!form.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }

  loading.value = true
  try {
    const res = await request.put('/users/update', form)
    if (res.data.code === 200) {
      persistSessionUser()
      ElMessage.success('资料已保存')
      return
    }
    ElMessage.error(res.data.msg || '保存失败')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

const changePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  passwordLoading.value = true
  try {
    const res = await request.post('/users/change-password', {
      id: form.id,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })

    if (res.data.code === 200) {
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      ElMessage.success('密码修改成功')
      return
    }

    ElMessage.error(res.data.msg || '密码修改失败')
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

onMounted(async () => {
  const userStr = sessionStorage.getItem('user')
  if (!userStr) {
    return
  }

  try {
    const user = JSON.parse(userStr)
    await loadData(user.id)
  } catch (error) {
    console.error('Failed to load profile:', error)
  }
})
</script>

<style scoped>
.profile-page {
  padding: 24px;
}

.profile-card {
  max-width: 860px;
}

.card-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.card-header p {
  margin: 6px 0 0;
  color: #909399;
}

.form-panel {
  max-width: 560px;
  margin: 24px auto 8px;
}

.avatar-uploader :deep(.el-upload) {
  width: 112px;
  height: 112px;
  border: 1px dashed #dcdfe6;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar {
  width: 112px;
  height: 112px;
  object-fit: cover;
  display: block;
}
</style>
