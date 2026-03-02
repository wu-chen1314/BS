<template>
    <div class="inheritor-container">
        <div class="header-actions">
            <div class="search-box">
                <el-input v-model="searchName" placeholder="搜索传承人姓名..." style="width: 200px; margin-right: 10px;"
                    clearable @clear="fetchData" @keyup.enter="fetchData" />
                <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
            </div>
            <div class="action-buttons">
                <el-button type="primary" :icon="Plus" @click="openAddDialog">新增传承人</el-button>
                <el-button type="success" :icon="Star" @click="openAutoAddDialog">自动添加</el-button>
                <el-button type="warning" :icon="Star" @click="handleBatchAutoAdd">批量自动添加</el-button>
                <el-button type="info" :icon="Search" @click="openPreviewDialog">查询预览</el-button>
            </div>
        </div>

        <div class="batch-actions" v-if="multipleSelection.length > 0">
            <span class="batch-selection-info">已选择 {{ multipleSelection.length }} 项</span>
            <el-button type="danger" :icon="Delete" @click="handleBatchDelete" size="small">
                批量删除
            </el-button>
        </div>

        <el-table :data="tableData" style="width: 100%; margin-top: 20px;" v-loading="loading" border stripe
            @selection-change="handleSelectionChange">

            <el-table-column type="selection" width="55" align="center" />

            <el-table-column label="头像" width="100" align="center">
                <template #default="scope">
                    <el-avatar shape="square" :size="50"
                        :src="scope.row.avatarUrl || 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'" />
                </template>
            </el-table-column>

            <el-table-column prop="name" label="姓名" width="120" align="center" />

            <el-table-column prop="sex" label="性别" width="80" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.sex === '男' ? '' : 'danger'">{{ scope.row.sex }}</el-tag>
                </template>
            </el-table-column>

            <el-table-column prop="level" label="传承级别" width="180" align="center">
                <template #default="scope">
                    <el-tag effect="plain" type="success">{{ scope.row.level }}</el-tag>
                </template>
            </el-table-column>

            <el-table-column prop="projectName" label="所属非遗项目" width="180" align="center">
                <template #default="scope">
                    <el-tag type="info">{{ scope.row.projectName || '未关联' }}</el-tag>
                </template>
            </el-table-column>

            <el-table-column prop="description" label="简介" show-overflow-tooltip />

            <el-table-column label="操作" width="180" align="center" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination-container">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 20]"
                layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="fetchData"
                @current-change="fetchData" />
        </div>

        <el-dialog v-model="dialogVisible" :title="form.id ? '编辑传承人' : '新增传承人'" width="550px"
            :close-on-click-modal="false">
            <el-form :model="form" label-width="100px" style="padding-right: 20px;">

                <el-form-item label="大师照片" prop="avatarUrl">
                    <el-upload class="avatar-uploader" action="http://localhost:8080/api/file/upload"
                        :show-file-list="false" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload">
                        <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar" />
                        <el-icon v-else class="avatar-uploader-icon">
                            <Plus />
                        </el-icon>
                    </el-upload>
                    <div style="font-size: 12px; color: #999; line-height: 1.5; margin-left: 10px;">
                        建议上传比例 1:1 的正方形图片
                    </div>
                </el-form-item>

                <el-form-item label="姓名" required>
                    <el-input v-model="form.name" placeholder="请输入姓名" />
                </el-form-item>

                <el-form-item label="性别">
                    <el-radio-group v-model="form.sex">
                        <el-radio label="男">男</el-radio>
                        <el-radio label="女">女</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item label="年龄">
                    <el-input-number v-model="form.age" :min="1" :max="120" />
                </el-form-item>

                <el-form-item label="传承级别" required>
                    <el-select v-model="form.level" placeholder="请选择级别" style="width: 100%;">
                        <el-option label="国家级非遗代表性传承人" value="国家级非遗代表性传承人" />
                        <el-option label="省级非遗代表性传承人" value="省级非遗代表性传承人" />
                        <el-option label="市级非遗代表性传承人" value="市级非遗代表性传承人" />
                        <el-option label="县级非遗代表性传承人" value="县级非遗代表性传承人" />
                    </el-select>
                </el-form-item>

                <el-form-item label="所属项目" required>
                    <el-select v-model="form.projectId" placeholder="请选择所属非遗项目" style="width: 100%;" filterable>
                        <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                </el-form-item>

                <el-form-item label="简介">
                    <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入大师生平简介..." />
                </el-form-item>
            </el-form>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="saveInheritor">保存</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 自动添加传承人弹窗 -->
        <el-dialog v-model="autoAddDialogVisible" title="自动添加传承人" width="500px" :close-on-click-modal="false">
            <el-form :model="autoAddForm" label-width="120px">
                <el-form-item label="选择非遗项目" required>
                    <el-select v-model="autoAddForm.projectId" placeholder="请选择非遗项目" style="width: 100%;" filterable>
                        <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="项目名称预览" v-if="autoAddForm.projectId">
                    <el-tag type="info">{{ getProjectNameById(autoAddForm.projectId) }}</el-tag>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="autoAddDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleAutoAdd" :loading="autoAddLoading">
                        {{ autoAddLoading ? '添加中...' : '开始添加' }}
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 查询传承人预览弹窗 -->
        <el-dialog v-model="previewDialogVisible" title="传承人预览" width="800px" :close-on-click-modal="false">
            <div class="preview-container">
                <el-form :model="previewForm" label-width="100px" style="margin-bottom: 20px;">
                    <el-form-item label="非遗项目名称">
                        <el-input v-model="previewForm.projectName" placeholder="请输入非遗项目名称" style="width: 300px;">
                            <template #append>
                                <el-button :icon="Search" @click="handlePreviewQuery">查询</el-button>
                            </template>
                        </el-input>
                    </el-form-item>
                </el-form>

                <div v-if="previewData.length > 0">
                    <el-alert title="以下为查询到的传承人信息（预览模式，不会保存到数据库）" type="info" show-icon style="margin-bottom: 20px;" />
                    <el-table :data="previewData" border stripe>
                        <el-table-column prop="name" label="姓名" width="120" align="center" />
                        <el-table-column prop="sex" label="性别" width="80" align="center" />
                        <el-table-column prop="level" label="传承级别" width="180" align="center" />
                        <el-table-column prop="description" label="简介" show-overflow-tooltip />
                    </el-table>
                </div>
                <div v-else-if="previewLoading" class="preview-loading">
                    <el-icon class="is-loading">
                        <Loading />
                    </el-icon>
                    <span style="margin-left: 10px;">正在查询传承人数据...</span>
                </div>
                <div v-else class="preview-empty">
                    <el-empty description="请输入非遗项目名称查询传承人信息" />
                </div>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="previewDialogVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Plus, Edit, Delete, Star, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import axios from 'axios'
