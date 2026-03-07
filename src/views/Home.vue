<template>
    <div class="app-main heritage-theme">
        <el-row :gutter="24" class="dashboard-row">
            <el-col :span="12">
                <el-card shadow="hover" class="premium-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">保护级别分布</span>
                        <div class="chart-decoration"></div>
                    </div>
                    <div id="pieChart" style="height: 280px;"></div>
                </el-card>
            </el-col>
            <el-col :span="12">
                <el-card shadow="hover" class="premium-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">存续状态统计</span>
                        <div class="chart-decoration"></div>
                    </div>
                    <div id="barChart" style="height: 280px;"></div>
                </el-card>
            </el-col>
        </el-row>

        <!-- ✨ 新增：通知公告区 -->
        <el-row :gutter="24" class="news-row" style="margin-bottom: 24px;">
            <el-col :span="24">
                <el-card shadow="hover" class="premium-card news-card">
                    <div class="chart-header">
                        <span class="chart-title"><el-icon>
                                <Bell />
                            </el-icon> 动态公告</span>
                        <div class="chart-decoration"></div>
                    </div>
                    <div class="news-list">
                        <div class="news-item">
                            <span class="news-tag new">NEW</span>
                            <span class="news-text">国家级非物质文化遗产生产性保护示范基地名单公示</span>
                            <span class="news-date">03-01</span>
                        </div>
                        <div class="news-item">
                            <span class="news-tag">荐</span>
                            <span class="news-text">"非遗进校园"优秀实践案例征集活动启动</span>
                            <span class="news-date">02-28</span>
                        </div>
                        <div class="news-item">
                            <span class="news-tag">告</span>
                            <span class="news-text">关于加强传统技艺类非遗项目版权保护的通知</span>
                            <span class="news-date">02-25</span>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <div class="premium-toolbar">
            <div class="search-group">
                <div class="search-input-container">
                    <el-input v-model="searchName" placeholder="输入非遗项目名称..." class="rounded-input" size="large"
                        clearable @clear="fetchData" @keyup.enter="handleSearch" :prefix-icon="Search"
                        @focus="showSearchHistory = true" @blur="() => hideSearchHistory()" />

                    <!-- 搜索历史下拉菜单 -->
                    <div v-if="showSearchHistory && searchHistory.length > 0 && userInfo.id"
                        class="search-history-dropdown">
                        <div class="search-history-header">
                            <span>搜索历史</span>
                            <el-button link type="primary" size="small" @click.stop="clearSearchHistory">清空</el-button>
                        </div>
                        <div class="search-history-list">
                            <div v-for="(keyword, index) in searchHistory" :key="index" class="search-history-item"
                                @click.stop="selectSearchHistory(keyword)">
                                <el-icon class="history-icon">
                                    <Clock />
                                </el-icon>
                                <span>{{ keyword }}</span>
                            </div>
                        </div>
                    </div>
                </div>
                <el-select v-model="searchLevel" placeholder="筛选保护级别" size="large" clearable @clear="fetchData"
                    class="rounded-select">
                    <el-option label="国家级" value="国家级" />
                    <el-option label="省级" value="省级" />
                    <el-option label="市级" value="市级" />
                    <el-option label="县级" value="县级" />
                </el-select>
                <el-button type="primary" size="large" @click="handleSearch" class="btn-explore">
                    开始探索
                </el-button>
            </div>

            <div class="admin-actions" v-if="userInfo.role === 'admin'">
                <el-button type="danger" plain round @click="handleBatchDelete"
                    :disabled="multipleSelection.length === 0">
                    <el-icon>
                        <Delete />
                    </el-icon> 批量删除
                </el-button>
                <el-button type="success" round @click="openAddDialog" class="btn-add">
                    <el-icon>
                        <Plus />
                    </el-icon> 新增项目
                </el-button>
                <el-button type="warning" round @click="handleExport" class="btn-export">
                    <el-icon>
                        <Download />
                    </el-icon> 导出名录
                </el-button>
            </div>
        </div>

        <div class="gallery-container" v-loading="loading">
            <el-empty v-if="tableData.length === 0" description="暂无相关非遗传承项目" :image-size="160" />

            <el-row :gutter="24">
                <el-col v-for="item in tableData" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6" class="gallery-col">
                    <el-card class="heritage-card" shadow="hover" :body-style="{ padding: '0px' }"
                        @click="handleEdit(item)">
                        <div class="card-cover">
                            <div class="admin-checkbox" v-if="userInfo.role === 'admin'" @click.stop>
                                <el-checkbox v-model="item.selected" @change="handleCardSelectionChange" size="large" />
                            </div>

                            <el-image
                                :src="buildStaticUrl(item.coverUrl) || 'https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?q=80&w=2069&auto=format&fit=crop'"
                                class="cover-img" fit="cover" loading="lazy" />

                            <div class="badge-level" :class="getLevelClass(item.protectLevel)">
                                {{ item.protectLevel }}
                            </div>

                            <div v-if="item.videoUrl" class="video-overlay">
                                <el-icon>
                                    <VideoPlay />
                                </el-icon>
                            </div>
                        </div>

                        <div class="card-content">
                            <div class="card-header-row">
                                <h3 class="card-title" :title="item.name">{{ item.name }}</h3>
                                <el-icon v-if="userInfo.id" class="fav-icon">
                                    <Star />
                                </el-icon>
                            </div>

                            <div class="card-tags">
                                <el-tag size="small" effect="dark"
                                    :color="item.status === '在传承' ? '#67C23A' : '#F56C6C'" style="border:none" round>
                                    {{ item.status }}
                                </el-tag>
                                <el-tag size="small" type="info" effect="plain" round
                                    style="margin-left: 8px; border-color: #dcdfe6; color: #606266;">
                                    {{ getCategoryName(item.categoryId) }}
                                </el-tag>
                            </div>

                            <div class="card-footer">
                                <div class="inheritor-info" @click="handleInheritorClick(item.id)">
                                    <span class="label">传承人:</span>
                                    <span class="value"
                                        :class="{ 'clickable': userInfo.role === 'admin' && item.inheritorNames && item.inheritorNames !== '\u6682\u65e0\u8bb0\u5f55' }">{{
                                            item.inheritorNames || '\u6682\u65e0\u8bb0\u5f55' }}</span>
                                    <el-icon v-if="userInfo.role === 'admin' && item.inheritorNames && item.inheritorNames !== '\u6682\u65e0\u8bb0\u5f55'"
                                        class="inheritor-link-icon">
                                        <Link />
                                    </el-icon>
                                </div>
                                <div class="card-stats">
                                    <span class="view-count">
                                        <el-icon size="14">
                                            <View />
                                        </el-icon>
                                        {{ item.viewCount || 0 }}
                                    </span>
                                </div>
                                <div class="card-actions">
                                    <el-button v-if="userInfo.role !== 'admin'"
                                        :type="favoritedProjects.includes(item.id) ? 'warning' : ''" circle size="small"
                                        @click.stop="toggleCardFavorite(item.id)" class="favorite-btn">
                                        <el-icon>
                                            <StarFilled v-if="favoritedProjects.includes(item.id)" />
                                            <Star v-else />
                                        </el-icon>
                                    </el-button>
                                    <div class="admin-btns" v-if="userInfo.role === 'admin'" @click.stop>
                                        <el-button link type="primary" @click="handleEdit(item)">编辑</el-button>
                                        <el-button link type="danger" @click="handleDelete(item.id)">删除</el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <div class="pagination-container">
            <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
                @current-change="handlePageChange" />
        </div>

        <el-dialog v-model="dialogVisible" width="750px" top="5vh" :close-on-click-modal="false" destroy-on-close
            class="elegant-dialog">
            <template #header="{ titleId, titleClass }">
                <div class="my-dialog-header">
                    <h4 :id="titleId" :class="titleClass">
                        <span class="dialog-title-accent"></span>
                        {{ form.id ? (userInfo.role === 'admin' ? '编辑项目信息' : '非遗项目概览') : '新增非遗项目' }}
                    </h4>
                    <el-button v-if="form.id && userInfo.role !== 'admin'" :type="isFavorited ? 'warning' : ''" circle
                        size="default" @click="toggleFavorite"
                        style="margin-right: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                        <el-icon>
                            <StarFilled v-if="isFavorited" />
                            <Star v-else />
                        </el-icon>
                    </el-button>
                </div>
            </template>

            <div class="dialog-body">
                <el-form :model="form" label-width="90px" :disabled="userInfo.role !== 'admin' && form.id">
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-form-item label="项目名称">
                                <el-input v-model="form.name" placeholder="请输入名称" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="保护级别">
                                <el-select v-model="form.protectLevel" style="width: 100%">
                                    <el-option label="国家级" value="国家级" />
                                    <el-option label="省级" value="省级" />
                                    <el-option label="市级" value="市级" />
                                    <el-option label="县级" value="县级" />
                                </el-select>
                            </el-form-item>
                        </el-col>
                    </el-row>

                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-form-item label="所属类别">
                                <el-select v-model="form.categoryId" style="width: 100%">
                                    <el-option label="民间文学" :value="1" />
                                    <el-option label="传统音乐" :value="2" />
                                    <el-option label="传统舞蹈" :value="3" />
                                    <el-option label="传统戏剧" :value="4" />
                                    <el-option label="曲艺" :value="5" />
                                    <el-option label="传统体育、游艺与杂技" :value="6" />
                                    <el-option label="传统美术" :value="7" />
                                    <el-option label="传统技艺" :value="8" />
                                    <el-option label="传统医药" :value="9" />
                                    <el-option label="民俗" :value="10" />
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="状态">
                                <el-select v-model="form.status" style="width: 100%">
                                    <el-option label="在传承" value="在传承" />
                                    <el-option label="濒危" value="濒危" />
                                </el-select>
                            </el-form-item>
                        </el-col>
                    </el-row>

                    <el-form-item label="封面图片">
                        <el-upload class="avatar-uploader" :action="fileUploadAction" :headers="uploadHeaders"
                            :show-file-list="false" :on-success="handleAvatarSuccess"
                            :before-upload="beforeAvatarUpload" :disabled="userInfo.role !== 'admin' && form.id">
                            <img v-if="form.coverUrl" :src="buildStaticUrl(form.coverUrl)" class="avatar" />
                            <el-icon v-else class="avatar-uploader-icon">
                                <Plus />
                            </el-icon>
                        </el-upload>
                    </el-form-item>

                    <el-form-item label="宣传视频">
                        <el-upload class="upload-demo" :action="fileUploadAction" :headers="uploadHeaders"
                            :on-success="handleVideoSuccess" :before-upload="beforeVideoUpload" :show-file-list="false"
                            :disabled="userInfo.role !== 'admin' && form.id">
                            <el-button type="primary" plain round v-if="!form.videoUrl">上传视频</el-button>
                            <div v-else style="display: flex; align-items: center; gap: 10px;">
                                <el-tag type="success" effect="light" round>视频已就绪</el-tag>
                                <el-button link type="danger" @click.stop="form.videoUrl = ''"
                                    v-if="userInfo.role === 'admin'">移除</el-button>
                            </div>
                        </el-upload>
                        <video v-if="form.videoUrl" :src="buildStaticUrl(form.videoUrl)" controls
                            style="width: 100%; max-height: 240px; margin-top: 15px; border-radius: 8px; background: #000; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"></video>
                    </el-form-item>

                    <el-form-item label="项目简介">
                        <div class="quill-container">
                            <QuillEditor ref="quillRef" v-model:content="form.history" contentType="html" theme="snow"
                                :toolbar="toolbarOptions" @ready="onQuillReady" style="height: 220px;"
                                :readOnly="userInfo.role !== 'admin' && form.id" />
                        </div>
                    </el-form-item>
                </el-form>

                <div v-if="form.id" class="comment-section">
                    <div class="section-title">
                        <el-icon color="#c41e3a">
                            <ChatLineRound />
                        </el-icon>
                        <span>印象与交流 ({{ commentList.length }})</span>
                    </div>

                    <div class="comment-list">
                        <div v-if="commentList.length === 0" class="empty-comment">暂无留言，写下您的文化初体验吧~</div>
                        <div v-for="item in commentList" :key="item.id" class="comment-item">
                            <el-avatar :size="36" :src="getAvatarUrl(item.avatarUrl)" />
                            <div class="comment-content">
                                <div class="comment-header">
                                    <span class="nickname">{{ item.nickname || '匿名文化使者' }}</span>
                                    <span class="time">{{ item.createdAt }}</span>
                                </div>
                                <div class="text">{{ item.content }}</div>
                                <div v-if="userInfo.role === 'admin' || userInfo.id === item.userId"
                                    style="text-align: right; margin-top: 5px;">
                                    <el-button type="danger" link size="small"
                                        @click="handleDeleteComment(item.id)">撤回留言</el-button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="comment-input" v-if="userInfo.id">
                        <el-input v-model="newComment" placeholder="分享您的见解..." @keyup.enter="submitComment"
                            class="custom-append">
                            <template #append>
                                <el-button type="primary" @click="submitComment">发表</el-button>
                            </template>
                        </el-input>
                    </div>
                </div>
            </div>

            <template #footer>
                <el-button round @click="dialogVisible = false">关闭</el-button>
                <el-button round type="primary" @click="saveProject" :loading="btnLoading"
                    v-if="userInfo.role === 'admin'"
                    style="background-color: #c41e3a; border-color: #c41e3a;">保存更改</el-button>
            </template>
        </el-dialog>
    </div>
    <input type="file" id="quill-img-input" style="display: none" accept="image/*" @change="uploadQuillImage" />
