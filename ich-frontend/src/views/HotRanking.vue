<template>
    <div class="hot-ranking-container">
        <div class="page-header">
            <h2 class="page-title">非遗项目热度排行榜</h2>
            <p class="page-subtitle">探索最受欢迎的非遗文化瑰宝</p>
        </div>

        <div class="ranking-content">
            <div v-if="loading" class="loading-container">
                <el-skeleton :rows="8" animated />
            </div>

            <div v-else-if="hotProjects.length === 0" class="empty-container">
                <el-empty description="暂无热度数据" :image-size="200" />
            </div>

            <div v-else class="ranking-list">
                <div v-for="(item, index) in hotProjects" :key="item.projectId" class="ranking-item"
                    :class="{ 'top-ranking': index < 3 }" @click="handleView(item)">
                    <div class="rank-number" :class="{ 'top-rank': index < 3 }">
                        {{ index + 1 }}
                    </div>
                    <div class="project-info">
                        <el-image
                            :src="item.coverUrl || 'https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?q=80&w=2069&auto=format&fit=crop'"
                            class="project-cover" fit="cover" />
                        <div class="project-details">
                            <h3 class="project-title">{{ item.projectName }}</h3>
                            <div class="project-stats">
                                <span class="view-count">
                                    <el-icon size="16">
                                        <View />
                                    </el-icon>
                                    {{ item.viewCount }} 次浏览
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import request from '@/utils/request'
import { View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 状态定义
const hotProjects = ref<any[]>([])
const loading = ref(false)

// 获取热度排行榜数据
const loadHotProjects = async () => {
    loading.value = true
    try {
        const res = await request.get('/view/hot', {
            params: { limit: 20 }
        })
        if (res.data.code === 200) {
            if (res.data.data && res.data.data.length > 0) {
                hotProjects.value = res.data.data.map((item: any) => ({
                    projectId: item.id,
                    projectName: item.name,
                    viewCount: item.viewCount,
                    coverUrl: item.coverUrl
                }))
            } else {
                // 热度排行榜为空，获取所有项目
                const projectRes = await request.get('/projects/page')
                if (projectRes.data.code === 200 && projectRes.data.data && projectRes.data.data.records) {
                    hotProjects.value = projectRes.data.data.records.map((item: any) => ({
                        projectId: item.id,
                        projectName: item.name,
                        viewCount: 0,
                        coverUrl: item.coverUrl
                    }))
                }
            }
        }
    } catch (error) {
        console.error('获取热度排行榜失败', error)
        // 发生错误时，获取所有项目
        try {
            const projectRes = await request.get('/projects/page')
            if (projectRes.data.code === 200 && projectRes.data.data && projectRes.data.data.records) {
                hotProjects.value = projectRes.data.data.records.map((item: any) => ({
                    projectId: item.id,
                    projectName: item.name,
                    viewCount: 0,
                    coverUrl: item.coverUrl
                }))
            }
        } catch (projectError) {
            console.error('获取项目列表失败', projectError)
            ElMessage.error('获取数据失败')
        }
    } finally {
        loading.value = false
    }
}

// 查看项目详情
const handleView = (item: any) => {
    // 跳转到Home页面并传递项目ID
    router.push({ path: '/home', query: { id: item.projectId } })
}

// 生命周期
onMounted(() => {
    loadHotProjects()
})
</script>

<style scoped>
.hot-ranking-container {
    min-height: 80vh;
    padding: 30px 24px;
}

.page-header {
    text-align: center;
    margin-bottom: 40px;
}

.page-title {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 10px;
    font-family: 'Noto Serif SC', serif;
}

.page-subtitle {
    font-size: 16px;
    color: #606266;
    margin: 0;
}

.loading-container {
    margin: 40px 0;
}

.empty-container {
    text-align: center;
    padding: 80px 0;
}

.ranking-list {
    max-width: 800px;
    margin: 0 auto;
}

.ranking-item {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    cursor: pointer;
}

.ranking-item:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
}

.top-ranking {
    background: linear-gradient(135deg, #fff3f3 0%, #fef0f0 100%);
    border: 1px solid #fbc4c4;
}

.rank-number {
    font-size: 32px;
    font-weight: bold;
    color: #909399;
    margin-right: 24px;
    min-width: 48px;
    text-align: center;
}

.top-rank {
    color: #f56c6c;
}

.project-info {
    flex: 1;
    display: flex;
    align-items: center;
}

.project-cover {
    width: 100px;
    height: 100px;
    border-radius: 8px;
    margin-right: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.project-details {
    flex: 1;
}

.project-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 12px 0;
    line-height: 1.4;
}

.project-stats {
    font-size: 14px;
    color: #909399;
}

.view-count {
    display: flex;
    align-items: center;
}

.view-count .el-icon {
    margin-right: 8px;
}

@media (max-width: 768px) {
    .hot-ranking-container {
        padding: 20px 16px;
    }

    .page-title {
        font-size: 24px;
    }

    .ranking-item {
        padding: 16px;
    }

    .rank-number {
        font-size: 24px;
        margin-right: 16px;
        min-width: 36px;
    }

    .project-cover {
        width: 80px;
        height: 80px;
        margin-right: 16px;
    }

    .project-title {
        font-size: 16px;
    }
}
</style>