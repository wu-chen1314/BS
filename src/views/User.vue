<template>
    <div class="app-main">
        <el-card>
            <template #header>
                <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; gap: 10px;">
                        <el-input v-model="searchName" placeholder="搜索用户名" style="width: 220px;" clearable @clear="fetchData" />
                        <el-button type="primary" @click="fetchData">查询</el-button>
                    </div>
                    <el-button type="success" @click="openAddDialog" v-if="currentUser.role === 'admin'">新增用户</el-button>
                </div>
            </template>

            <el-table :data="tableData" stripe border style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="nickname" label="昵称" width="120" />
                <el-table-column prop="role" label="角色" width="100">
                    <template #default="scope">
                        <el-tag :type="scope.row.role === 'admin' ? 'danger' : 'success'">
                            {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="80">
                    <template #default="scope">
                        <el-tag type="success" v-if="scope.row.status === 1">正常</el-tag>
                        <el-tag type="info" v-else>禁用</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="scope">
                        <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button link type="warning" size="small" @click="handleResetPwd(scope.row.id)" v-if="currentUser.role === 'admin'">重置密码</el-button>
                        <el-button link type="danger" size="small" @click="handleDelete(scope.row.id)" v-if="currentUser.role === 'admin'">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
                <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" @current-change="handlePageChange" />
            </div>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="用户名">
                    <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
                </el-form-item>
                <el-form-item label="昵称">
                    <el-input v-model="form.nickname" placeholder="显示名称" />
                </el-form-item>
                <el-form-item label="角色">
                    <el-radio-group v-model="form.role" :disabled="currentUser.role !== 'admin'">
                        <el-radio value="user">普通用户</el-radio>
                        <el-radio value="admin">管理员</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="form.email" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status" :disabled="currentUser.role !== 'admin'">
                        <el-radio :value="1">正常</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveUser" :loading="btnLoading">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref<any[]>([])
const total = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)
const searchName = ref('')
const dialogVisible = ref(false)
const loading = ref(false)
const btnLoading = ref(false)
const currentUser = ref<any>({})

const form = reactive({
    id: undefined as number | undefined,
    username: '',
    nickname: '',
    role: 'user',
    email: '',
    status: 1,
})

onMounted(() => {
    const str = sessionStorage.getItem('user')
    if (str) {
        currentUser.value = JSON.parse(str)
    }
    fetchData()
})

const fetchData = async () => {
    loading.value = true
    try {
        const res = await request.get('/users/page', {
            params: {
                pageNum: currentPage.value,
                pageSize: pageSize.value,
                keyword: searchName.value,
            },
        })
        tableData.value = res.data.data.records
        total.value = res.data.data.total
    } catch {
        ElMessage.error('数据加载失败')
    } finally {
        loading.value = false
    }
}

const openAddDialog = () => {
    Object.assign(form, { id: undefined, username: '', nickname: '', role: 'user', email: '', status: 1 })
    dialogVisible.value = true
}

const handleEdit = (row: any) => {
    Object.assign(form, row)
    dialogVisible.value = true
}

const saveUser = async () => {
    if (!form.username) {
        ElMessage.warning('请输入用户名')
        return
    }

    btnLoading.value = true
    try {
        const res = form.id
            ? await request.put('/users/update', form)
            : await request.post('/users/add', form)

        ElMessage.success(res.data.msg || '保存成功')
        dialogVisible.value = false
        fetchData()
    } catch {
        ElMessage.error('操作失败')
    } finally {
        btnLoading.value = false
    }
}

const handleDelete = (id: number) => {
    if (id === currentUser.value.id) {
        ElMessage.error('不能删除当前登录账号')
        return
    }

    ElMessageBox.confirm('确定删除该用户吗？', '警告', { type: 'warning' }).then(async () => {
        try {
            await request.delete(`/users/delete/${id}`)
            ElMessage.success('删除成功')
            fetchData()
        } catch {
            ElMessage.error('删除失败')
        }
    })
}

const handleResetPwd = (id: number) => {
    ElMessageBox.confirm('确定将密码重置为 123456 吗？', '警告', { type: 'warning' }).then(async () => {
        await request.put(`/users/reset-password/${id}`)
        ElMessage.success('重置成功，新密码为 123456')
    })
}

const handlePageChange = (val: number) => {
    currentPage.value = val
    fetchData()
}
</script>

<style scoped>
.app-main {
    padding: 20px;
}
</style>