</template>

<script setup lang="ts">
// ✨ 注意：引入了 onUnmounted 和 ElNotification
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Search, Download, Star, StarFilled, ChatLineRound, VideoPlay, Delete, Clock, View, Link, Bell } from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import axios from 'axios'
import request from '@/utils/request'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { buildApiUrl, buildStaticUrl, buildWsUrl, getAuthHeaders } from '@/utils/url'
import type { UploadProps } from 'element-plus'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use, init, getInstanceByDom, graphic } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

// 路由
const route = useRoute()
const router = useRouter()

// --- 状态定义 ---
const userInfo = ref<any>({})
const tableData = ref<any[]>([])
const total = ref(0)
const pageSize = ref(8)
const currentPage = ref(1)
const loading = ref(false)
const dialogVisible = ref(false)
const searchName = ref('')
const searchLevel = ref('')
const btnLoading = ref(false)
const multipleSelection = ref<number[]>([])

const isFavorited = ref(false)
const favoritedProjects = ref<number[]>([])
const commentList = ref<any[]>([])
const newComment = ref('')

// 搜索历史相关
const searchHistory = ref<string[]>([])
const showSearchHistory = ref(false)

// 浏览量相关
const viewCounts = ref<Record<number, number>>({})
const hideSearchHistory = () => { setTimeout(() => showSearchHistory.value = false, 200) } // ✨ 修复：setTimeout 访问问题

