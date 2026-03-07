<template>
  <div class="home-page">
    <div class="dashboard-grid">
      <el-card shadow="hover"><template #header><div class="panel-header">保护级别分布</div></template><div id="pieChart" class="chart-box"></div></el-card>
      <el-card shadow="hover"><template #header><div class="panel-header">项目状态分布</div></template><div id="barChart" class="chart-box"></div></el-card>
    </div>
    <el-card shadow="hover" class="map-card"><template #header><div class="panel-header">全国项目地域分布</div></template><div id="chinaMap" class="map-box" v-loading="mapLoading"></div></el-card>
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input v-model="searchName" placeholder="搜索项目名称" clearable :prefix-icon="Search" @keyup.enter="handleSearch" @clear="fetchData" />
          <el-select v-model="searchLevel" clearable placeholder="保护级别" @change="fetchData"><el-option v-for="item in protectLevelOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-select v-if="isAdmin" v-model="searchAuditStatus" clearable placeholder="审核状态" @change="fetchData"><el-option v-for="item in auditStatusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="success" @click="openAddDialog"><el-icon><Plus /></el-icon>{{ isAdmin ? '新增项目' : '申请项目' }}</el-button>
          <el-button v-if="isAdmin" type="warning" @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
          <el-button v-if="isAdmin" type="danger" plain @click="goAuditCenter"><el-icon><Document /></el-icon>审核中心<el-badge v-if="pendingCount > 0" :value="pendingCount" class="audit-badge" /></el-button>
        </div>
      </div>
    </el-card>
    <div v-loading="loading">
      <el-empty v-if="tableData.length === 0" description="暂无项目数据" />
      <div v-else class="card-grid">
        <el-card v-for="item in tableData" :key="item.id" shadow="hover" class="project-card" @click="openProjectDialog(item)">
          <template #header><div class="card-header"><div><div class="card-title">{{ item.name }}</div><div class="tag-row"><el-tag size="small">{{ getCategoryName(item.categoryId) }}</el-tag><el-tag size="small" :type="auditTagType(item.auditStatus)">{{ auditLabel(item.auditStatus) }}</el-tag></div></div><el-checkbox v-if="isAdmin" :model-value="selectedIds.includes(item.id)" @click.stop @change="toggleSelection(item.id, $event as boolean)" /></div></template>
          <el-image :src="fileUrl(item.coverUrl)" fit="cover" class="cover-image"><template #error><div class="cover-fallback">暂无封面</div></template></el-image>
          <div class="meta-list">
            <div class="meta-line"><span>保护级别</span><strong>{{ item.protectLevel || '未设置' }}</strong></div>
            <div class="meta-line"><span>项目状态</span><strong>{{ item.status || '未设置' }}</strong></div>
            <div class="meta-line"><span>传承人</span><span class="link-text" @click.stop="goInheritor(item.id)">{{ item.inheritorNames || '暂无' }}</span></div>
            <div class="meta-line"><span>浏览量</span><strong>{{ item.viewCount || 0 }}</strong></div>
          </div>
          <div class="card-actions" @click.stop>
            <el-button v-if="userInfo.id" link :type="favoritedIds.includes(item.id) ? 'warning' : 'primary'" @click="toggleCardFavorite(item.id)"><el-icon><StarFilled v-if="favoritedIds.includes(item.id)" /><Star v-else /></el-icon>{{ favoritedIds.includes(item.id) ? '已收藏' : '收藏' }}</el-button>
            <el-button link type="primary" @click="openProjectDialog(item)">详情</el-button>
            <el-button v-if="isAdmin" link type="danger" @click="handleDelete(item.id)">删除</el-button>
          </div>
        </el-card>
      </div>
      <div class="pagination-wrap"><el-pagination background layout="prev, pager, next, total" :current-page="currentPage" :page-size="pageSize" :total="total" @current-change="handlePageChange" /></div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="920px" destroy-on-close>
      <div class="dialog-grid">
        <div>
          <el-alert v-if="form.auditStatus === 2 && form.auditReason" title="该项目已被驳回" type="error" :closable="false" show-icon><template #default><div>驳回原因：{{ form.auditReason }}</div><div v-if="form.auditBy">审核人：{{ form.auditBy }}</div></template></el-alert>
          <el-form :model="form" label-width="92px" :disabled="isReadonly" class="project-form">
            <el-row :gutter="16"><el-col :span="12"><el-form-item label="项目名称"><el-input v-model="form.name" /></el-form-item></el-col><el-col :span="12"><el-form-item label="保护级别"><el-select v-model="form.protectLevel" style="width:100%"><el-option v-for="item in protectLevelOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col></el-row>
            <el-row :gutter="16"><el-col :span="12"><el-form-item label="项目分类"><el-select v-model="form.categoryId" filterable style="width:100%"><el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="地区 ID"><el-input-number v-model="form.regionId" :min="1" style="width:100%" /></el-form-item></el-col></el-row>
            <el-row :gutter="16"><el-col :span="12"><el-form-item label="项目状态"><el-select v-model="form.status" style="width:100%"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="传承人"><el-input :model-value="form.inheritorNames || '暂无'" readonly /></el-form-item></el-col></el-row>
            <el-form-item label="项目特征"><el-input v-model="form.features" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="历史介绍"><el-input v-model="form.history" type="textarea" :rows="6" /></el-form-item>
            <el-row :gutter="16"><el-col :span="12"><el-form-item label="封面图片"><el-upload :action="uploadUrl" :show-file-list="false" :on-success="handleCoverSuccess" :before-upload="beforeCoverUpload" :disabled="isReadonly" class="uploader"><el-image v-if="form.coverUrl" :src="fileUrl(form.coverUrl)" fit="cover" class="upload-preview" /><div v-else class="upload-box"><el-icon><Plus /></el-icon><span>上传封面</span></div></el-upload></el-form-item></el-col><el-col :span="12"><el-form-item label="项目视频"><el-upload :action="uploadUrl" :show-file-list="false" :on-success="handleVideoSuccess" :before-upload="beforeVideoUpload" :disabled="isReadonly" class="uploader"><div class="upload-box small"><el-icon><VideoPlay /></el-icon><span>{{ form.videoUrl ? '重新上传视频' : '上传视频' }}</span></div></el-upload><video v-if="form.videoUrl" :src="fileUrl(form.videoUrl)" controls class="video-preview"></video></el-form-item></el-col></el-row>
          </el-form>
        </div>
        <div>
          <el-card shadow="never"><template #header><div class="side-header"><span>项目详情</span><el-button v-if="form.id && userInfo.id" circle :type="isFavorited ? 'warning' : 'default'" @click="toggleFavorite"><el-icon><StarFilled v-if="isFavorited" /><Star v-else /></el-icon></el-button></div></template><div class="side-list"><div class="meta-line"><span>分类</span><strong>{{ getCategoryName(form.categoryId) }}</strong></div><div class="meta-line"><span>状态</span><strong>{{ form.status || '未设置' }}</strong></div><div class="meta-line"><span>浏览量</span><strong>{{ form.viewCount || 0 }}</strong></div><div class="meta-line"><span>审核状态</span><strong>{{ auditLabel(form.auditStatus) }}</strong></div></div></el-card>
          <el-card v-if="form.id" shadow="never" class="comment-card"><template #header><div class="side-header"><span>评论区</span><span>{{ commentList.length }} 条</span></div></template><div v-if="commentList.length === 0" class="empty-text">暂无评论</div><div v-else class="comment-list"><div v-for="item in commentList" :key="item.id" class="comment-item"><el-avatar :size="34" :src="avatarUrl(item.avatarUrl)" /><div class="comment-body"><div class="comment-meta"><strong>{{ item.nickname || '匿名用户' }}</strong><span>{{ item.createdAt }}</span></div><div>{{ item.content }}</div><div v-if="isAdmin || userInfo.id === item.userId" class="comment-delete"><el-button link type="danger" size="small" @click="handleDeleteComment(item.id)">删除</el-button></div></div></div></div><div v-if="userInfo.id" class="comment-editor"><el-input v-model="newComment" type="textarea" :rows="3" placeholder="输入评论内容" /><el-button type="primary" @click="submitComment">发表评论</el-button></div></el-card>
        </div>
      </div>
      <template #footer><div class="dialog-footer"><div><el-button v-if="isAdmin && form.id && pendingAudit(form.auditStatus)" type="success" plain @click="handleAudit(1)">通过审核</el-button><el-button v-if="isAdmin && form.id && pendingAudit(form.auditStatus)" type="danger" plain @click="handleAudit(2)">驳回申请</el-button></div><div><el-button @click="dialogVisible = false">关闭</el-button><el-button v-if="isAdmin || !form.id" type="primary" :loading="btnLoading" @click="saveProject">保存</el-button></div></div></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Delete, Document, Download, Plus, Search, Star, StarFilled, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import type { UploadProps } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { DEFAULT_AVATAR_URL, toApiUrl, toServerUrl, toWsUrl } from '@/utils/url'

