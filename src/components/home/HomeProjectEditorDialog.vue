<template>
  <el-dialog :model-value="visible" :title="form.id ? '编辑项目' : '新增项目'" width="760px" destroy-on-close @close="$emit('update:visible', false)">
    <el-form :model="form" label-width="92px" class="editor-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="项目名称">
            <el-input v-model="form.name" placeholder="请输入项目名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="保护级别">
            <el-select v-model="form.protectLevel" placeholder="请选择保护级别">
              <el-option v-for="item in protectLevels" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="项目类别">
            <el-select v-model="form.categoryId" placeholder="请选择项目类别">
              <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="传承状态">
            <el-select v-model="form.status" placeholder="请选择传承状态">
              <el-option label="在传承" value="在传承" />
              <el-option label="濒危" value="濒危" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="地区编号">
            <el-input-number v-model="form.regionId" :min="1" :step="1" controls-position="right" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="传承人 ID">
            <el-select v-model="inheritorIds" multiple collapse-tags collapse-tags-tooltip filterable placeholder="可选绑定传承人">
              <el-option v-for="item in inheritorOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="封面图片">
            <el-upload :action="uploadAction" :headers="uploadHeaders" :show-file-list="false" :on-success="(response) => emit('upload-cover-success', response)">
              <el-button plain>上传封面</el-button>
            </el-upload>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="视频资料">
            <el-upload :action="uploadAction" :headers="uploadHeaders" :show-file-list="false" :on-success="(response) => emit('upload-video-success', response)">
              <el-button plain>上传视频</el-button>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="历史介绍">
        <el-input v-model="form.history" type="textarea" :rows="7" placeholder="支持粘贴现有富文本内容，保存时会原样提交给后端" />
      </el-form-item>

      <el-form-item label="项目特点">
        <el-input v-model="form.features" type="textarea" :rows="4" placeholder="补充项目亮点、工艺特点或传播亮点" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('save')">保存项目</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { HeritageProject } from "@/types/project";

const props = defineProps<{
  visible: boolean;
  form: HeritageProject;
  inheritorIds: number[];
  categories: Array<{ value: number; label: string }>;
  protectLevels: Array<{ value: string; label: string }>;
  inheritorOptions: Array<{ value: number; label: string }>;
  uploadAction: string;
  uploadHeaders: Record<string, string>;
  saving: boolean;
}>();

const emit = defineEmits<{
  (e: "update:visible", value: boolean): void;
  (e: "update:inheritor-ids", value: number[]): void;
  (e: "save"): void;
  (e: "upload-cover-success", payload: any): void;
  (e: "upload-video-success", payload: any): void;
}>();

const inheritorIds = computed({
  get: () => props.inheritorIds,
  set: (value: number[]) => emit("update:inheritor-ids", value),
});
</script>

<style scoped>
.editor-form :deep(.el-select),
.editor-form :deep(.el-input-number.full-width) {
  width: 100%;
}
</style>