const quillRef = ref()
const toolbarOptions = [
    ['bold', 'italic', 'underline'],
    [{ 'header': 1 }, { 'header': 2 }],
    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
    ['image']
]
const onQuillReady = (quill: any) => {
    const toolbar = quill.getModule('toolbar')
    toolbar.addHandler('image', () => {
        document.getElementById('quill-img-input')?.click()
    })
}

const fileUploadAction = buildApiUrl('/file/upload')
const uploadHeaders = getAuthHeaders()

const form = reactive({
    id: undefined,
    name: '',
    protectLevel: '国家级',
    status: '在传承',
    categoryId: 1,
    regionId: 1,
    history: '',
    coverUrl: '',
    videoUrl: '',
    auditStatus: 1
})

// ================== WebSocket 实时同步引擎 ==================
let ws: WebSocket | null = null

const initWebSocket = () => {
    // 建立连接 (注意端口号需与 SpringBoot 保持一致)
    ws = new WebSocket(buildWsUrl('/ws/project'))

    ws.onopen = () => {
        console.log('✅ [WebSocket] 实时数据同步管道已连接')
    }

    ws.onmessage = (event) => {
        console.log('【前端诊断】收到后端WS消息:', event.data)

        // 当收到后端发来的更新信号时
        if (event.data === 'PROJECT_DATA_CHANGED' || event.data.includes('PROJECT_DATA_CHANGED')) {
            // 1. 弹出一个优雅的右上角提示
            ElNotification({
                title: '展馆数据更新',
                message: '管理员刚刚更新了文化资产档案，已为您无感同步。',
                type: 'success',
                position: 'top-right',
                duration: 4000
            })

            // 2. 静默刷新页面数据和图表
            fetchData()
            initCharts()
        }
    }

    ws.onerror = () => {
        console.error('❌ [WebSocket] 连接失败，请检查后端或登录拦截器')
    }

    ws.onclose = () => {
        console.log('⚠️ [WebSocket] 同步管道已断开')
    }
}

