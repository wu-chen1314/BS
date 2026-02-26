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

        <div class="premium-toolbar">
            <div class="search-group">
                <el-input v-model="searchName" placeholder="输入非遗项目名称..." class="rounded-input" size="large" clearable
                    @clear="fetchData" @keyup.enter="handleSearch" :prefix-icon="Search" />
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
                                :src="item.coverUrl || 'https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?q=80&w=2069&auto=format&fit=crop'"
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
                                <div class="inheritor-info">
                                    <span class="label">传承人:</span>
                                    <span class="value">{{ item.inheritorNames || '暂无记录' }}</span>
                                </div>
                                <div class="admin-btns" v-if="userInfo.role === 'admin'" @click.stop>
                                    <el-button link type="primary" @click="handleEdit(item)">编辑</el-button>
                                    <el-button link type="danger" @click="handleDelete(item.id)">删除</el-button>
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
                        <el-upload class="avatar-uploader" action="http://localhost:8080/api/file/upload"
                            :show-file-list="false" :on-success="handleAvatarSuccess"
                            :before-upload="beforeAvatarUpload" :disabled="userInfo.role !== 'admin' && form.id">
                            <img v-if="form.coverUrl" :src="form.coverUrl" class="avatar" />
                            <el-icon v-else class="avatar-uploader-icon">
                                <Plus />
                            </el-icon>
                        </el-upload>
                    </el-form-item>

                    <el-form-item label="宣传视频">
                        <el-upload class="upload-demo" action="http://localhost:8080/api/file/upload"
                            :on-success="handleVideoSuccess" :before-upload="beforeVideoUpload" :show-file-list="false"
                            :disabled="userInfo.role !== 'admin' && form.id">
                            <el-button type="primary" plain round v-if="!form.videoUrl">上传视频</el-button>
                            <div v-else style="display: flex; align-items: center; gap: 10px;">
                                <el-tag type="success" effect="light" round>视频已就绪</el-tag>
                                <el-button link type="danger" @click.stop="form.videoUrl = ''"
                                    v-if="userInfo.role === 'admin'">移除</el-button>
                            </div>
                        </el-upload>
                        <video v-if="form.videoUrl" :src="form.videoUrl" controls
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
                            <el-avatar :size="36"
                                :src="item.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
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
import { Plus, Search, Download, Star, StarFilled, ChatLineRound, VideoPlay, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import type { UploadProps } from 'element-plus'
import * as echarts from 'echarts'
import { Quill } from '@vueup/vue-quill'

// --- 状态定义 ---
const userInfo = ref<any>({})
const tableData = ref<any[]>([])
const total = ref(0)
const pageSize = ref(12)
const currentPage = ref(1)
const loading = ref(false)
const dialogVisible = ref(false)
const searchName = ref('')
const searchLevel = ref('')
const btnLoading = ref(false)
const multipleSelection = ref<number[]>([])

const isFavorited = ref(false)
const commentList = ref<any[]>([])
const newComment = ref('')

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
    ws = new WebSocket('ws://localhost:8080/ws/project')

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
        const res = await axios.get('http://localhost:8080/api/projects/page', {
            params: {
                pageNum: currentPage.value,
                pageSize: pageSize.value,
                name: searchName.value,
                protectLevel: searchLevel.value
            }
        })
        if (res.data.code === 200) {
            tableData.value = res.data.data.records.map((item: any) => ({
                ...item, selected: false
            }))
            total.value = res.data.data.total
            multipleSelection.value = []
        }
    } catch (error) {
        ElMessage.error('服务连接异常')
    } finally {
        loading.value = false
    }
}

// 互动功能
const checkFavoriteStatus = async (projectId: number) => {
    if (!userInfo.value.id) return
    const res = await axios.get('http://localhost:8080/api/favorites/check', { params: { userId: userInfo.value.id, projectId } })
    isFavorited.value = res.data.data
}

const toggleFavorite = async () => {
    const res = await axios.post('http://localhost:8080/api/favorites/toggle', { userId: userInfo.value.id, projectId: form.id })
    isFavorited.value = res.data.data
    isFavorited.value ? ElMessage.success('已加入您的雅集收藏') : ElMessage.info('已移出收藏')
}

const fetchComments = async (projectId: number) => {
    const res = await axios.get('http://localhost:8080/api/comments/list', { params: { projectId } })
    commentList.value = res.data.data
}

