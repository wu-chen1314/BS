<template>
    <div class="inheritor-container">
        <div class="header-actions">
            <div class="search-box">
                <el-input v-model="searchName" placeholder="搜索传承人姓名..." style="width: 200px; margin-right: 10px;"
                    clearable @clear="fetchData" @keyup.enter="fetchData" />
                <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
            </div>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增传承人</el-button>
        </div>

        <el-table :data="tableData" style="width: 100%; margin-top: 20px;" v-loading="loading" border stripe>
           

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
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import axios from 'axios'

// --- 状态定义 ---
const tableData = ref([])
const projectList = ref<any[]>([]) // 存储所有的非遗项目，用于下拉框
const loading = ref(false)
const searchName = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)

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
onMounted(() => {
    fetchData()
    fetchProjects() // 加载项目下拉框数据
})

// --- 核心方法 ---

// 1. 获取传承人列表
const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.get('http://localhost:8080/api/inheritors/page', {
            params: {
                pageNum: pageNum.value,
                pageSize: pageSize.value,
                name: searchName.value
            }
        })
        if (res.data.code === 200) {
            tableData.value = res.data.data.records
            total.value = res.data.data.total
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
        const res = await axios.get('http://localhost:8080/api/projects/page?pageNum=1&pageSize=1000')

        if (res.data.code === 200) {
            // ⚠️ 注意：分页接口返回的数据通常在 records 里
            projectList.value = res.data.data.records
        }
    } catch (err) {
        console.error('加载项目列表失败', err)
    }
}

// 辅助方法：根据ID获取项目名称 (用于表格显示)
const getProjectName = (pid: number) => {
    if (!pid) return '未关联'
    const found = projectList.value.find((p: any) => p.id === pid)
    return found ? found.name : `项目ID:${pid}`
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
    if (!form.name || !form.level || !form.projectId) {
        ElMessage.warning('请补全必填信息')
        return
    }

    try {
        const url = form.id
            ? 'http://localhost:8080/api/inheritors/update'
            : 'http://localhost:8080/api/inheritors/add'

        // 如果是修改，用 put；如果是新增，用 post (根据你后端的实际定义)
        // 通常 update 用 put, add 用 post
        const method = form.id ? 'put' : 'post'

        const res = await axios[method](url, form)

        if (res.data.code === 200) {
            ElMessage.success('保存成功')
            dialogVisible.value = false
            fetchData()
        } else {
            ElMessage.error(res.data.msg || '保存失败')
        }
    } catch (error) {
        ElMessage.error('网络错误')
    }
}

// 6. 删除
const handleDelete = (id: number) => {
    ElMessageBox.confirm('确认删除该传承人吗?', '警告', {
        type: 'warning'
    }).then(async () => {
        const res = await axios.delete(`http://localhost:8080/api/inheritors/delete/${id}`)
        if (res.data.code === 200) {
            ElMessage.success('删除成功')
            fetchData()
        }
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
    margin-bottom: 20px;
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

/* 头像上传样式 */
.avatar-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 100px;
    height: 100px;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: .3s;
}

.avatar-uploader:hover {
    border-color: #409EFF;
}

.avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
}

.avatar {
    width: 100px;
    height: 100px;
    display: block;
    object-fit: cover;
}
</style>