import request from '@/utils/request'

// 路由参数
const route = useRoute()
const currentProjectId = ref<number | undefined>(undefined)

// --- 状态定义 ---
const tableData = ref([])
const projectList = ref<any[]>([]) // 存储所有的非遗项目，用于下拉框
const loading = ref(false)
const searchName = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)

// 自动添加相关状态
const autoAddDialogVisible = ref(false)
const autoAddLoading = ref(false)
const autoAddForm = reactive({
    projectId: undefined
})

// 批量选择相关状态
const multipleSelection = ref<any[]>([])

// 预览查询相关状态
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewForm = reactive({
    projectName: ''
})
const previewData = ref<any[]>([])

// 表单数据
const form = reactive({
    id: undefined,
    name: '',
    sex: '男', // 默认选中男
    age: 50,
    avatarUrl: '', // 头像链接
    level: '',
    description: '',
    projectId: undefined
})

// --- 初始化 ---
onMounted(async () => {
    // 从路由参数中获取初始的projectId
    if (route.query.projectId) {
        currentProjectId.value = parseInt(route.query.projectId as string)
    }
    await fetchProjects() // 加载项目下拉框数据
    fetchData() // 加载传承人数据
})

// 监听路由参数变化并加载数据
watch(() => route.query.projectId, (newProjectId) => {
    if (newProjectId) {
        currentProjectId.value = parseInt(newProjectId as string)
    } else {
        currentProjectId.value = undefined
    }
    fetchData() // 加载数据
})

// --- 核心方法 ---

// 1. 获取传承人列表
const fetchData = async () => {
    loading.value = true
    try {
        const params: any = {
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            name: searchName.value
        }

        // 如果有项目ID参数，则添加项目ID筛选
        if (currentProjectId.value) {
            params.projectId = currentProjectId.value
        }

        const res = await request.get('/inheritors/page', {
            params: params
        })
        if (res.data.code === 200 && res.data.data) {
            tableData.value = res.data.data.records || []
            total.value = res.data.data.total || 0
        } else {
            tableData.value = []
            total.value = 0
        }
    } catch (error) {
        console.error(error)
        ElMessage.error('获取数据失败')
    } finally {
        loading.value = false
    }
}


