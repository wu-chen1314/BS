<template>
  <div class="region-category-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">地区分类</h2>
        <p class="page-description">从地域与类别两个维度浏览全国非遗项目，快速定位项目分布与代表作品。</p>
      </div>
    </div>

    <el-card class="filter-card" shadow="hover">
      <div class="filter-title">
        <el-icon><Filter /></el-icon>
        <span>筛选条件</span>
      </div>
      <div class="filter-content">
        <div class="filter-item">
          <label class="filter-label">省份</label>
          <el-select
            v-model="selectedProvince"
            placeholder="选择省份"
            style="width: 200px"
            clearable
            @change="handleProvinceChange"
          >
            <el-option v-for="province in provinces" :key="province.id" :label="province.name" :value="province.id" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">城市</label>
          <el-select
            v-model="selectedCity"
            placeholder="选择城市"
            style="width: 200px"
            clearable
            :disabled="!selectedProvince"
            @change="handleCityChange"
          >
            <el-option v-for="city in cities" :key="city.id" :label="city.name" :value="city.id" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">类别</label>
          <el-cascader
            v-model="selectedCategory"
            :options="categoryTree"
            placeholder="选择非遗类别"
            style="width: 240px"
            clearable
            :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children' }"
            @change="handleCategoryChange"
          />
        </div>

        <div class="filter-item">
          <el-button type="primary" :icon="Refresh" @click="resetFilters" class="reset-btn">重置筛选</el-button>
        </div>
      </div>
    </el-card>

    <div class="statistics-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon red"><el-icon size="32"><Document /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ regionStats.totalProjects || 0 }}</div>
                <div class="stat-label">项目总数</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon gold"><el-icon size="32"><Grid /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ regionStats.categoryCount || 0 }}</div>
                <div class="stat-label">覆盖类别</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon green"><el-icon size="32"><User /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ regionStats.inheritorCount || 0 }}</div>
                <div class="stat-label">传承人数</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon orange"><el-icon size="32"><Location /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ categoryStats.regionCount || 0 }}</div>
                <div class="stat-label">覆盖地区</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

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
        <el-table-column prop="name" label="项目名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="类别" width="150" align="center">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.categoryName || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="地区" width="150" align="center">
          <template #default="scope">
            <el-tag type="success" size="small">{{ scope.row.regionName || '未设置' }}</el-tag>
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
            <el-button type="primary" link size="small" @click.stop="viewDetail(scope.row.id)">查看详情</el-button>
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
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Document, Filter, Grid, List, Location, Refresh, User } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();

const loading = ref(false);
const provinces = ref<any[]>([]);
const cities = ref<any[]>([]);
const categoryTree = ref<any[]>([]);
const projectList = ref<any[]>([]);

const selectedProvince = ref<number | null>(null);
const selectedCity = ref<number | null>(null);
const selectedCategory = ref<number[]>([]);

const page = ref(1);
const limit = ref(10);
const total = ref(0);

const regionStats = ref<any>({
  totalProjects: 0,
  categoryCount: 0,
  inheritorCount: 0,
});

const categoryStats = ref<any>({
  totalProjects: 0,
  regionCount: 0,
  inheritorCount: 0,
});

onMounted(() => {
  fetchInitialData();
});

const fetchInitialData = async () => {
  loading.value = true;
  try {
    const res = await request.get("/region-category/all");
    if (res.data.code === 200) {
      const data = res.data.data || {};
      provinces.value = data.provinces || [];
      categoryTree.value = data.categoryTree || [];
      regionStats.value = data.regionStatistics || {};
      categoryStats.value = data.categoryStatistics || {};
      await fetchProjects();
    } else {
      ElMessage.error(res.data.msg || "加载地区分类数据失败");
    }
  } catch (error: any) {
    console.error("加载地区分类数据失败", error);
    ElMessage.error(error.message || "加载地区分类数据失败");
  } finally {
    loading.value = false;
  }
};