// --- 核心业务逻辑 ---
const fetchData = async () => {
    loading.value = true
    try {
        const res = await request.get('/projects/page', {
            params: {
                pageNum: currentPage.value,
                pageSize: pageSize.value,
                name: searchName.value,
                protectLevel: searchLevel.value
            }
        })
        const page = res.data.data || { records: [], total: 0 }
        tableData.value = (page.records || []).map((item: any) => ({
            ...item,
            inheritorNames: item.inheritorNames || '暂无记录',
            selected: false
        }))
        total.value = page.total || 0
        multipleSelection.value = []
    } catch (error) {
        console.error('获取项目列表失败', error)
        ElMessage.error('获取项目列表失败，请稍后重试')
        tableData.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const loadProjectDetail = async (projectId: number) => {
    const current = tableData.value.find(item => item.id === projectId)
    if (current) {
        handleEdit(current)
        return
    }
    try {
        const res = await request.get(`/projects/${projectId}`)
        if (res.data.code === 200) {
            handleEdit({ ...res.data.data, inheritorNames: res.data.data?.inheritorNames || '暂无记录' })
        }
    } catch (error) {
        console.error(`获取项目 ${projectId} 详情失败`, error)
        ElMessage.error('打开项目详情失败')
    }
}


// 互动功能
const checkFavoriteStatus = async (projectId: number) => {
    if (!userInfo.value.id) return
    const res = await request.get('/favorites/check', { params: { projectId } })
    isFavorited.value = res.data.data
}

const toggleFavorite = async () => {
    const res = await request.post('/favorites/toggle', { projectId: form.id })
    isFavorited.value = res.data.data
    isFavorited.value ? ElMessage.success('已加入您的雅集收藏') : ElMessage.info('已移出收藏')
    // 更新收藏项目列表
    await loadFavoritedProjects()
}

// 加载用户收藏的项目ID列表
const loadFavoritedProjects = async () => {
    if (!userInfo.value.id) return
    try {
        const res = await request.get('/favorites/list', {
            params: { pageNum: 1, pageSize: 100 }
        })
        if (res.data.code === 200) {
            favoritedProjects.value = res.data.data.records.map((item: any) => item.id)
        }
    } catch (error) {
        console.error('加载收藏项目失败', error)
    }
}

// 切换卡片上的收藏状态
const toggleCardFavorite = async (projectId: number) => {
    if (!userInfo.value.id) return ElMessage.warning('请先登录')
    const res = await request.post('/favorites/toggle', { projectId })
    const isFav = res.data.data
    isFav ? ElMessage.success('已加入您的雅集收藏') : ElMessage.info('已移出收藏')
    // 更新收藏项目列表
    await loadFavoritedProjects()
}

// 搜索历史相关方法
const loadSearchHistory = async () => {
    if (!userInfo.value.id) return
    try {
        const res = await request.get('/search/history', {
            params: { userId: userInfo.value.id }
        })
        if (res.data.code === 200) {
            searchHistory.value = res.data.data
        }
    } catch (error) {
        console.error('加载搜索历史失败', error)
    }
}

const clearSearchHistory = async () => {
    if (!userInfo.value.id) return
    try {
        const res = await request.delete('/search/history', {
            params: { userId: userInfo.value.id }
        })
        if (res.data.code === 200) {
            searchHistory.value = []
            ElMessage.success('搜索历史已清空')
        }
    } catch (error) {
        console.error('清空搜索历史失败', error)
        ElMessage.error('清空搜索历史失败')
    }
}

const selectSearchHistory = (keyword: string) => {
    searchName.value = keyword
    showSearchHistory.value = false
    handleSearch()
}

// 浏览量相关方法
const increaseViewCount = async (projectId: number) => {
    try {
        const res = await request.get('/view/count', {
            params: { projectId }
        })
        if (res.data.code === 200) {
            viewCounts.value[projectId] = res.data.data
            // ✨ 浏览成功后，同步更新到列表对象中
            const item = tableData.value.find(p => p.id === projectId)
            if (item) item.viewCount = res.data.data
        }
    } catch (error) {
        console.error('增加浏览量失败', error)
    }
}

const fetchComments = async (projectId: number) => {
    const res = await request.get('/comments/list', { params: { projectId } })
    commentList.value = res.data.data
}

const submitComment = async () => {
    if (!newComment.value.trim()) return ElMessage.warning('内容不能为空')
    await request.post('/comments/add', { projectId: form.id, content: newComment.value })
    ElMessage.success('留言成功')
    newComment.value = ''
    fetchComments(form.id!)
}

const handleDeleteComment = (commentId: number) => {
    ElMessageBox.confirm('是否撤回该条留言？', '确认操作', { type: 'warning' }).then(async () => {
        const res = await request.delete(`/comments/delete/${commentId}`)
        if (res.data.code === 200) { ElMessage.success('撤回成功'); if (form.id) fetchComments(form.id) }
    }).catch(() => { })
}

// 弹窗与增删改
const openAddDialog = () => {
    Object.assign(form, { id: undefined, name: '', history: '', coverUrl: '', videoUrl: '', protectLevel: '国家级', status: '在传承' })
    dialogVisible.value = true
}

const handleEdit = (row: any) => {
    Object.assign(form, row)
    dialogVisible.value = true
    if (row.id) {
        checkFavoriteStatus(row.id)
        fetchComments(row.id)
        increaseViewCount(row.id)
    }
}

const saveProject = async () => {
    if (!form.name) return ElMessage.warning('项目名称为必填项')
    btnLoading.value = true
    try {
        const res = form.id ? await request.put('/projects/update', form) : await request.post('/projects/add', form)
        if (res.data.code === 200) {
            ElMessage.success('信息已存档')
            dialogVisible.value = false
            fetchData()
            initCharts()
        } else ElMessage.error(res.data.msg)
    } finally { btnLoading.value = false }
}


const handleCardSelectionChange = () => {
    multipleSelection.value = tableData.value.filter(item => item.selected).map(item => item.id)
}

const handleBatchDelete = () => {
    if (multipleSelection.value.length === 0) return
    ElMessageBox.confirm(`将永久移除选中的 ${multipleSelection.value.length} 个项目，是否继续？`, '核心警告', { type: 'error' })
        .then(async () => {
            const res = await request.delete('/projects/delete/batch', { data: multipleSelection.value })
            if (res.data.code === 200) { ElMessage.success('档案已清理'); fetchData(); initCharts() }
        })
}

const handleDelete = (id: number) => {
    ElMessageBox.confirm('此操作不可逆，确认移除该项目吗？', '警告', { type: 'error' })
        .then(async () => {
            const res = await request.delete(`/projects/delete/${id}`)
            if (res.data.code === 200) { ElMessage.success('已移除'); fetchData(); initCharts() }
        })
}

// 文件上传
const handleAvatarSuccess: UploadProps['onSuccess'] = (res) => { if (res.code === 200) form.coverUrl = res.data }
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
    const isImg = file.type === 'image/jpeg' || file.type === 'image/png'
    if (!isImg) ElMessage.error('仅支持 JPG/PNG 格式'); return isImg
}
const handleVideoSuccess: UploadProps['onSuccess'] = (res) => { if (res.code === 200) form.videoUrl = res.data }
const beforeVideoUpload: UploadProps['beforeUpload'] = (file) => {
    const isVideo = file.type.startsWith('video/')
    if (!isVideo) ElMessage.error('仅支持视频文件'); return isVideo
}
const uploadQuillImage = async (e: any) => {
    const file = e.target.files[0]; if (!file) return
    const formData = new FormData(); formData.append('file', file)
    try {
        const res = await request.post('/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
        if (res.data.code === 200) {
            const quill = quillRef.value.getQuill(); const range = quill.getSelection(true)
            quill.insertEmbed(range.index, 'image', buildStaticUrl(res.data.data)); quill.setSelection(range.index + 1)
        }
    } finally { e.target.value = '' }
}

// 其他工具方法
const handleSearch = async () => {
    currentPage.value = 1

    // 记录搜索词
    if (searchName.value.trim() && userInfo.value.id) {
        try {
            await request.post('/search/record', {
                userId: userInfo.value.id,
                keyword: searchName.value.trim()
            })
            // 重新加载搜索历史
            await loadSearchHistory()
        } catch (error) {
            console.error('记录搜索词失败', error)
        }
    }

    fetchData()
}
const handlePageChange = (val: number) => { currentPage.value = val; fetchData() }

// 点击传承人跳转到传承人管理页面
const handleInheritorClick = (projectId: number) => {
    if (userInfo.value.role !== 'admin') return
    router.push({
        path: '/inheritor',
        query: { projectId }
    })
}


const getLevelClass = (level: string) => {
    const map: Record<string, string> = { '国家级': 'level-national', '省级': 'level-provincial', '市级': 'level-city', '县级': 'level-county' }
    return map[level] || 'level-county'
}

const getAvatarUrl = (avatarUrl: string | null) => {
    return buildStaticUrl(avatarUrl) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

const handleExport = async () => {
    try {
        const response = await axios.get(buildApiUrl('/projects/export'), { responseType: 'blob', headers: getAuthHeaders() })
        const blob = new Blob([response.data], { type: response.headers['content-type'] || 'text/csv;charset=utf-8' })
        const downloadUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        const disposition = response.headers['content-disposition'] || ''
        const matchedName = disposition.match(/filename\*=UTF-8''([^;]+)|filename=([^;]+)/i)
        const filename = decodeURIComponent((matchedName?.[1] || matchedName?.[2] || '非遗项目名录.csv').replace(/"/g, '').trim())
        link.href = downloadUrl
        link.download = filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(downloadUrl)
        ElMessage.success('导出成功')
    } catch (error) {
        console.error('导出项目失败', error)
        ElMessage.error('导出失败，请稍后重试')
    }
}

// --- 高级质感 ECharts 初始化 ---
const initCharts = async () => {
    await nextTick()

    // 雅致国风色彩体系
    const heritageColors = ['#c41e3a', '#e6a23c', '#4A90E2', '#2c3e50', '#8fb2c9']

    const pieChartDom = document.getElementById('pieChart')
    if (pieChartDom) {
        getInstanceByDom(pieChartDom)?.dispose()
        const myPieChart = init(pieChartDom)
        try {
            const res = await request.get('/statistics/level')
            if (res.data.code === 200) {
                myPieChart.setOption({
                    color: heritageColors,
                    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.9)', borderColor: '#eee', textStyle: { color: '#333' } },
                    legend: { top: 'bottom', icon: 'circle', itemGap: 20 },
                    series: [{
                        name: '保护级别', type: 'pie', radius: ['45%', '70%'], center: ['50%', '45%'],
                        avoidLabelOverlap: true,
                        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
                        label: { show: false, position: 'center' },
                        emphasis: { label: { show: true, fontSize: '18', fontWeight: 'bold' } },
                        labelLine: { show: false },
                        data: res.data.data
                    }]
                })
            }
        } catch (e) { console.error(e) }
    }

    const barChartDom = document.getElementById('barChart')
    if (barChartDom) {
        getInstanceByDom(barChartDom)?.dispose()
        const myBarChart = init(barChartDom)
        try {
            const res = await request.get('/statistics/status')
            if (res.data.code === 200) {
                const rawData = res.data.data
                const xData = rawData.map((item: any) => item.name)
                const yData = rawData.map((item: any) => item.value)
                myBarChart.setOption({
                    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                    grid: { left: '5%', right: '5%', bottom: '15%', top: '10%', containLabel: true },
                    xAxis: {
                        type: 'category', data: xData,
                        axisLine: { lineStyle: { color: '#e0e6ed' } },
                        axisLabel: { color: '#606266', margin: 15 }
                    },
                    yAxis: {
                        type: 'value',
                        minInterval: 1, // 强制为整数
                        splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
                        axisLabel: { color: '#909399' }
                    },
                    series: [{
                        name: '项目数量', type: 'bar', data: yData, barWidth: '35%',
                        itemStyle: {
                            borderRadius: [6, 6, 0, 0],
                            // 故宫红到暖珊瑚色的高级渐变
                            color: new graphic.LinearGradient(0, 0, 0, 1, [
                                { offset: 0, color: '#e74c3c' },
                                { offset: 1, color: '#c41e3a' }
                            ])
                        }
                    }]
                })
            }
        } catch (e) { console.error(e) }
    }
}

// 窗口自适应
const resizeCharts = () => {
    const pieChart = document.getElementById('pieChart')
    const barChart = document.getElementById('barChart')
    if (pieChart) {
        getInstanceByDom(pieChart as HTMLElement)?.resize()
    }
    if (barChart) {
        getInstanceByDom(barChart as HTMLElement)?.resize()
    }
}

onMounted(async () => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) userInfo.value = JSON.parse(userStr)

    initWebSocket()
    window.addEventListener('resize', resizeCharts)

    await fetchData()
    await initCharts()
    await loadFavoritedProjects()
    await loadSearchHistory()

    const projectId = Number(route.query.id)
    if (Number.isFinite(projectId) && projectId > 0) {
        await loadProjectDetail(projectId)
    }
})

onUnmounted(() => {
    if (ws) {
        ws.close()
    }
    window.removeEventListener('resize', resizeCharts)
})


// ✨ 新增：非遗类别 ID 到名称的转换映射
// 非遗类别 ID 到名称的映射
const getCategoryName = (id: number) => {
    const categoryMap: Record<number, string> = {
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
    };
    return categoryMap[id] || '其他类别';
}
</script>

<style scoped>
/* ================= 雅致国风 主题变量 ================= */
.heritage-theme {
    --brand-red: #c41e3a;
    /* 故宫红 */
    --brand-red-hover: #a81a32;
    --brand-blue: #2c3e50;
    /* 黛蓝 */
    --brand-gold: #e6a23c;
    /* 琉璃黄 */
    --bg-color: #f4f6f9;
    --card-radius: 16px;
    --input-radius: 20px;
    --shadow-soft: 0 4px 16px rgba(0, 0, 0, 0.04);
    --shadow-hover: 0 12px 32px rgba(196, 30, 58, 0.12);
}

/* ================= 全局底色与纹理 ================= */
.app-main {
    padding: 24px;
    background-color: var(--bg-color);
    min-height: calc(100vh - 60px);
    background-image: radial-gradient(#dcdfe6 1px, transparent 1px);
    background-size: 24px 24px;
}

/* ================= 顶部数据卡片 ================= */
.dashboard-row {
    margin-bottom: 24px;
}

.premium-card {
    border: none;
    border-radius: var(--card-radius);
    box-shadow: var(--shadow-soft);
    background: #fff;
    padding: 20px;
    transition: box-shadow 0.3s ease;
}

.premium-card:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.premium-card :deep(.el-card__body) {
    padding: 0;
}

.chart-header {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px dashed #ebeef5;
}

.chart-title {
    font-size: 18px;
    font-weight: bold;
    color: var(--brand-blue);
    letter-spacing: 1px;
}

.chart-decoration {
    margin-left: 12px;
    height: 4px;
    width: 24px;
    background: var(--brand-red);
    border-radius: 2px;
}

/* ================= 精致探索工具栏 ================= */
.premium-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;
    padding: 16px 24px;
    border-radius: var(--card-radius);
    box-shadow: var(--shadow-soft);
    margin-bottom: 32px;
}

.search-group {
    display: flex;
    gap: 16px;
    flex: 1;
    max-width: 650px;
}

.rounded-input :deep(.el-input__wrapper),
.rounded-select :deep(.el-select__wrapper) {
    border-radius: var(--input-radius);
    box-shadow: 0 0 0 1px #e4e7ed inset;
    background-color: #fafafa;
}

.rounded-input :deep(.el-input__wrapper.is-focus),
.rounded-select :deep(.el-select__wrapper.is-focused) {
    box-shadow: 0 0 0 1.5px var(--brand-red) inset !important;
    background-color: #fff;
}

.btn-explore {
    background-color: var(--brand-red);
    border-color: var(--brand-red);
    border-radius: var(--input-radius);
    padding: 0 28px;
    font-weight: bold;
    letter-spacing: 2px;
    transition: all 0.3s;
}

.btn-explore:hover {
    background-color: var(--brand-red-hover);
    border-color: var(--brand-red-hover);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(196, 30, 58, 0.4);
}

/* ================= 搜索历史样式 ================= */
.search-input-container {
    position: relative;
    flex: 1;
    min-width: 300px;
}

.search-history-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 0 0 8px 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    z-index: 1000;
    margin-top: 4px;
}

.search-history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    font-size: 14px;
    color: #606266;
}

