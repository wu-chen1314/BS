<template>
  <article class="project-card heritage-float-card" @click="$emit('open', project)">
    <div class="card-cover">
      <img :src="coverUrl" alt="" />
      <div class="card-cover-overlay"></div>
      <div class="card-top-row">
        <el-checkbox v-if="isAdmin" :model-value="selected" size="large" class="select-box" @click.stop @change="$emit('toggle-select', project.id)" />
        <el-tag :type="levelType" effect="dark" round>{{ project.protectLevel || "未标注" }}</el-tag>
      </div>
      <button v-if="!isAdmin" type="button" class="favorite-button" :class="{ active: isFavorited }" @click.stop="$emit('toggle-favorite', project.id)">
        <el-icon><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
      </button>
    </div>

    <div class="card-body">
      <div class="card-head">
        <h3>{{ project.name }}</h3>
        <span class="views">{{ project.viewCount || 0 }} 浏览</span>
      </div>

      <p class="card-summary">{{ summary }}</p>

      <div class="card-tags">
        <el-tag size="small" effect="plain">{{ categoryLabel }}</el-tag>
        <el-tag size="small" effect="plain" type="info">{{ project.regionName || "地区待补充" }}</el-tag>
        <el-tag size="small" :type="statusType" effect="light">{{ project.status || "状态待补充" }}</el-tag>
      </div>

      <div class="card-footer">
        <div class="footer-info">
          <span class="footer-label">传承人</span>
          <strong>{{ project.inheritorNames || "暂无记录" }}</strong>
        </div>

        <div v-if="isAdmin" class="admin-actions" @click.stop>
          <el-button link type="primary" @click="$emit('edit', project)">编辑</el-button>
          <el-button link type="danger" @click="$emit('delete', project.id)">删除</el-button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Star, StarFilled } from "@element-plus/icons-vue";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import type { HeritageProject } from "@/types/project";
import { getCategoryName, getProtectLevelType, summarizeRichText } from "@/utils/heritage";
import { buildStaticUrl } from "@/utils/url";

const fallbackCover = MATERIAL_PLACEHOLDERS.projectCover;

const props = defineProps<{
  project: HeritageProject;
  isAdmin: boolean;
  isFavorited: boolean;
  selected: boolean;
}>();

defineEmits<{
  (e: "open", project: HeritageProject): void;
  (e: "edit", project: HeritageProject): void;
  (e: "delete", id?: number): void;
  (e: "toggle-select", id?: number): void;
  (e: "toggle-favorite", id?: number): void;
}>();

const coverUrl = computed(() => buildStaticUrl(props.project.coverUrl) || fallbackCover);
const summary = computed(() => summarizeRichText(props.project.history || props.project.features, 78));
const categoryLabel = computed(() => props.project.categoryName || getCategoryName(props.project.categoryId));
const levelType = computed(() => getProtectLevelType(props.project.protectLevel || undefined));
const statusType = computed(() => (props.project.status === "濒危" ? "danger" : "success"));
</script>

<style scoped>
.project-card { 
  display:flex; 
  flex-direction:column; 
  border-radius:20px; 
  overflow:hidden; 
  background: var(--heritage-glass-bg); 
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--heritage-glass-border);
  box-shadow: var(--heritage-glass-shadow); 
  cursor:pointer; 
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.3s ease;
}
.project-card:hover { 
  transform: translateY(-8px) scale(1.02); 
  box-shadow: var(--heritage-card-shadow-hover); 
  border-color: rgba(192, 57, 43, 0.2);
}
.card-cover { position:relative; height:240px; overflow:hidden; }
.card-cover img { width:100%; height:100%; object-fit:cover; transition: transform 0.5s ease; }
.project-card:hover .card-cover img { transform: scale(1.08); }
.card-cover-overlay { position:absolute; inset:0; background:linear-gradient(180deg, rgba(28,40,51,0.05) 0%, rgba(28,40,51,0.6) 100%); transition: opacity 0.3s ease; }
.project-card:hover .card-cover-overlay { opacity: 0.8; }
.card-top-row { position:absolute; top:20px; left:20px; right:20px; display:flex; justify-content:space-between; align-items:flex-start; z-index: 2; }
.select-box { padding:8px 10px; border-radius:999px; background:rgba(255,255,255,0.85); backdrop-filter: blur(8px); }
.favorite-button { position:absolute; right:20px; bottom:20px; width:46px; height:46px; border:none; border-radius:50%; display:inline-flex; align-items:center; justify-content:center; background:rgba(255,255,255,0.85); backdrop-filter: blur(8px); color:var(--heritage-gold); cursor:pointer; font-size: 20px; box-shadow: 0 8px 16px rgba(0,0,0,0.1); transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); z-index: 2;}
.favorite-button:hover { transform: scale(1.1); background: #fff;}
.favorite-button.active { background:var(--heritage-primary); color:#ffffff; }
.card-body { display:grid; gap:18px; padding:24px; position:relative; }
.card-head { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; }
.card-head h3 { margin:0; font-size:24px; color:var(--heritage-ink); font-weight: 700; line-height: 1.3; }
.views { flex-shrink:0; color:var(--heritage-ink-soft); font-size:13px; font-weight: 500; background: rgba(0,0,0,0.04); padding: 4px 10px; border-radius: 12px; }
.card-summary { margin:0; color:var(--heritage-ink-soft); line-height:1.6; min-height:50px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; font-size: 15px; }
.card-tags { display:flex; flex-wrap:wrap; gap:10px; }
.card-tags .el-tag { border-radius: 8px; border: none; padding: 0 12px; height: 28px; font-weight: 500;}
.card-footer { display:flex; justify-content:space-between; gap:16px; align-items:flex-end; margin-top: 8px; padding-top: 18px; border-top: 1px solid var(--heritage-border); }
.footer-info { display:grid; gap:4px; }
.footer-label { color:var(--heritage-muted); font-size:12px; text-transform: uppercase; letter-spacing: 0.05em; font-weight: 600;}
.footer-info strong { color:var(--heritage-ink); font-size: 15px; font-weight: 600;}
.admin-actions { display:flex; gap:10px; }
</style>
