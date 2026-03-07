<template>
  <div class="layout-container">
    <aside class="layout-sidebar" :class="{ collapsed: isCollapse }">
      <div class="logo">
        <div class="logo-icon">非遗</div>
        <div v-if="!isCollapse" class="logo-text">
          <div class="logo-title">非遗传承平台</div>
          <div class="logo-subtitle">ICH Promotion</div>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#fff"
        active-text-color="#ffd04b"
        :collapse="isCollapse"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>
            <span>{{ item.title }}</span>
            <el-badge
              v-if="item.index === '/audit-center' && pendingAuditCount > 0"
              :value="pendingAuditCount"
              class="menu-badge"
              type="danger"
            />
          </template>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="layout-main">
      <header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-popover placement="bottom" :width="220" trigger="click">
            <template #reference>
              <div class="notice-trigger">
                <el-badge :value="pendingAuditCount" :hidden="pendingAuditCount === 0">
                  <el-icon size="20"><Bell /></el-icon>
                </el-badge>
              </div>
            </template>
            <div class="notice-content">
              <div v-if="userInfo.role === 'admin'">
                当前待审核项目 {{ pendingAuditCount }} 个。
              </div>
              <div v-else>
                暂无新的系统通知。
              </div>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <div class="user-trigger">
              <el-avatar :size="32" :src="avatarUrl" />
              <div class="user-meta">
                <span class="user-name">{{ userInfo.nickname || userInfo.username || '未登录' }}</span>
                <span class="user-role">{{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="layout-content">
        <router-view />
      </section>
    </main>

    <ChatAssistant />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  ChatLineRound,
  DataLine,
  Document,
  Expand,
  Fold,
  Location,
  Setting,
  Star,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { DEFAULT_AVATAR_URL, toServerUrl } from '@/utils/url'
import ChatAssistant from '@/components/ChatAssistant.vue'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)
const pendingAuditCount = ref(0)

const userInfo = ref<any>({
  role: 'user',
  username: '',
  nickname: '',
  avatarUrl: '',
})

const menuItems = computed(() => {
  const items = [
    { index: '/home', title: '项目首页', icon: DataLine },
    { index: '/favorites', title: '我的收藏', icon: Star, roles: ['user'] },
    { index: '/hot-ranking', title: '热门排行', icon: TrendCharts },
    { index: '/chat', title: 'AI 助手', icon: ChatLineRound },
    { index: '/region-category', title: '地区分类', icon: Location },
    { index: '/audit-center', title: '审核中心', icon: Document, roles: ['admin'] },
    { index: '/inheritor', title: '传承人管理', icon: User, roles: ['admin'] },
    { index: '/user', title: '用户管理', icon: Setting, roles: ['admin'] },
  ]

  return items.filter((item) => !item.roles || item.roles.includes(userInfo.value.role))
})

const activeMenu = computed(() => route.path)

const breadcrumbMap: Record<string, string> = {
  home: '项目首页',
  inheritor: '传承人管理',
  user: '用户管理',
  profile: '个人中心',
  favorites: '我的收藏',
  'hot-ranking': '热门排行',
  chat: 'AI 助手',
  'audit-center': '审核中心',
  'region-category': '地区分类',
  Person: '个人中心',
}

const breadcrumbs = computed(() => {
  return route.matched
    .filter((item) => item.name && item.name !== 'login')
    .map((item) => ({
      path: item.path,
      title: breadcrumbMap[String(item.name)] || String(item.name),
    }))
})

const avatarUrl = computed(() => {
  if (userInfo.value.avatarUrl) {
    return toServerUrl(userInfo.value.avatarUrl)
  }
  return DEFAULT_AVATAR_URL
})

const loadUserInfo = () => {
  const userStr = sessionStorage.getItem('user')
  if (!userStr) {
    return
  }

  try {
    userInfo.value = JSON.parse(userStr)
  } catch (error) {
    console.error('Failed to parse user info:', error)
  }
}

const fetchPendingCount = async () => {
  if (userInfo.value.role !== 'admin') {
    pendingAuditCount.value = 0
    return
  }

  try {
    const res = await request.get('/projects/page', {
      params: { pageNum: 1, pageSize: 1, auditStatus: 0 },
    })
    pendingAuditCount.value = res.data.data?.total || 0
  } catch {
    pendingAuditCount.value = 0
  }
}

const handleMenuSelect = (index: string) => {
  router.push(index)
}

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
      sessionStorage.removeItem('user')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('tokenExpires')
      router.push('/login')
      ElMessage.success('已退出登录')
    } catch {
      return
    }
  }
}

onMounted(async () => {
  loadUserInfo()
  await fetchPendingCount()
})
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.layout-sidebar {
  width: 220px;
  background: #304156;
  color: #fff;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.layout-sidebar.collapsed {
  width: 64px;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: #243243;
  overflow: hidden;
}

.logo-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c41e3a 0%, #e6a23c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.logo-text {
  min-width: 0;
}

.logo-title {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.logo-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.el-menu-vertical {
  border-right: none;
  flex: 1;
}

.menu-badge {
  margin-left: 8px;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-width: 0;
}

.layout-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.notice-trigger {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.notice-content {
  color: #606266;
  line-height: 1.6;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  outline: none;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.layout-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
</style>