.search-history-list {
    max-height: 200px;
    overflow-y: auto;
}

.search-history-item {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    cursor: pointer;
    transition: background-color 0.2s;
}

.search-history-item:hover {
    background-color: #f5f7fa;
}

.history-icon {
    margin-right: 8px;
    color: #909399;
    font-size: 14px;
}

/* ================= 浏览量样式 ================= */
.card-footer {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
}

.inheritor-info {
    flex: 1;
    min-width: 0;
}

.card-stats {
    flex: 0 0 auto;
    padding: 0 12px;
}

.card-actions {
    flex: 0 0 auto;
    display: flex;
    align-items: flex-end;
    gap: 4px;
}

.admin-btns {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;
}

.admin-btns .el-button {
    padding: 2px 4px;
    font-size: 12px;
    line-height: 1.2;
}

.view-count {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #909399;
}

.view-count .el-icon {
    margin-right: 4px;
}

.admin-actions {
    display: flex;
    gap: 12px;
}

/* ================= 热度排行样式 ================= */
.hot-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.hot-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
}

.hot-item:hover {
    background: #fdf2f2;
    transform: translateX(4px);
}

.hot-rank {
    width: 24px;
    height: 24px;
    line-height: 24px;
    text-align: center;
    border-radius: 4px;
    font-weight: bold;
    font-size: 14px;
    margin-right: 12px;
    background: #f0f2f5;
    color: #909399;
}