// 获取所有非遗项目 (用于下拉框)
const fetchProjects = async () => {
    try {
        // ⚠️ 注意：如果你没有写 /api/projects/list 接口，就用分页接口！
        // pageSize=1000 是为了把所有项目都拉下来，防止漏掉
        const res = await request.get('/projects/page?pageNum=1&pageSize=1000')

        if (res.data.code === 200 && res.data.data) {
            // ⚠️ 注意：分页接口返回的数据通常在 records 里
            projectList.value = res.data.data.records || []
        } else {
            projectList.value = []
            ElMessage.error(res.data.msg || '加载项目列表失败')
        }
    } catch (err) {
        console.error('加载项目列表失败', err)
        projectList.value = []
        ElMessage.error('加载项目列表失败，请稍后重试')
    }
}

// 辅助方法：根据ID获取项目名称 (用于表格显示)
const getProjectName = (pid: number) => {
    if (!pid) return '未关联'
    const found = projectList.value.find((p: any) => p.id === pid)
    return found ? found.name : `项目ID:${pid}`
}

// 辅助方法：根据ID获取项目名称 (用于自动添加弹窗)
const getProjectNameById = (pid: number) => {
    if (!pid) return ''
    const found = projectList.value.find((p: any) => p.id === pid)
    return found ? found.name : ''
}

// 3. 打开新增弹窗
const openAddDialog = () => {
    resetForm()
    dialogVisible.value = true
}

// 4. 打开编辑弹窗
const handleEdit = (row: any) => {
    Object.assign(form, row) // 回显数据
    dialogVisible.value = true
}

// 5. 保存 (新增/修改)
const saveInheritor = async () => {
    // 数据验证
    if (!form.name || form.name.trim() === '') {
        ElMessage.warning('请输入姓名')
        return
    }
    if (form.name.trim().length > 50) {
        ElMessage.warning('姓名长度不能超过50个字符')
        return
    }
    if (!form.level) {
        ElMessage.warning('请选择传承级别')
        return
    }
    if (!form.projectId) {
        ElMessage.warning('请选择所属非遗项目')
        return
    }
    if (form.age && (form.age < 1 || form.age > 120)) {
        ElMessage.warning('年龄必须在1-120之间')
        return
    }

    // 构建提交数据
    const submitData = {
        ...form,
        name: form.name.trim(),
        description: form.description ? form.description.trim() : ''
    }

    console.log('保存传承人数据:', submitData)

    try {
        const url = form.id
            ? 'http://localhost:8080/api/inheritors/update'
            : 'http://localhost:8080/api/inheritors/add'

        const method = form.id ? 'put' : 'post'

        const res = await axios[method](url, submitData)

        if (res.data.code === 200) {
            console.log('传承人保存成功:', res.data)
            ElMessage.success(form.id ? '编辑成功' : '新增成功')
            dialogVisible.value = false
            fetchData()
        } else {
            console.error('保存失败:', res.data)
            ElMessage.error(res.data.msg || '保存失败')
        }
    } catch (error: any) {
        console.error('保存传承人请求失败:', error)
        if (error.response) {
            ElMessage.error(`保存失败: ${error.response.data?.msg || error.response.statusText}`)
        } else if (error.request) {
            ElMessage.error('网络错误: 无法连接到服务器')
        } else {
            ElMessage.error('保存失败: ' + error.message)
        }
    }
}

// 6. 删除
const handleDelete = (id: number) => {
    if (!id) {
        ElMessage.error('传承人ID无效')
        return
    }

    ElMessageBox.confirm('确认删除该传承人吗？删除后无法恢复！', '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
    }).then(async () => {
        console.log('开始删除传承人, ID:', id)
        try {
            const res = await request.delete(`/inheritors/delete/${id}`)
            if (res.data.code === 200) {
                console.log('删除成功:', res.data)
                ElMessage.success('删除成功')
                fetchData()
            } else {
                console.error('删除失败:', res.data)
                ElMessage.error(res.data.msg || '删除失败')
            }
        } catch (error: any) {
            console.error('删除传承人请求失败:', error)
            if (error.response) {
                ElMessage.error(`删除失败: ${error.response.data?.msg || error.response.statusText}`)
            } else if (error.request) {
                ElMessage.error('网络错误: 无法连接到服务器')
            } else {
                ElMessage.error('删除失败: ' + error.message)
            }
        }
    }).catch(() => {
        console.log('用户取消删除')
    })
}

// 7. 重置表单
const resetForm = () => {
    form.id = undefined
    form.name = ''
    form.sex = '男'
    form.age = 50
    form.avatarUrl = ''
    form.level = ''
    form.description = ''
    form.projectId = undefined
}

// 8. 打开自动添加弹窗
const openAutoAddDialog = () => {
    autoAddForm.projectId = undefined
    autoAddDialogVisible.value = true
}

