<template>
  <div class="audit-center">
    <div class="audit-header">
      <div class="audit-title-area">
        <el-icon class="audit-icon"><Document /></el-icon>
        <div>
          <h2 class="audit-title">非遗档案审核台</h2>
          <p class="audit-subtitle">集中处理项目申报、审核结果与驳回原因，保持数据质量和流程清晰。</p>
        </div>
      </div>
      <div class="audit-stats">
        <div class="stat-card stat-pending">
          <div class="stat-num">{{ stats.pending }}</div>
          <div class="stat-label">待审核</div>
        </div>
        <div class="stat-card stat-pass">
          <div class="stat-num">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
        <div class="stat-card stat-reject">
          <div class="stat-num">{{ stats.rejected }}</div>
          <div class="stat-label">已驳回</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="audit-tabs" @tab-change="handleTabChange">
      <el-tab-pane name="0">
        <template #label>
          <span class="tab-label">
            <el-icon><Clock /></el-icon>
            待审核
            <el-badge v-if="stats.pending > 0" :value="stats.pending" class="tab-badge" type="warning" />
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="1">
        <template #label>
          <span class="tab-label">
            <el-icon><CircleCheck /></el-icon>
            已通过
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="2">
        <template #label>
          <span class="tab-label">
            <el-icon><CircleClose /></el-icon>
            已驳回
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="filter-bar">
      <el-input
        v-model="searchName"
        placeholder="搜索项目名称"
        :prefix-icon="Search"
        clearable
        @clear="fetchList"
        @keyup.enter="fetchList"
        style="width: 260px"
      />
      <el-select
        v-model="searchLevel"
        placeholder="保护级别"
        clearable
        @change="fetchList"
        style="width: 140px; margin-left: 12px"
      >
        <el-option label="国家级" value="国家级" />
        <el-option label="省级" value="省级" />
        <el-option label="市级" value="市级" />
        <el-option label="县级" value="县级" />
      </el-select>
      <el-button type="primary" plain :icon="Refresh" @click="fetchList" style="margin-left: 12px">刷新</el-button>
      <template v-if="activeTab === '0' && selectedIds.length > 0">
        <el-button type="success" @click="batchApprove" style="margin-left: 12px">
          <el-icon><Select /></el-icon>
          批量通过 ({{ selectedIds.length }})
        </el-button>
      </template>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      @selection-change="handleSelectionChange"
      :row-class-name="getRowClass"
      class="audit-table"
    >
      <el-table-column v-if="activeTab === '0'" type="selection" width="50" />
      <el-table-column label="项目名称" min-width="200">
        <template #default="{ row }">
          <div class="project-name-cell">
            <el-image v-if="row.coverUrl" :src="getFileUrl(row.coverUrl)" class="mini-cover" fit="cover" />
            <div v-else class="mini-cover mini-placeholder">
              <el-icon><Picture /></el-icon>
            </div>
            <div>
              <div class="project-name">{{ row.name }}</div>
              <div class="project-meta" v-if="row.submitterName">申报人：{{ row.submitterName }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类别" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info" effect="plain">{{ getCategoryName(row.categoryId) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="保护级别" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="getLevelType(row.protectLevel)" effect="dark">{{ row.protectLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === '在传承' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="110">
        <template #default="{ row }">
          <div class="status-badge" :class="getStatusClass(row.auditStatus)">
            <el-icon class="status-icon"><component :is="getStatusIcon(row.auditStatus)" /></el-icon>
            {{ getStatusLabel(row.auditStatus) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申报时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab === '2'" label="驳回原因" min-width="180">
        <template #default="{ row }">
          <el-tooltip v-if="row.auditReason" :content="row.auditReason" placement="top">
            <div class="reject-reason">{{ row.auditReason }}</div>
          </el-tooltip>
          <span v-else class="text-muted">无</span>
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab !== '0'" label="审核人" width="120">
        <template #default="{ row }">
          <span class="text-muted">{{ row.auditBy || '未记录' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button size="small" type="primary" text @click="viewDetail(row)">查看</el-button>
            <template v-if="activeTab === '0'">
              <el-button size="small" type="success" text @click="handleApprove(row)">通过</el-button>
              <el-button size="small" type="danger" text @click="openRejectDialog(row)">驳回</el-button>
            </template>
            <template v-if="activeTab === '2'">
              <el-button size="small" type="warning" text @click="handleApprove(row)">重新通过</el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchList"
        @size-change="fetchList"
        background
      />
    </div>

    <el-dialog v-model="rejectDialogVisible" title="填写驳回原因" width="480px" align-center>
      <div class="project-reject-info">
        <el-icon color="#F56C6C" style="font-size: 20px"><WarningFilled /></el-icon>
        <span>即将驳回项目：<strong>{{ currentProject?.name }}</strong></span>
      </div>
      <el-form class="reject-form">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请明确说明驳回原因，内容会同步给申报人。"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="auditLoading" :disabled="!rejectReason.trim()" @click="confirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" :title="currentProject?.name || '项目详情'" size="520px">
      <div class="detail-content" v-if="currentProject">
        <el-image v-if="currentProject.coverUrl" :src="getFileUrl(currentProject.coverUrl)" class="detail-cover" fit="cover" />

        <div class="detail-audit-status">
          <div class="status-badge" :class="getStatusClass(currentProject.auditStatus)" style="padding: 8px 16px; font-size: 14px">
            <el-icon><component :is="getStatusIcon(currentProject.auditStatus)" /></el-icon>
            {{ getStatusLabel(currentProject.auditStatus) }}
          </div>
          <div v-if="currentProject.auditStatus === 2 && currentProject.auditReason" class="reject-banner">
            <el-icon><WarningFilled /></el-icon>
            <span>驳回原因：{{ currentProject.auditReason }}</span>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="项目名称">{{ currentProject.name }}</el-descriptions-item>
          <el-descriptions-item label="保护级别">{{ currentProject.protectLevel }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentProject.status }}</el-descriptions-item>
          <el-descriptions-item label="所属类别">{{ getCategoryName(currentProject.categoryId) }}</el-descriptions-item>
          <el-descriptions-item label="申报时间" :span="2">{{ formatTime(currentProject.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-history" v-if="currentProject.history">
          <div class="detail-section-title">项目简介</div>
          <div class="detail-text" v-html="currentProject.history"></div>
        </div>

        <div class="detail-actions" v-if="currentProject.auditStatus === 0 || currentProject.auditStatus === null">
          <el-button type="success" @click="handleApprove(currentProject); detailDrawerVisible = false">
            <el-icon><CircleCheck /></el-icon>
            通过审核
          </el-button>
          <el-button type="danger" @click="openRejectDialog(currentProject); detailDrawerVisible = false">
            <el-icon><CircleClose /></el-icon>
            驳回申请
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  CircleCheck,
  CircleClose,
  Clock,
  Document,
  Picture,
  Refresh,
  Search,
  Select,
  WarningFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { toServerUrl } from '@/utils/url'

const activeTab = ref('0')
const tableData = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')
const searchLevel = ref('')
const selectedIds = ref<number[]>([])
const rejectDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const currentProject = ref<any>(null)
const rejectReason = ref('')
const auditLoading = ref(false)

const stats = reactive({ pending: 0, approved: 0, rejected: 0 })

const CATEGORIES: Record<number, string> = {
  1: '民间文学',
  2: '传统音乐',
  3: '传统舞蹈',
  4: '传统戏剧',
  5: '曲艺',
  6: '传统体育、游艺与杂技',
  7: '传统美术',
  8: '传统技艺',
  9: '传统医药',
  10: '民俗',
}

const getCategoryName = (id: number) => CATEGORIES[id] || '其他'

const getLevelType = (level: string) => {
  const map: Record<string, string> = {
    国家级: '',
    省级: 'success',
    市级: 'warning',
    县级: 'info',
  }
  return map[level] || 'info'
}

const getStatusClass = (status: number | null) => {
  if (status === 1) return 'status-approved'
  if (status === 2) return 'status-rejected'
  return 'status-pending'
}

const getStatusIcon = (status: number | null) => {
  if (status === 1) return CircleCheck
  if (status === 2) return CircleClose
  return Clock
}

const getStatusLabel = (status: number | null) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

const getRowClass = ({ row }: any) => {
  if (row.auditStatus === 0 || row.auditStatus === null) return 'row-pending'
  return ''
}

const getFileUrl = (url?: string) => {
  return toServerUrl(url)
}

const formatTime = (value: string) => {
  if (!value) return '未记录'
  return value.replace('T', ' ').substring(0, 16)
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/projects/page', {
      params: {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        name: searchName.value || undefined,
        protectLevel: searchLevel.value || undefined,
        auditStatus: Number(activeTab.value),
      },
    })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
      request.get('/projects/page', { params: { pageNum: 1, pageSize: 1, auditStatus: 0 } }),
      request.get('/projects/page', { params: { pageNum: 1, pageSize: 1, auditStatus: 1 } }),
      request.get('/projects/page', { params: { pageNum: 1, pageSize: 1, auditStatus: 2 } }),
    ])
    stats.pending = pendingRes.data.data?.total || 0
    stats.approved = approvedRes.data.data?.total || 0
    stats.rejected = rejectedRes.data.data?.total || 0
  } catch {
    // ignore stats refresh failure
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  selectedIds.value = []
  fetchList()
}

const handleSelectionChange = (rows: any[]) => {
  selectedIds.value = rows.map(row => row.id)
}

const handleApprove = async (row: any) => {
  try {
    const res = await request.put('/projects/audit', null, {
      params: { id: row.id, auditStatus: 1 },
    })
    if (res.data.code === 200) {
      ElMessage.success(`《${row.name}》已通过审核`)
      fetchList()
      fetchStats()
    } else {
      ElMessage.error(res.data.msg || '审核失败')
    }
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}

const openRejectDialog = (row: any) => {
  currentProject.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  auditLoading.value = true
  try {
    const res = await request.put('/projects/audit', null, {
      params: {
        id: currentProject.value.id,
        auditStatus: 2,
        auditReason: rejectReason.value.trim(),
      },
    })
    if (res.data.code === 200) {
      ElMessage.success(`《${currentProject.value.name}》已驳回`)
      rejectDialogVisible.value = false
      fetchList()
      fetchStats()
    } else {
      ElMessage.error(res.data.msg || '驳回失败')
    }
  } finally {
    auditLoading.value = false
  }
}

const batchApprove = async () => {
  await ElMessageBox.confirm(`确认批量通过 ${selectedIds.value.length} 个项目？`, '批量审核确认', {
    type: 'success',
    confirmButtonText: '全部通过',
    cancelButtonText: '取消',
  })

  let successCount = 0
  for (const id of selectedIds.value) {
    try {
      const res = await request.put('/projects/audit', null, { params: { id, auditStatus: 1 } })
      if (res.data.code === 200) successCount += 1
    } catch {
      // continue remaining items
    }
  }

  ElMessage.success(`成功通过 ${successCount} 个项目`)
  selectedIds.value = []
  fetchList()
  fetchStats()
}

const viewDetail = (row: any) => {
  currentProject.value = row
  detailDrawerVisible.value = true
}

onMounted(() => {
  fetchList()
  fetchStats()
})
</script>

<style scoped>
.audit-center {
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}

.audit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 24px 28px;
  border-radius: 16px;
  color: #fff;
}

.audit-title-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

.audit-icon {
  font-size: 40px;
  color: #c41e3a;
}

.audit-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(90deg, #fff, #e8c4a0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.audit-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.audit-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  text-align: center;
  padding: 12px 24px;
  border-radius: 12px;
  min-width: 90px;
}

.stat-pending {
  background: rgba(230, 162, 60, 0.2);
  border: 1px solid rgba(230, 162, 60, 0.4);
}

.stat-pass {
  background: rgba(103, 194, 58, 0.2);
  border: 1px solid rgba(103, 194, 58, 0.4);
}

.stat-reject {
  background: rgba(245, 108, 108, 0.2);
  border: 1px solid rgba(245, 108, 108, 0.4);
}

.stat-num {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.stat-pending .stat-num {
  color: #e6a23c;
}

.stat-pass .stat-num {
  color: #67c23a;
}

.stat-reject .stat-num {
  color: #f56c6c;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4px;
}

.audit-tabs {
  margin-bottom: 0;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.tab-badge {
  margin-left: 4px;
}

.filter-bar {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #e5e6eb;
  margin-bottom: 16px;
}

.audit-table {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.row-pending) {
  border-left: 4px solid #e6a23c;
}

.project-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-cover {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  flex-shrink: 0;
  overflow: hidden;
}

.mini-placeholder {
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 18px;
}

.project-name {
  font-weight: 600;
  font-size: 13px;
}

.project-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.status-approved {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #c2e7b0;
}

.status-rejected {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.reject-reason {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #f56c6c;
  font-size: 12px;
}

.text-muted {
  color: #c0c4cc;
  font-size: 12px;
}

.action-btns {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

.project-reject-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fef0f0;
  border-radius: 8px;
  margin-bottom: 16px;
  color: #f56c6c;
  font-size: 14px;
}

.reject-form {
  margin-top: 8px;
}

.detail-cover {
  width: 100%;
  height: 200px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.detail-audit-status {
  margin-bottom: 16px;
}

.reject-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 8px;
  padding: 12px;
  margin-top: 10px;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.6;
}

.detail-desc {
  margin-top: 16px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 8px;
  padding-left: 8px;
  border-left: 3px solid #c41e3a;
}

.detail-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
  max-height: 200px;
  overflow-y: auto;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>