.rank-1 {
    background: #ff4d4f;
    color: #fff;
}

.rank-2 {
    background: #ffa940;
    color: #fff;
}

.rank-3 {
    background: #ffec3d;
    color: #333;
}

.hot-thumb {
    width: 48px;
    height: 48px;
    border-radius: 4px;
    margin-right: 12px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.hot-more {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: 16px;
    padding: 8px 0;
    border-top: 1px solid #f0f0f0;
    cursor: pointer;
    transition: all 0.3s ease;
}

.hot-more:hover {
    color: #dc2626;
}

.hot-more span {
    margin-right: 4px;
    font-size: 14px;
}

.hot-more-icon {
    font-size: 14px;
    transition: transform 0.3s ease;
}

.hot-more:hover .hot-more-icon {
    transform: translateX(4px);
}

.hot-info {
    flex: 1;
}

.hot-name {
    font-weight: 500;
    color: #2c3e50;
    font-size: 14px;
    margin-bottom: 4px;
}

.hot-views {
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;
}

.news-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.news-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #606266;
    padding: 4px 0;
    cursor: pointer;
}

.news-item:hover .news-text {
    color: var(--brand-red);
}

.news-tag {
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 4px;
    margin-right: 10px;
    background: #f0f2f5;
    color: #909399;
}