// 9. 自动添加传承人
const handleAutoAdd = async () => {
    if (!autoAddForm.projectId) {
        ElMessage.warning('请选择非遗项目')
        return
    }

    autoAddLoading.value = true
    try {
        const res = await request.post(`/inheritors/auto-add/${autoAddForm.projectId}`)
        if (res.data.code === 200) {
            ElMessage.success('自动添加传承人成功')
            autoAddDialogVisible.value = false
            fetchData() // 刷新列表
        } else {
            ElMessage.error(res.data.msg || '自动添加失败')
        }
    } catch (error) {
        ElMessage.error('网络错误，请稍后重试')
    } finally {
        autoAddLoading.value = false
    }
}

// 10. 批量自动添加传承人
const handleBatchAutoAdd = async () => {
    ElMessageBox.confirm('此操作将为所有非遗项目自动添加传承人，已存在传承人的项目将被跳过。确认继续吗？', '批量自动添加', {
        type: 'warning'
    }).then(async () => {
        try {
            const res = await request.post('/inheritors/batch-auto-add')
            if (res.data.code === 200) {
                ElMessage.success(`批量自动添加完成！成功添加 ${res.data.data.successCount || 0} 个项目，跳过 ${res.data.data.skipCount || 0} 个项目`)
                fetchData() // 刷新列表
            } else {
                ElMessage.error(res.data.msg || '批量自动添加失败')
            }
        } catch (error) {
            ElMessage.error('网络错误，请稍后重试')
        }
    })
}

// 11. 打开预览查询弹窗
const openPreviewDialog = () => {
    previewForm.projectName = ''
    previewData.value = []
    previewDialogVisible.value = true
}

// 12. 查询传承人预览
const handlePreviewQuery = async () => {
    if (!previewForm.projectName.trim()) {
        ElMessage.warning('请输入非遗项目名称')
        return
    }

    previewLoading.value = true
    previewData.value = []

    try {
        const res = await request.get('/inheritors/query', {
            params: {
                projectName: previewForm.projectName.trim()
            }
        })
        if (res.data.code === 200) {
            previewData.value = res.data.data || []
            if (previewData.value.length === 0) {
                ElMessage.info('未查询到相关传承人信息')
            }
        } else {
            ElMessage.error(res.data.msg || '查询失败')
        }
    } catch (error) {
        ElMessage.error('网络错误，请稍后重试')
    } finally {
        previewLoading.value = false
    }
}

// 13. 处理表格选择变化
const handleSelectionChange = (selection: any[]) => {
    multipleSelection.value = selection
}

// 14. 批量删除传承人
const handleBatchDelete = async () => {
    if (multipleSelection.value.length === 0) {
        ElMessage.warning('请选择要删除的传承人')
        return
    }

    ElMessageBox.confirm(`确认删除选中的 ${multipleSelection.value.length} 位传承人吗？此操作不可撤销。`, '批量删除', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
    }).then(async () => {
        try {
            const ids = multipleSelection.value.map(item => item.id)
            const res = await request.post('/inheritors/delete/batch', {
                ids: ids
            })
            if (res.data.code === 200) {
                ElMessage.success(`成功删除 ${multipleSelection.value.length} 位传承人`)
                multipleSelection.value = []
                fetchData() // 刷新数据
            } else {
                ElMessage.error(res.data.msg || '批量删除失败')
            }
        } catch (error) {
            ElMessage.error('网络错误，请稍后重试')
        }
    })
}

// --- 头像上传逻辑 ---
const handleAvatarSuccess: UploadProps['onSuccess'] = (response) => {
    // response 是后端 /api/file/upload 返回的 Result 对象
    if (response.code === 200) {
        form.avatarUrl = response.data // 后端应该返回文件 URL
        ElMessage.success('头像上传成功')
    } else {
        ElMessage.error('上传失败')
    }
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
    if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
        ElMessage.error('头像必须是 JPG 或 PNG 格式!')
        return false
    } else if (rawFile.size / 1024 / 1024 > 5) { // 5MB 限制
        ElMessage.error('图片大小不能超过 5MB!')
        return false
    }
    return true
}
</script>

<style scoped>
.inheritor-container {
    padding: 20px;
    background-color: #fff;
    border-radius: 8px;
}

.header-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.search-box {
    display: flex;
    align-items: center;
}

.action-buttons {
    display: flex;
    gap: 10px;
}

.batch-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;
    border: 1px solid #e4e7ed;
}

.batch-selection-info {
    color: #606266;
    font-weight: 500;
}

.pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
}

.preview-container {
    min-height: 300px;
}

.preview-loading {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
    color: #666;
}

.preview-empty {
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.avatar-uploader .avatar {
    width: 100px;
    height: 100px;
    display: block;
}

.avatar-uploader .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
}
</style>