interface ProjectForm { id?: number; name: string; categoryId: number; regionId: number; protectLevel: string; status: string; history: string; features: string; coverUrl: string; videoUrl: string; auditStatus?: number | null; auditReason?: string | null; auditBy?: string | null; inheritorNames?: string; viewCount?: number }
const categoryOptions = [{ id: 1, name: '民间文学' }, { id: 2, name: '传统音乐' }, { id: 3, name: '传统舞蹈' }, { id: 4, name: '传统戏剧' }, { id: 5, name: '曲艺' }, { id: 6, name: '传统体育、游艺与杂技' }, { id: 7, name: '传统美术' }, { id: 8, name: '传统技艺' }, { id: 9, name: '传统医药' }, { id: 10, name: '民俗' }, { id: 11, name: '神话传说' }, { id: 12, name: '史诗' }, { id: 13, name: '民间故事' }, { id: 14, name: '民间歌谣' }, { id: 15, name: '民间谚语' }, { id: 16, name: '民间歌曲' }, { id: 17, name: '传统器乐' }, { id: 18, name: '民间舞乐' }, { id: 19, name: '宗教音乐' }, { id: 20, name: '陶瓷烧制技艺' }, { id: 21, name: '纺织染色技艺' }, { id: 22, name: '刺绣技艺' }, { id: 23, name: '竹木制作技艺' }, { id: 24, name: '金属锻造技艺' }, { id: 25, name: '漆器制作技艺' }, { id: 26, name: '食品制作技艺' }, { id: 27, name: '造纸印刷技艺' }, { id: 28, name: '中医药' }, { id: 29, name: '民族医药' }, { id: 30, name: '传统养生功法' }, { id: 31, name: '传统节日' }, { id: 32, name: '岁时节令' }, { id: 33, name: '传统礼仪' }, { id: 34, name: '民间信仰' }]
const protectLevelOptions = [{ label: '国家级', value: '国家级' }, { label: '省级', value: '省级' }, { label: '市级', value: '市级' }, { label: '县级', value: '县级' }]
const statusOptions = [{ label: '在传承', value: '在传承' }, { label: '濒危', value: '濒危' }]
const auditStatusOptions = [{ label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已驳回', value: 2 }]
const route = useRoute(); const router = useRouter(); const userInfo = ref<any>({}); const tableData = ref<any[]>([]); const total = ref(0); const pageSize = ref(8); const currentPage = ref(1); const loading = ref(false); const mapLoading = ref(true); const dialogVisible = ref(false); const searchName = ref(''); const searchLevel = ref(''); const searchAuditStatus = ref<number | undefined>(undefined); const btnLoading = ref(false); const pendingCount = ref(0); const selectedIds = ref<number[]>([]); const favoritedIds = ref<number[]>([]); const isFavorited = ref(false); const commentList = ref<any[]>([]); const newComment = ref(''); const uploadUrl = toApiUrl('/file/upload')
const emptyForm = (): ProjectForm => ({ name: '', categoryId: 1, regionId: 1, protectLevel: '国家级', status: '在传承', history: '', features: '', coverUrl: '', videoUrl: '', auditStatus: 0, auditReason: '', auditBy: '', inheritorNames: '', viewCount: 0 })
const form = reactive<ProjectForm>(emptyForm()); let ws: WebSocket | null = null
const isAdmin = computed(() => userInfo.value.role === 'admin'); const isReadonly = computed(() => Boolean(form.id) && !isAdmin.value); const dialogTitle = computed(() => !form.id ? (isAdmin.value ? '新增项目' : '申请项目') : (isAdmin.value ? '编辑项目' : '项目详情'))
const fileUrl = (url?: string | null) => url ? toServerUrl(url) : ''; const avatarUrl = (url?: string | null) => url ? toServerUrl(url) : DEFAULT_AVATAR_URL
const getCategoryName = (id?: number | null) => categoryOptions.find((item) => item.id === id)?.name || '未分类'; const auditLabel = (status?: number | null) => status === 1 ? '已通过' : status === 2 ? '已驳回' : '待审核'; const auditTagType = (status?: number | null) => status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'; const pendingAudit = (status?: number | null) => status === 0 || status === null || status === undefined
const loadCurrentUser = () => { const raw = sessionStorage.getItem('user'); userInfo.value = raw ? JSON.parse(raw) : {} }
const buildParams = () => { const params: Record<string, any> = { pageNum: currentPage.value, pageSize: pageSize.value }; if (searchName.value.trim()) params.name = searchName.value.trim(); if (searchLevel.value) params.protectLevel = searchLevel.value; if (isAdmin.value) { if (searchAuditStatus.value !== undefined) params.auditStatus = searchAuditStatus.value } else { params.auditStatus = 1 } return params }
const fetchData = async () => { loading.value = true; try { const res = await request.get('/projects/page', { params: buildParams() }); const page = res.data.data; tableData.value = (page.records || []).map((item: any) => ({ ...item, inheritorNames: item.inheritorNames || '' })); total.value = page.total || 0; selectedIds.value = [] } catch (error) { console.error('Failed to fetch projects:', error); tableData.value = []; total.value = 0; ElMessage.error('项目数据加载失败') } finally { loading.value = false } }
const fetchPendingCount = async () => { if (!isAdmin.value) { pendingCount.value = 0; return } try { const res = await request.get('/projects/page', { params: { pageNum: 1, pageSize: 1, auditStatus: 0 } }); pendingCount.value = res.data.data?.total || 0 } catch { pendingCount.value = 0 } }
const loadFavorited = async () => { if (!userInfo.value.id) { favoritedIds.value = []; return } try { const res = await request.get('/favorites/list', { params: { userId: userInfo.value.id, pageNum: 1, pageSize: 100 } }); favoritedIds.value = (res.data.data.records || []).map((item: any) => item.id) } catch (error) { console.error('Failed to load favorites:', error) } }
const checkFavorite = async (projectId: number) => { if (!userInfo.value.id) { isFavorited.value = false; return } try { const res = await request.get('/favorites/check', { params: { userId: userInfo.value.id, projectId } }); isFavorited.value = Boolean(res.data.data) } catch { isFavorited.value = false } }
const toggleFavorite = async () => { if (!userInfo.value.id || !form.id) { ElMessage.warning('请先登录后再收藏项目'); return } const res = await request.post('/favorites/toggle', { userId: userInfo.value.id, projectId: form.id }); isFavorited.value = Boolean(res.data.data); await loadFavorited(); ElMessage.success(isFavorited.value ? '已加入收藏' : '已取消收藏') }
const toggleCardFavorite = async (projectId: number) => { if (!userInfo.value.id) { ElMessage.warning('请先登录后再收藏项目'); return } await request.post('/favorites/toggle', { userId: userInfo.value.id, projectId }); await loadFavorited() }
const fetchComments = async (projectId: number) => { const res = await request.get('/comments/list', { params: { projectId } }); commentList.value = res.data.data || [] }
const submitComment = async () => { if (!form.id || !userInfo.value.id) { ElMessage.warning('请先登录后再发表评论'); return } if (!newComment.value.trim()) { ElMessage.warning('请输入评论内容'); return } await request.post('/comments/add', { projectId: form.id, userId: userInfo.value.id, content: newComment.value.trim() }); newComment.value = ''; await fetchComments(form.id); ElMessage.success('评论发表成功') }
const handleDeleteComment = async (commentId: number) => { await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', { type: 'warning' }); await request.delete(`/comments/delete/${commentId}`); if (form.id) await fetchComments(form.id); ElMessage.success('评论已删除') }
const increaseViewCount = async (projectId: number) => { try { const res = await request.get('/view/count', { params: { projectId } }); form.viewCount = res.data.data || form.viewCount || 0 } catch (error) { console.error('Failed to increase view count:', error) } }
const resetForm = () => { Object.assign(form, emptyForm()); commentList.value = []; newComment.value = ''; isFavorited.value = false }
const openAddDialog = () => { resetForm(); dialogVisible.value = true }
const openProjectDialog = async (project: any) => { resetForm(); Object.assign(form, emptyForm(), project); dialogVisible.value = true; if (project.id) await Promise.all([checkFavorite(project.id), fetchComments(project.id), increaseViewCount(project.id)]) }
const saveProject = async () => { if (!form.name.trim()) { ElMessage.warning('请输入项目名称'); return } btnLoading.value = true; try { const requestFn = form.id ? request.put : request.post; const url = form.id ? '/projects/update' : '/projects/add'; await requestFn(url, { ...form }); dialogVisible.value = false; await Promise.all([fetchData(), initCharts(), fetchPendingCount()]); ElMessage.success(form.id ? '项目已更新' : isAdmin.value ? '项目已创建' : '项目已提交，等待审核') } catch (error: any) { ElMessage.error(error?.response?.data?.msg || '项目保存失败') } finally { btnLoading.value = false } }
const handleAudit = async (status: number) => { if (!form.id) return; try { let auditReason = ''; if (status === 2) { const promptResult = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', { inputPlaceholder: '例如：资料不完整、封面缺失、内容不准确', inputValidator: (value) => value.trim() ? true : '驳回原因不能为空' }); auditReason = promptResult.value.trim() } await request.put('/projects/audit', null, { params: { id: form.id, auditStatus: status, auditReason } }); dialogVisible.value = false; await Promise.all([fetchData(), fetchPendingCount()]); ElMessage.success(status === 1 ? '项目已审核通过' : '项目已驳回') } catch (error: any) { if (error === 'cancel' || error === 'close' || error?.action === 'cancel' || error?.action === 'close') return; ElMessage.error(error?.response?.data?.msg || '审核操作失败') } }
const toggleSelection = (projectId: number, checked: boolean) => { selectedIds.value = checked ? Array.from(new Set([...selectedIds.value, projectId])) : selectedIds.value.filter((id) => id !== projectId) }
const handleDelete = async (id: number) => { await ElMessageBox.confirm('确认删除这个项目吗？', '删除项目', { type: 'warning' }); await request.delete(`/projects/delete/${id}`); await Promise.all([fetchData(), initCharts(), fetchPendingCount()]); ElMessage.success('项目已删除') }
const handleCoverSuccess: UploadProps['onSuccess'] = (response: any) => { if (response.code === 200) form.coverUrl = response.data }
const beforeCoverUpload: UploadProps['beforeUpload'] = (file) => { if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) { ElMessage.error('封面仅支持 JPG、PNG、WEBP 图片'); return false } return true }
const handleVideoSuccess: UploadProps['onSuccess'] = (response: any) => { if (response.code === 200) form.videoUrl = response.data }
const beforeVideoUpload: UploadProps['beforeUpload'] = (file) => { if (!file.type.startsWith('video/')) { ElMessage.error('请上传视频文件'); return false } return true }
const handleSearch = async () => { currentPage.value = 1; await fetchData() }
const handlePageChange = (page: number) => { currentPage.value = page; fetchData() }
const goInheritor = (projectId: number) => { router.push({ path: '/inheritor', query: { projectId } }) }
const handleExport = () => window.open(toApiUrl('/projects/export'), '_blank')
const goAuditCenter = () => router.push('/audit-center')
const maybeOpenProjectFromRoute = async () => { const routeId = Number(route.query.id); if (!routeId) return; const matched = tableData.value.find((item) => item.id === routeId); if (matched) await openProjectDialog(matched) }
const initWebSocket = () => { try { ws = new WebSocket(toWsUrl('/ws/project')); ws.onmessage = async (event) => { if (!String(event.data).includes('PROJECT_DATA_CHANGED')) return; ElNotification({ title: '项目数据已更新', message: '检测到项目数据变更，页面内容已自动刷新。', type: 'success' }); await Promise.all([fetchData(), initCharts(), fetchPendingCount()]) } } catch (error) { console.error('WebSocket init failed:', error) } }
const resizeCharts = () => { for (const id of ['pieChart', 'barChart', 'chinaMap']) { const dom = document.getElementById(id); if (dom) echarts.getInstanceByDom(dom)?.resize() } }
const initCharts = async () => { await nextTick(); mapLoading.value = true; const pieDom = document.getElementById('pieChart'); const barDom = document.getElementById('barChart'); const mapDom = document.getElementById('chinaMap'); if (!pieDom || !barDom || !mapDom) { mapLoading.value = false; return } echarts.getInstanceByDom(pieDom)?.dispose(); echarts.getInstanceByDom(barDom)?.dispose(); echarts.getInstanceByDom(mapDom)?.dispose(); const pieChart = echarts.init(pieDom); const barChart = echarts.init(barDom); const mapChart = echarts.init(mapDom); try { const [levelRes, statusRes, mapRes] = await Promise.all([request.get('/statistics/level'), request.get('/statistics/status'), request.get('/statistics/map')]); const levelData = levelRes.data.data || []; const statusData = statusRes.data.data || []; const mapData = mapRes.data.data || []; pieChart.setOption({ color: ['#c41e3a', '#e6a23c', '#409eff', '#67c23a'], tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['45%', '70%'], data: levelData }] }); barChart.setOption({ tooltip: { trigger: 'axis' }, grid: { left: 40, right: 20, top: 20, bottom: 30 }, xAxis: { type: 'category', data: statusData.map((item: any) => item.name) }, yAxis: { type: 'value', minInterval: 1 }, series: [{ type: 'bar', data: statusData.map((item: any) => item.value), barWidth: '40%', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#e74c3c' }, { offset: 1, color: '#c41e3a' }]), borderRadius: [6, 6, 0, 0] } }] }); const geoResponse = await fetch('/china.json'); const geoJson = await geoResponse.json(); echarts.registerMap('china', geoJson as any); const normalizedMapData = mapData.map((item: any) => { const feature = geoJson.features?.find((candidate: any) => { const featureName = candidate.properties?.name || ''; return featureName.includes(item.name) || item.name?.includes(featureName) }); return { ...item, name: feature?.properties?.name || item.name } }); const maxValue = normalizedMapData.length > 0 ? Math.max(...normalizedMapData.map((item: any) => Number(item.value) || 0)) : 10; mapChart.setOption({ tooltip: { trigger: 'item', formatter: (params: any) => `${params.name}<br/>项目数：${params.value || 0}` }, visualMap: { min: 0, max: maxValue, left: 20, bottom: 20, calculable: true, inRange: { color: ['#f7e3c3', '#e6a23c', '#c41e3a'] } }, series: [{ type: 'map', map: 'china', roam: true, zoom: 1.15, data: normalizedMapData, emphasis: { label: { show: true }, itemStyle: { areaColor: '#f1c40f' } }, itemStyle: { areaColor: '#f5f7fa', borderColor: '#fff', borderWidth: 1 } }] }) } catch (error) { console.error('Failed to init charts:', error) } finally { mapLoading.value = false } }
onMounted(async () => { loadCurrentUser(); await fetchData(); await initCharts(); await fetchPendingCount(); if (userInfo.value.id) await loadFavorited(); initWebSocket(); await maybeOpenProjectFromRoute(); window.addEventListener('resize', resizeCharts) })
onUnmounted(() => { if (ws) ws.close(); window.removeEventListener('resize', resizeCharts) })
</script>
<style scoped>
.home-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 64px); }
.dashboard-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; margin-bottom: 20px; }
.panel-header { font-weight: 700; color: #303133; }
.chart-box { height: 280px; }
.map-card, .toolbar-card { margin-bottom: 20px; }
.map-box { height: 460px; }
.toolbar-row, .toolbar-left, .toolbar-right { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.toolbar-row { justify-content: space-between; }
.toolbar-left :deep(.el-input) { width: 260px; }
.audit-badge { margin-left: 8px; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 18px; }
.project-card { border-radius: 16px; cursor: pointer; }
.card-header { display: flex; justify-content: space-between; gap: 12px; }
.card-title { font-size: 18px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.tag-row { display: flex; gap: 8px; flex-wrap: wrap; }
.cover-image, .upload-preview { width: 100%; height: 180px; border-radius: 14px; overflow: hidden; }
.cover-fallback, .upload-box { width: 100%; height: 180px; border: 1px dashed #dcdfe6; border-radius: 14px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #909399; background: #fafafa; }
.upload-box.small { height: 120px; }
.meta-list { margin-top: 14px; display: flex; flex-direction: column; gap: 10px; }
.meta-line { display: flex; justify-content: space-between; gap: 10px; color: #606266; }
.meta-line span:first-child { color: #909399; }
.link-text { color: #409eff; }
.card-actions, .dialog-footer, .side-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }
.dialog-grid { display: grid; grid-template-columns: minmax(0, 2fr) 320px; gap: 18px; }
.project-form { margin-top: 16px; }
.uploader { width: 100%; }
.uploader :deep(.el-upload) { width: 100%; }
.video-preview { width: 100%; margin-top: 12px; border-radius: 12px; background: #000; }
.side-list { display: flex; flex-direction: column; gap: 12px; }
.comment-card { margin-top: 16px; }
.empty-text { color: #909399; }
.comment-list { display: flex; flex-direction: column; gap: 16px; max-height: 320px; overflow: auto; }
.comment-item { display: flex; gap: 12px; }
.comment-body { flex: 1; min-width: 0; }
.comment-meta { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 6px; color: #909399; font-size: 13px; }
.comment-delete, .comment-editor { margin-top: 8px; }
.comment-editor { display: flex; flex-direction: column; gap: 12px; }
@media (max-width: 1100px) { .dashboard-grid, .dialog-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .home-page { padding: 16px; } .toolbar-row { flex-direction: column; align-items: stretch; } .toolbar-left, .toolbar-right { width: 100%; } .toolbar-left :deep(.el-input) { width: 100%; } }
</style>
