<template>
    <div class="layout-container">
        <div class="layout-sidebar" :class="{ 'collapsed': isCollapse }">
            <div class="logo">
                <div class="logo-icon">
                    <span class="logo-chinese">非</span>
                </div>
                <span v-if="!isCollapse" class="logo-text">非遗推广系统</span>
            </div>
            <el-menu active-text-color="#ffd04b" background-color="#304156" text-color="#fff"
                :default-active="activeMenu" class="el-menu-vertical" :collapse="isCollapse" @select="handleMenuSelect">
                <el-menu-item index="home">
                    <el-icon>
                        <DataLine />
                    </el-icon>
                    <span>{{ userInfo.role === 'admin' ? '非遗项目管理' : '非遗数字展馆' }}</span>
                </el-menu-item>

                <el-menu-item index="favorites" v-if="userInfo.role !== 'admin'">
                    <el-icon>
                        <Star />
                    </el-icon>
                    <span>我的收藏</span>
                </el-menu-item>

                <el-menu-item index="hot-ranking">
                    <el-icon>
                        <TrendCharts />
                    </el-icon>
                    <span>热度排行</span>
                </el-menu-item>

                <el-menu-item index="chat">
                    <el-icon>
                        <ChatLineRound />
                    </el-icon>
                    <span>AI 聊天</span>
                </el-menu-item>

                <el-menu-item index="inheritor" v-if="userInfo.role === 'admin'">
                    <el-icon>
                        <User />
                    </el-icon>
                    <span>传承人管理</span>
                </el-menu-item>

                <el-menu-item index="user" v-if="userInfo.role === 'admin'">
                    <el-icon>
                        <Setting />
                    </el-icon>
                    <span>用户管理</span>
                </el-menu-item>
            </el-menu>
        </div>

        <div class="layout-main">
            <div class="layout-header">
                <div class="header-left">
                    <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
                        <Fold v-if="!isCollapse" />
                        <Expand v-else />
                    </el-icon>
                    <el-breadcrumb separator="/">
                        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
                            {{ item.title }}
                        </el-breadcrumb-item>
                    </el-breadcrumb>
                </div>

                <div class="header-right">
                    <el-popover placement="bottom" :width="300" trigger="click">
                        <template #reference>
                            <div style="margin-right: 20px; cursor: pointer; position: relative;">
                                <el-icon size="20">
                                    <Bell />
                                </el-icon>
                            </div>
                        </template>
                        <div style="text-align: center; color: #999; padding: 20px;">暂无新消息</div>
                    </el-popover>

                    <el-dropdown @command="handleCommand">
                        <div style="display: flex; align-items: center; cursor: pointer; outline: none;">
                            <el-avatar :size="30" :src="getAvatarUrl" />
                            <span style="margin-left: 8px; margin-right: 5px;">{{ userInfo.nickname || userInfo.username
                            }}</span>
                            <el-tag size="small" effect="dark" :type="userInfo.role === 'admin' ? 'danger' : 'success'">
                                {{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}
                            </el-tag>
                            <el-icon class="el-icon--right">
                                <ArrowDown />
                            </el-icon>
                        </div>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </div>

            <div class="layout-content">
                <router-view />
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { DataLine, User, Fold, Expand, ArrowDown, Setting, Bell, Star, TrendCharts } from '@element-plus/icons-vue' // 删除了 ChatAssistant 引用
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatAssistant from '../components/ChatAssistant.vue' // 引入
const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)

const userInfo = ref<any>({
    role: 'user',
    nickname: '',
    username: '',
    avatarUrl: ''
})

// 计算头像URL，确保相对路径也能正确显示
const getAvatarUrl = computed(() => {
    if (userInfo.value.avatarUrl) {
        return userInfo.value.avatarUrl.startsWith('http') ? userInfo.value.avatarUrl : `http://localhost:8080${userInfo.value.avatarUrl}`;
    }
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
});

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
    let matched = route.matched.filter(item => item.name)
    const nameMap: any = {
        'home': '项目管理',
        'inheritor': '传承人管理',
        'user': '用户管理',
        'profile': '个人中心'
    }
    return matched.map(item => {
        return {
            path: item.path,
            title: nameMap[item.name as string] || item.name
        }
    })
})

const handleCommand = (command: string) => {
    if (command === 'logout') {
        ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
            sessionStorage.removeItem('user')
            router.push('/login')
            ElMessage.success('退出成功')
        })
    } else if (command === 'profile') {
        router.push('/profile')
    }
}

// 处理菜单选择
const handleMenuSelect = (index: string) => {
    router.push(`/${index}`)
}

onMounted(() => {
    loadUserInfo()
})

// 加载用户信息
const loadUserInfo = () => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
        try {
            userInfo.value = JSON.parse(userStr)
        } catch (e) {
            console.error('用户信息解析失败', e)
        }
    }
}
</script>

<style scoped>
.layout-container {
    display: flex;
    height: 100vh;
    width: 100%;
}

.layout-sidebar {
    background-color: #304156;
    height: 100%;
    transition: width 0.3s;
    display: flex;
    flex-direction: column;
    width: 220px;
}

.layout-sidebar.collapsed {
    width: 64px;
}

.logo {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    background-color: #2b3a4d;
    gap: 10px;
    overflow: hidden;
}

.logo-icon {
    width: 38px;
    height: 38px;
    background: linear-gradient(135deg, #c41e3a 0%, #e6a23c 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 16px rgba(196, 30, 58, 0.4);
    border: 2px solid rgba(255, 255, 255, 0.3);
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease;
}

.logo-icon:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 20px rgba(196, 30, 58, 0.6);
}

.logo-icon::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transform: rotate(45deg);
    animation: shine 3s infinite;
}

@keyframes shine {
    0% {
        transform: translateX(-100%) rotate(45deg);
    }

    100% {
        transform: translateX(100%) rotate(45deg);
    }
}

.logo-chinese {
    font-size: 24px;
    font-weight: bold;
    color: #fff;
    font-family: 'STZhongsong', 'Microsoft YaHei', serif;
    text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.5);
    letter-spacing: 2px;
    position: relative;
    z-index: 1;
}

.logo-text {
    font-size: 16px;
    font-family: 'Microsoft YaHei', serif;
    letter-spacing: 1px;
    white-space: nowrap;
}

.el-menu-vertical {
    border-right: none;
    flex: 1;
}

.layout-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background-color: #f0f2f5;
}

/* 头部样式 */
.layout-header {
    height: 50px;
    background: white;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 15px;
}

/* ✨✨ 修复点：这里必须加上 flex 布局 */
.header-right {
    display: flex;
    align-items: center;
}

.collapse-btn {
    font-size: 20px;
    cursor: pointer;
}

.layout-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
}
</style>