.news-tag.new {
    background: #fff1f0;
    color: #ff4d4f;
    border: 1px solid #ffccc7;
}

.news-text {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.news-date {
    color: #999;
    font-size: 12px;
    margin-left: 10px;
}

/* ================= 展馆卡片区 ================= */
.gallery-container {
    min-height: 400px;
}

.gallery-col {
    margin-bottom: 28px;
}

.heritage-card {
    border: none;
    border-radius: var(--card-radius);
    overflow: hidden;
    cursor: pointer;
    background: #fff;
    box-shadow: var(--shadow-soft);
    transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
    position: relative;
}

.heritage-card:hover {
    transform: translateY(-8px);
    box-shadow: var(--shadow-hover);
}

.card-cover {
    position: relative;
    height: 210px;
    overflow: hidden;
}

.cover-img {
    width: 100%;
    height: 100%;
    transition: transform 0.6s ease;
    filter: brightness(0.95);
}

.heritage-card:hover .cover-img {
    transform: scale(1.08);
    filter: brightness(1.05);
}

.badge-level {
    position: absolute;
    top: 16px;
    left: 16px;
    padding: 4px 14px;
    font-size: 12px;
    font-weight: bold;
    color: #fff;
    border-radius: 20px;
    backdrop-filter: blur(4px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    z-index: 2;
    letter-spacing: 1px;
}

.level-national {
    background: linear-gradient(135deg, #c41e3a, #e74c3c);
}

.level-provincial {
    background: linear-gradient(135deg, #e6a23c, #f39c12);
}

.level-city {
    background: linear-gradient(135deg, #3498db, #2980b9);
}

.level-county {
    background: linear-gradient(135deg, #95a5a6, #7f8c8d);
}

.video-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.2);
    opacity: 0;
    transition: all 0.3s;
    z-index: 1;
}

.heritage-card:hover .video-overlay {
    opacity: 1;
    backdrop-filter: blur(2px);
}

.video-overlay .el-icon {
    font-size: 54px;
    color: #fff;
    filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.5));
    transform: scale(0.8);
    transition: 0.3s;
}

.heritage-card:hover .video-overlay .el-icon {
    transform: scale(1);
}

.card-content {
    padding: 20px;
}

.card-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.card-title {
    margin: 0;
    font-size: 19px;
    color: var(--brand-blue);
    font-weight: bold;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
}

.fav-icon {
    font-size: 20px;
    color: #dcdfe6;
    transition: 0.3s;
}

.heritage-card:hover .fav-icon {
    color: var(--brand-gold);
}

.card-tags {
    margin-bottom: 16px;
    display: flex;
    gap: 8px;
}

.card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 14px;
    border-top: 1px solid #f0f2f5;
    font-size: 13px;
}

.inheritor-info .label {
    color: #909399;
    margin-right: 6px;
}

.inheritor-info .value {
    color: #303133;
    font-weight: 500;
}

.inheritor-info .value.clickable {
    color: #409EFF;
    cursor: pointer;
    text-decoration: underline;
    transition: all 0.3s ease;
}

.inheritor-info .value.clickable:hover {
    color: #67C23A;
    text-decoration: none;
}

.inheritor-link-icon {
    margin-left: 4px;
    color: #409EFF;
    font-size: 12px;
    transition: all 0.3s ease;
}

.inheritor-info:hover .inheritor-link-icon {
    color: #67C23A;
    transform: translateX(2px);
}

.inheritor-info {
    cursor: pointer;
    display: flex;
    align-items: center;
    transition: all 0.3s ease;
    padding: 4px 8px;
    border-radius: 4px;
}

.inheritor-info:hover {
    background-color: rgba(64, 158, 255, 0.1);
}

.admin-checkbox {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 10;
    background: rgba(255, 255, 255, 0.85);
    border-radius: 8px;
    padding: 2px 6px;
}

/* ================= 分页器 ================= */
.pagination-container {
    display: flex;
    justify-content: center;
    margin: 40px 0;
}

.pagination-container :deep(.el-pagination.is-background .el-pager li.is-active) {
    background-color: var(--brand-red) !important;
    color: #fff;
    box-shadow: 0 4px 10px rgba(196, 30, 58, 0.3);
    border-radius: 6px;
}

.pagination-container :deep(.el-pagination.is-background .el-pager li) {
    border-radius: 6px;
    background: #fff;
    border: 1px solid #e4e7ed;
}

/* ================= 统计卡片样式 ================= */
.stat-card .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.stat-card .stat-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.stat-card .stat-decoration {
    width: 20px;
    height: 4px;
    background: linear-gradient(90deg, #409EFF, #67C23A);
    border-radius: 2px;
}

.stat-card .stat-content {
    text-align: center;
    padding: 20px 0;
}

.stat-card .stat-number {
    font-size: 36px;
    font-weight: 700;
    color: #c41e3a;
    line-height: 1;
    margin-bottom: 8px;
}

.stat-card .stat-label {
    font-size: 14px;
    color: #909399;
}

/* ================= 弹窗美化 ================= */
.elegant-dialog :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.elegant-dialog :deep(.el-dialog__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f2f5;
    background: #fafafa;
    margin-right: 0;
}

.elegant-dialog :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #f0f2f5;
    background: #fafafa;
}

.my-dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
}