const fetchProjects = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: page.value,
      limit: limit.value,
    };

    if (selectedCity.value) {
      params.regionId = selectedCity.value;
    } else if (selectedProvince.value) {
      params.regionId = selectedProvince.value;
    }

    if (selectedCategory.value.length > 0) {
      params.categoryId = selectedCategory.value[selectedCategory.value.length - 1];
    }

    const res = await request.get("/region-category/projects", { params });
    if (res.data.code === 200) {
      const data = res.data.data || {};
      projectList.value = data.records || [];
      total.value = data.total || 0;
    } else {
      ElMessage.error(res.data.msg || "加载项目列表失败");
    }
  } catch (error: any) {
    console.error("加载项目列表失败", error);
    ElMessage.error(error.message || "加载项目列表失败");
  } finally {
    loading.value = false;
  }
};

const handleProvinceChange = async (provinceId: number) => {
  selectedCity.value = null;
  cities.value = [];
  page.value = 1;

  if (provinceId) {
    try {
      const res = await request.get("/region-category/cities", {
        params: { provinceId },
      });
      if (res.data.code === 200) {
        cities.value = res.data.data || [];
      }
    } catch (error: any) {
      console.error("加载城市列表失败", error);
    }
  }

  await Promise.all([fetchProjects(), updateStatistics()]);
};

const handleCityChange = async () => {
  page.value = 1;
  await Promise.all([fetchProjects(), updateStatistics()]);
};

const handleCategoryChange = async () => {
  page.value = 1;
  await Promise.all([fetchProjects(), updateStatistics()]);
};

const updateStatistics = async () => {
  try {
    const regionId = selectedCity.value || selectedProvince.value || undefined;
    const categoryId = selectedCategory.value.length > 0
      ? selectedCategory.value[selectedCategory.value.length - 1]
      : undefined;

    const [regionRes, categoryRes] = await Promise.all([
      request.get("/region-category/statistics/by-region", { params: { regionId } }),
      request.get("/region-category/statistics/by-category", { params: { categoryId } }),
    ]);

    if (regionRes.data.code === 200) {
      regionStats.value = regionRes.data.data || {};
    }
    if (categoryRes.data.code === 200) {
      categoryStats.value = categoryRes.data.data || {};
    }
  } catch (error: any) {
    console.error("更新统计数据失败", error);
  }
};

const resetFilters = async () => {
  selectedProvince.value = null;
  selectedCity.value = null;
  selectedCategory.value = [];
  cities.value = [];
  page.value = 1;
  await fetchInitialData();
};

const handleRowClick = (row: any) => {
  viewDetail(row.id);
};

const viewDetail = (projectId: number) => {
  router.push(`/home?id=${projectId}`);
};

const getProtectLevelType = (level: string) => {
  const typeMap: Record<string, string> = {
    国家级: "danger",
    省级: "warning",
    市级: "success",
    县级: "info",
  };
  return typeMap[level] || "info";
};
</script>

<style scoped>
.region-category-container {
  padding: 20px;
  background: linear-gradient(180deg, #faf7f2 0%, #f5f7fa 100%);
  min-height: calc(100vh - 84px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.filter-card,
.list-card,
.stat-card {
  border-radius: 18px;
  border: 1px solid rgba(196, 30, 58, 0.08);
  box-shadow: 0 12px 40px rgba(44, 62, 80, 0.06);
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

.filter-title .el-icon,
.card-title .el-icon {
  color: #c41e3a;
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

.statistics-section {
  margin: 20px 0;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.red { background-color: #c41e3a; }
.stat-icon.gold { background-color: #d4af37; }
.stat-icon.green { background-color: #52c41a; }
.stat-icon.orange { background-color: #fa8c16; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label,
.total-count {
  font-size: 14px;
  color: #999;
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

.project-table {
  cursor: pointer;
}

.project-table :deep(.el-table__row:hover) {
  background-color: #fff7f6;
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