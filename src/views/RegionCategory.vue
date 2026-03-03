<template>
    <div class="region-category-container">
        <div class="page-header">
            <h2 class="page-title">地区分类</h2>
            <p class="page-description">探索全国各地区非物质文化遗产项目分布</p>
        </div>

        <div class="filter-section">
            <el-card class="filter-card" shadow="hover">
                <div class="filter-title">
                    <el-icon><Filter /></el-icon>
                    <span>筛选条件</span>
                </div>
                <div class="filter-content">
                    <div class="filter-item">
                        <label class="filter-label">省份：</label>
                        <el-select 
                            v-model="selectedProvince" 
                            placeholder="选择省份" 
                            style="width: 200px"
                            clearable
                            @change="handleProvinceChange"
                        >
                            <el-option 
                                v-for="province in provinces" 
                                :key="province.id" 
                                :label="province.name" 
                                :value="province.id" 
                            />
                        </el-select>
                    </div>
                    
                    <div class="filter-item">
                        <label class="filter-label">城市：</label>
                        <el-select 
                            v-model="selectedCity" 
                            placeholder="选择城市" 
                            style="width: 200px"
                            clearable
                            @change="handleCityChange"
                            :disabled="!selectedProvince"
                        >
                            <el-option 
                                v-for="city in cities" 
                                :key="city.id" 
                                :label="city.name" 
                                :value="city.id" 
                            />
                        </el-select>
                    </div>
                    
                    <div class="filter-item">
                        <label class="filter-label">类别：</label>
                        <el-cascader 
                            v-model="selectedCategory"
                            :options="categoryTree"
                            placeholder="选择非遗类别"
                            style="width: 220px"
                            clearable
                            @change="handleCategoryChange"
                            :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children' }"
                        />
                    </div>
                    
                    <div class="filter-item">
                        <el-button 
                            type="primary" 
                            :icon="Refresh" 
                            @click="resetFilters"
                            class="reset-btn"
                        >
                            重置
                        </el-button>
                    </div>
                </div>
            </el-card>
        </div>

        <div class="statistics-section">
            <el-row :gutter="20">
                <el-col :xs="24" :sm="12" :md="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon" style="background-color: #C41E3A;">
                                <el-icon size="32"><Document /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ regionStats.totalProjects || 0 }}</div>
                                <div class="stat-label">非遗项目总数</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                
                <el-col :xs="24" :sm="12" :md="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon" style="background-color: #D4AF37;">
                                <el-icon size="32"><Grid /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ regionStats.categoryCount || 0 }}</div>
                                <div class="stat-label">覆盖类别数</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                
                <el-col :xs="24" :sm="12" :md="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon" style="background-color: #52C41A;">
                                <el-icon size="32"><User /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ regionStats.inheritorCount || 0 }}</div>
                                <div class="stat-label">传承人数量</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                
                <el-col :xs="24" :sm="12" :md="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon" style="background-color: #FA8C16;">
                                <el-icon size="32"><Location /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ categoryStats.regionCount || 0 }}</div>
                                <div class="stat-label">覆盖地区数</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <div class="project-list-section">
            <el-card class="list-card" shadow="hover">
                <template #header>
                    <div class="card-header">
                        <span class="card-title">
                            <el-icon><List /></el-icon>
                            非遗项目列表
                        </span>
                        <span class="total-count">共 {{ total }} 条</span>
                    </div>
                </template>
                
                <el-table 
                    :data="projectList" 
                    border 
                    stripe
                    v-loading="loading"
                    class="project-table"
                    @row-click="handleRowClick"
                >
                    <el-table-column prop="name" label="项目名称" min-width="200" show-overflow-tooltip />
                    <el-table-column prop="categoryName" label="类别" width="150" align="center">
                        <template #default="scope">
                            <el-tag type="info" size="small">{{ scope.row.categoryName || '未分类' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="regionName" label="地区" width="150" align="center">
                        <template #default="scope">
                            <el-tag type="success" size="small">{{ scope.row.regionName || '未分配' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="protectLevel" label="保护级别" width="120" align="center">
                        <template #default="scope">
                            <el-tag :type="getProtectLevelType(scope.row.protectLevel)" size="small">
                                {{ scope.row.protectLevel || '未定级' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" align="center" fixed="right">
                        <template #default="scope">
                            <el-button 
                                type="primary" 
                                link 
                                size="small"
                                @click.stop="viewDetail(scope.row.id)"
                            >
                                详情
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
                
                <div class="pagination-container">
                    <el-pagination
                        v-model:current-page="page"
                        v-model:page-size="limit"
                        :page-sizes="[10, 20, 50, 100]"
                        layout="total, sizes, prev, pager, next, jumper"
                        :total="total"
                        @size-change="fetchProjects"
                        @current-change="fetchProjects"
                    />
                </div>
            </el-card>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
    Filter, 
    Refresh, 
    Document, 
    Grid, 
    User, 
    Location, 
    List 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const provinces = ref<any[]>([])
const cities = ref<any[]>([])
const categoryTree = ref<any[]>([])
const projectList = ref<any[]>([])

const selectedProvince = ref<number | null>(null)
const selectedCity = ref<number | null>(null)
const selectedCategory = ref<number[]>([])

const page = ref(1)
const limit = ref(10)
const total = ref(0)

const regionStats = ref<any>({
    totalProjects: 0,
    categoryCount: 0,
    inheritorCount: 0
})

const categoryStats = ref<any>({
    totalProjects: 0,
    regionCount: 0,
    inheritorCount: 0
})

onMounted(() => {
    fetchInitialData()
})

const fetchInitialData = async () => {
    loading.value = true
    try {
        const res = await request.get('/region-category/all')
        if (res.code === 0) {
            provinces.value = res.data.provinces || []
            categoryTree.value = res.data.categoryTree || []
            regionStats.value = res.data.regionStatistics || {}
            categoryStats.value = res.data.categoryStatistics || {}
            fetchProjects()
        } else {
            ElMessage.error(res.message || '加载数据失败')
        }
    } catch (error: any) {
        console.error('加载初始数据失败:', error)
        ElMessage.error('加载数据失败：' + error.message)
    } finally {
        loading.value = false
    }
}

const fetchProjects = async () => {
    loading.value = true
    try {
        const params: any = {
            page: page.value,
            limit: limit.value
        }
        
        if (selectedCity.value) {
            params.regionId = selectedCity.value
        } else if (selectedProvince.value) {
            params.regionId = selectedProvince.value
        }
        
        if (selectedCategory.value && selectedCategory.value.length > 0) {
            params.categoryId = selectedCategory.value[selectedCategory.value.length - 1]
        }
        
        const res = await request.get('/region-category/projects', { params })
        if (res.code === 0) {
            projectList.value = res.data.records || []
            total.value = res.data.total || 0
        } else {
            ElMessage.error(res.message || '加载项目列表失败')
        }
    } catch (error: any) {
        console.error('加载项目列表失败:', error)
        ElMessage.error('加载项目列表失败：' + error.message)
    } finally {
        loading.value = false
    }
}

const handleProvinceChange = async (provinceId: number) => {
    selectedCity.value = null
    cities.value = []
    
    if (provinceId) {
        try {
            const res = await request.get('/region-category/cities', {
                params: { provinceId }
            })
            if (res.code === 0) {
                cities.value = res.data || []
            }
        } catch (error: any) {
            console.error('加载城市列表失败:', error)
        }
    }
    
    fetchProjects()
    updateStatistics()
}

const handleCityChange = () => {
    fetchProjects()
    updateStatistics()
}

const handleCategoryChange = () => {
    fetchProjects()
    updateStatistics()
}

const updateStatistics = async () => {
    try {
        const regionId = selectedCity.value || selectedProvince.value || null
        const categoryId = selectedCategory.value && selectedCategory.value.length > 0 
            ? selectedCategory.value[selectedCategory.value.length - 1] 
            : null
        
        const [regionRes, categoryRes] = await Promise.all([
            request.get('/region-category/statistics/by-region', {
                params: { regionId: regionId || undefined }
            }),
            request.get('/region-category/statistics/by-category', {
                params: { categoryId: categoryId || undefined }
            })
        ])
        
        if (regionRes.code === 0) {
            regionStats.value = regionRes.data || {}
        }
        if (categoryRes.code === 0) {
            categoryStats.value = categoryRes.data || {}
        }
    } catch (error: any) {
        console.error('更新统计数据失败:', error)
    }
}

const resetFilters = () => {
    selectedProvince.value = null
    selectedCity.value = null
    selectedCategory.value = []
    cities.value = []
    page.value = 1
    
    fetchInitialData()
}

const handleRowClick = (row: any) => {
    viewDetail(row.id)
}

const viewDetail = (projectId: number) => {
    router.push(`/home?id=${projectId}`)
}

const getProtectLevelType = (level: string) => {
    const typeMap: any = {
        '国家级': 'danger',
        '省级': 'warning',
        '市级': 'success',
        '县级': 'info'
    }
    return typeMap[level] || 'info'
}
</script>

<style scoped>
.region-category-container {
    padding: 20px;
    background-color: #f5f7fa;
    min-height: calc(100vh - 84px);
}

.page-header {
    margin-bottom: 20px;
}

.page-title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0 0 8px 0;
}

.page-description {
    font-size: 14px;
    color: #666;
    margin: 0;
}

.filter-section {
    margin-bottom: 20px;
}

.filter-card {
    border-radius: 8px;
}

.filter-card :deep(.el-card__body) {
    padding: 20px;
}

.filter-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
}

.filter-title .el-icon {
    color: #C41E3A;
}

.filter-content {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: center;
}

.filter-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.filter-label {
    font-size: 14px;
    color: #666;
    white-space: nowrap;
}

.reset-btn {
    margin-left: 8px;
}

.statistics-section {
    margin-bottom: 20px;
}

.stat-card {
    border-radius: 8px;
    margin-bottom: 0;
}

.stat-card :deep(.el-card__body) {
    padding: 20px;
}

.stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
}

.stat-icon {
    width: 64px;
    height: 64px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    flex-shrink: 0;
}

.stat-info {
    flex: 1;
}

.stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #333;
    line-height: 1;
    margin-bottom: 4px;
}

.stat-label {
    font-size: 14px;
    color: #999;
}

.project-list-section {
    margin-bottom: 20px;
}

.list-card {
    border-radius: 8px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #333;
}

.card-title .el-icon {
    color: #C41E3A;
}

.total-count {
    font-size: 14px;
    color: #999;
}

.project-table {
    cursor: pointer;
}

.project-table :deep(.el-table__row:hover) {
    background-color: #f5f7fa;
}

.pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
}

@media (max-width: 768px) {
    .filter-content {
        flex-direction: column;
        align-items: stretch;
    }
    
    .filter-item {
        width: 100%;
    }
    
    .filter-item :deep(.el-select),
    .filter-item :deep(.el-cascader) {
        width: 100% !important;
    }
    
    .stat-value {
        font-size: 24px;
    }
    
    .stat-icon {
        width: 48px;
        height: 48px;
    }
}
</style>