.my-dialog-header h4 {
    font-size: 18px;
    color: var(--brand-blue);
    font-weight: bold;
    margin: 0;
    display: flex;
    align-items: center;
}

.dialog-title-accent {
    display: inline-block;
    width: 4px;
    height: 16px;
    background: var(--brand-red);
    border-radius: 2px;
    margin-right: 10px;
}

.quill-container {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    overflow: hidden;
    transition: 0.3s;
}

.quill-container:hover {
    border-color: #c0c4cc;
}

.avatar-uploader .el-upload {
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: 0.3s;
    background: #fafafa;
}

.avatar-uploader .el-upload:hover {
    border-color: var(--brand-red);
}

.avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 120px;
    height: 120px;
    text-align: center;
    line-height: 120px;
}

.avatar {
    width: 120px;
    height: 120px;
    display: block;
    object-fit: cover;
}

.comment-section {
    margin-top: 30px;
    background: #fafafa;
    padding: 20px;
    border-radius: 12px;
}

.section-title {
    font-weight: bold;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    color: var(--brand-blue);
}

.comment-list {
    max-height: 250px;
    overflow-y: auto;
    margin-bottom: 20px;
    padding-right: 10px;
}

.comment-item {
    display: flex;
    gap: 15px;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px dashed #ebeef5;
}

.comment-item:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
}

.comment-content {
    flex: 1;
}

.nickname {
    font-weight: bold;
    color: var(--brand-blue);
    font-size: 14px;
}

.time {
    color: #909399;
    font-size: 12px;
}

.text {
    font-size: 14px;
    line-height: 1.6;
    color: #606266;
    margin-top: 6px;
    background: #fff;
    padding: 10px 14px;
    border-radius: 0 8px 8px 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.custom-append :deep(.el-input-group__append) {
    background-color: var(--brand-red);
    color: white;
    border-color: var(--brand-red);
    border-radius: 0 20px 20px 0;
}

.custom-append :deep(.el-input__wrapper) {
    border-radius: 20px 0 0 20px;
}
</style>
