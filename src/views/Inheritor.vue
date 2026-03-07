<template>
  <div class="inheritor-page">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchName" placeholder="Search inheritor" clearable @clear="fetchData" @keyup.enter="fetchData" style="width: 240px" />
        <el-button type="primary" @click="fetchData">Search</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openAddDialog">Add Inheritor</el-button>
      </div>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="Avatar" width="100">
        <template #default="scope">
          <el-avatar :size="44" shape="square" :src="getFileUrl(scope.row.avatarUrl)" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="Name" min-width="140" />
      <el-table-column prop="sex" label="Sex" width="100" />
      <el-table-column prop="age" label="Age" width="100" />
      <el-table-column prop="level" label="Level" min-width="180" show-overflow-tooltip />
      <el-table-column prop="projectName" label="Project" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="Description" min-width="220" show-overflow-tooltip />
      <el-table-column label="Actions" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleEdit(scope.row)">Edit</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? 'Edit Inheritor' : 'Add Inheritor'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Avatar">
          <el-upload class="avatar-uploader" :action="uploadUrl" :show-file-list="false" :on-success="handleAvatarSuccess">
            <img v-if="form.avatarUrl" :src="getFileUrl(form.avatarUrl)" class="avatar-preview" />
            <el-button v-else>Upload</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="Name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Sex">
          <el-select v-model="form.sex" style="width: 100%">
            <el-option label="Male" value="Male" />
            <el-option label="Female" value="Female" />
          </el-select>
        </el-form-item>
        <el-form-item label="Age">
          <el-input-number v-model="form.age" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="Level">
          <el-input v-model="form.level" />
        </el-form-item>
        <el-form-item label="Project">
          <el-select v-model="form.projectId" filterable style="width: 100%">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveInheritor">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { toApiUrl, toServerUrl } from '@/utils/url'

const route = useRoute()
const loading = ref(false)
const tableData = ref<any[]>([])
const projectList = ref<any[]>([])
const searchName = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentProjectId = ref<number | undefined>()
const dialogVisible = ref(false)

const form = reactive<any>({
  id: undefined,
  name: '',
  sex: 'Male',
  age: 30,
  avatarUrl: '',
  level: '',
  description: '',
  projectId: undefined,
})

const fetchProjects = async () => {
  const res = await request.get('/projects/page', { params: { pageNum: 1, pageSize: 1000 } })
  projectList.value = res.data.data?.records || []
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
    }
    if (currentProjectId.value) params.projectId = currentProjectId.value
    const res = await request.get('/inheritors/page', { params })
    tableData.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load inheritors')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    name: '',
    sex: 'Male',
    age: 30,
    avatarUrl: '',
    level: '',
    description: '',
    projectId: currentProjectId.value,
  })
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const saveInheritor = async () => {
  if (!form.name?.trim()) return ElMessage.warning('Name is required')
  if (!form.level?.trim()) return ElMessage.warning('Level is required')
  if (!form.projectId) return ElMessage.warning('Project is required')

  const payload = {
    ...form,
    name: form.name.trim(),
    description: form.description?.trim() || '',
  }

  try {
    const res = form.id
      ? await request.put('/inheritors/update', payload)
      : await request.post('/inheritors/add', payload)
    if (res.data.code === 200) {
      ElMessage.success(form.id ? 'Updated successfully' : 'Created successfully')
      dialogVisible.value = false
      fetchData()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || 'Save failed')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('Delete this inheritor?', 'Confirm', { type: 'warning' })
    const res = await request.delete(`/inheritors/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('Deleted successfully')
      fetchData()
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.msg || 'Delete failed')
    }
  }
}

const handleAvatarSuccess = (res: any) => {
  if (res.code === 200) {
    form.avatarUrl = res.data
  }
}

const uploadUrl = toApiUrl('/file/upload')

const getFileUrl = (url?: string) => {
  return toServerUrl(url)
}

onMounted(async () => {
  if (route.query.projectId) {
    currentProjectId.value = Number(route.query.projectId)
  }
  await fetchProjects()
  await fetchData()
})

watch(() => route.query.projectId, async (value) => {
  currentProjectId.value = value ? Number(value) : undefined
  pageNum.value = 1
  await fetchData()
})
</script>

<style scoped>
.inheritor-page {
  padding: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
}
</style>