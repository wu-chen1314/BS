<template>
    <div class="favorites-container">
        <div class="page-header">
            <h2 class="page-title">我的雅集收藏</h2>
            <p class="page-subtitle">珍藏那些触动心灵的非遗之美</p>
        </div>

        <div class="favorites-content">
            <div v-if="loading" class="loading-container">
                <el-skeleton :rows="8" animated />
            </div>

            <div v-else-if="favoritesList.length === 0" class="empty-container">
                <el-empty description="暂无收藏项目" :image-size="200">
                    <el-button type="primary" @click="$router.push('/home')">去发现非遗之美</el-button>
                </el-empty>
            </div>

            <el-row :gutter="24" v-else>
                <el-col :xs="24" :sm="12" :md="8" v-for="item in favoritesList" :key="item.id" class="project-card-col">
                    <el-card :body-style="{ padding: '0' }" class="heritage-card">
                        <div class="card-cover">
                            <img :src="item.coverUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" :alt="item.name" class="cover-image" />
                            <div class="cover-overlay">
                                <el-button link type="primary" text class="view-btn" @click="handleView(item)">
                                    查看详情
                                </el-button>
                            </div>
                        </div>
                        <div class="card-body">
                            <h3 class="card-title">{{ item.name }}</h3>
                            <div class="card-meta">
                                <el-tag size="small" :type="getLevelType(item.protectLevel)" effect="plain" round>
                                    {{ item.protectLevel }}
                                </el-tag>
                                <el-tag size="small" type="info" effect="plain" round style="margin-left: 8px; border-color: #dcdfe6; color: #606266;">
                                    {{ getCategoryName(item.categoryId) }}
                                </el-tag>
                            </div>
                            <div class="card-footer">
                                <div class="inheritor-info">
                                    <span class="label">传承人:</span>
                                    <span class="value">{{ item.inheritorNames || '暂无记录' }}</span>
                                </div>
                                <div class="card-actions">
                                    <el-button :type="'warning'" circle size="small" @click.stop="toggleCardFavorite(item.id)" class="favorite-btn">
                                        <el-icon>
                                            <StarFilled />
                                        </el-icon>
                                    </el-button>
                                </div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <div class="pagination-container" v-if="total > pageSize">
                <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
                    @current-change="handlePageChange" />
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import request from '@/utils/request'
import { StarFilled, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 状态定义
const userInfo = ref<any>({})
const favoritesList = ref<any[]>([])
const total = ref(0)
const pageSize = ref(8)
const currentPage = ref(1)
const loading = ref(false)

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

// 获取收藏列表
const fetchFavorites = async () => {
    if (!userInfo.value.id) return
    
    loading.value = true
    try {
        const res = await request.get('/favorites/list', {
            params: {
                userId: userInfo.value.id,
                pageNum: currentPage.value,
                pageSize: pageSize.value
            }
        })
        
        if (res.data.code === 200) {
            favoritesList.value = res.data.data.records
            total.value = res.data.data.total
        }
    } catch (error) {
        console.error('获取收藏列表失败', error)
        ElMessage.error('获取收藏列表失败')
    } finally {
        loading.value = false
    }
}

// 切换收藏状态
const toggleCardFavorite = async (projectId: number) => {
    if (!userInfo.value.id) return ElMessage.warning('请先登录')
    
    try {
        const res = await request.post('/favorites/toggle', {
            userId: userInfo.value.id,
            projectId: projectId
        })
        
        const isFav = res.data.data
        isFav ? ElMessage.success('已加入您的雅集收藏') : ElMessage.info('已移出收藏')
        
        // 重新加载收藏列表
        await fetchFavorites()
    } catch (error) {
        console.error('切换收藏状态失败', error)
        ElMessage.error('操作失败，请重试')
    }
}

// 查看项目详情
const handleView = (item: any) => {
    // 跳转到Home页面并传递项目ID
    router.push({ path: '/home', query: { id: item.id } })
}

// 获取级别对应的标签类型
const getLevelType = (level: string) => {
    const levelMap: any = {
        '国家级': 'danger',
        '省级': 'warning',
        '市级': 'info',
        '县级': 'success'
    }
    return levelMap[level] || 'info'
}

// 获取类别名称
const getCategoryName = (categoryId: number) => {
    const categoryMap: any = {
        1: '民间文学',
        2: '传统音乐',
        3: '传统舞蹈',
        4: '传统戏剧',
        5: '曲艺',
        6: '传统体育、游艺与杂技',
        7: '传统美术',
        8: '传统技艺',
        9: '传统医药',
        10: '民俗'
    }
    return categoryMap[categoryId] || '其他'
}

// 分页处理
const handlePageChange = (val: number) => {
    currentPage.value = val
    fetchFavorites()
}

// 生命周期
onMounted(() => {
    loadUserInfo()
    fetchFavorites()
})
</script>

<style scoped>
.favorites-container {
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

.project-card-col {
    margin-bottom: 24px;
}

.heritage-card {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
}

.heritage-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
}

.card-cover {
    position: relative;
    height: 180px;
    overflow: hidden;
}

.cover-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
}

.heritage-card:hover .cover-image {
    transform: scale(1.05);
}

.cover-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
    padding: 20px;
    opacity: 0;
    transition: opacity 0.3s ease;
}

.heritage-card:hover .cover-overlay {
    opacity: 1;
}

.view-btn {
    color: #fff !important;
    font-weight: 500;
}

.card-body {
    padding: 16px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 12px 0;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.card-meta {
    margin-bottom: 12px;
}

.card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
}

.inheritor-info {
    flex: 1;
}

.label {
    font-size: 12px;
    color: #909399;
    margin-right: 4px;
}

.value {
    font-size: 14px;
    color: #606266;
}

.card-actions {
    display: flex;
    align-items: center;
}

.favorite-btn {
    margin-left: 12px;
}

.pagination-container {
    margin-top: 40px;
    text-align: center;
}

@media (max-width: 768px) {
    .favorites-container {
        padding: 20px 16px;
    }

    .page-title {
        font-size: 24px;
    }

    .card-cover {
        height: 150px;
    }
}
</style>