const submitComment = async () => {
    if (!newComment.value.trim()) return ElMessage.warning('内容不能为空')
    await axios.post('http://localhost:8080/api/comments/add', { projectId: form.id, userId: userInfo.value.id, content: newComment.value })
    ElMessage.success('留言成功')
    newComment.value = ''
    fetchComments(form.id!)
}

const handleDeleteComment = (commentId: number) => {
    ElMessageBox.confirm('是否撤回该条留言？', '确认操作', { type: 'warning' }).then(async () => {
        const res = await axios.delete(`http://localhost:8080/api/comments/delete/${commentId}`)
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
    if (row.id) { checkFavoriteStatus(row.id); fetchComments(row.id); }
}

const saveProject = async () => {
    if (!form.name) return ElMessage.warning('项目名称为必填项')
    btnLoading.value = true
    try {
        const req = form.id ? axios.put : axios.post
        const url = `http://localhost:8080/api/projects/${form.id ? 'update' : 'add'}`
        const res = await req(url, form)
        if (res.data.code === 200) {
            ElMessage.success('信息已存档')
            dialogVisible.value = false;
            fetchData();
            initCharts();
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
            const res = await axios.delete('http://localhost:8080/api/projects/delete/batch', { data: multipleSelection.value })
            if (res.data.code === 200) { ElMessage.success('档案已清理'); fetchData(); initCharts() }
        })
}

const handleDelete = (id: number) => {
    ElMessageBox.confirm('此操作不可逆，确认移除该项目吗？', '警告', { type: 'error' })
        .then(async () => {
            const res = await axios.delete(`http://localhost:8080/api/projects/delete/${id}`)
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
        const res = await axios.post('http://localhost:8080/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
        if (res.data.code === 200) {
            const quill = quillRef.value.getQuill(); const range = quill.getSelection(true)
            quill.insertEmbed(range.index, 'image', res.data.data); quill.setSelection(range.index + 1)
        }
    } finally { e.target.value = '' }
}

// 其他工具方法
const handleSearch = () => { currentPage.value = 1; fetchData() }
const handlePageChange = (val: number) => { currentPage.value = val; fetchData() }
const getLevelClass = (level: string) => {
    const map: any = { '国家级': 'level-national', '省级': 'level-provincial', '市级': 'level-city', '县级': 'level-county' }
    return map[level] || 'level-county'
}
const handleExport = () => { window.open('http://localhost:8080/api/projects/export') }

// --- 高级质感 ECharts 初始化 ---
const initCharts = async () => {
    await nextTick()

    // 雅致国风色彩体系
    const heritageColors = ['#c41e3a', '#e6a23c', '#4A90E2', '#2c3e50', '#8fb2c9']

    const pieChartDom = document.getElementById('pieChart')
    if (pieChartDom) {
        echarts.getInstanceByDom(pieChartDom)?.dispose()
        const myPieChart = echarts.init(pieChartDom)
        try {
            const res = await axios.get('http://localhost:8080/api/statistics/level')
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
        echarts.getInstanceByDom(barChartDom)?.dispose()
        const myBarChart = echarts.init(barChartDom)
        try {
            const res = await axios.get('http://localhost:8080/api/statistics/status')
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
                            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
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
window.addEventListener('resize', () => {
    echarts.getInstanceByDom(document.getElementById('pieChart') as HTMLElement)?.resize()
    echarts.getInstanceByDom(document.getElementById('barChart') as HTMLElement)?.resize()
})

// ================== 生命周期钩子 ==================
onMounted(() => {
    const userStr = localStorage.getItem('user')
    if (userStr) userInfo.value = JSON.parse(userStr)

    // 获取初始数据
    fetchData()
    initCharts()

    // ✨ 开启 WebSocket 监听，戴上“接收器”！
    initWebSocket()
})

onUnmounted(() => {
    // ✨ 离开页面时安全关闭连接，防止内存泄漏
    if (ws) {
        ws.close()
    }
})

// ✨ 新增：非遗类别 ID 到名称的转换映射
const getCategoryName = (id: number) => {
    const categoryMap: Record<number, string> = {
        1: '民间文学',
        2: '传统技艺',
        3: '传统舞蹈',
        4: '传统戏剧',
        5: '曲艺',
        6: '传统体育、游艺与杂技',
        7: '传统美术',
        8: '传统医药',
        9: '民俗',
        10: '传统音乐'
    }
    // 如果匹配不到，默认显示“其他类别”
    return categoryMap[id] || '其他类别'
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

.admin-actions {
    display: flex;
    gap: 12px;
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