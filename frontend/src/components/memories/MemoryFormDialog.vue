<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MemoryImage from '@/components/MemoryImage.vue'
import { uploadImage } from '@/api/files'
import { createMemory, updateMemory } from '@/api/memories'
import type { FileInfo, Memory, MemoryInput } from '@/types/api'

const props = defineProps<{
  modelValue: boolean
  memory: Memory | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  saved: []
}>()

const submitting = ref(false)
const uploading = ref(false)
const selectedFiles = ref<FileInfo[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const tagsText = ref('')

function today() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

const form = reactive<MemoryInput>({
  title: '',
  content: '',
  eventDate: today(),
  locationName: '',
  latitude: null,
  longitude: null,
  starType: 'CLASSIC',
  tags: [],
  mediaFileIds: [],
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (visible) => emit('update:modelValue', visible),
})

function populateForm() {
  const memory = props.memory
  Object.assign(form, {
    title: memory?.title || '',
    content: memory?.content || '',
    eventDate: memory?.eventDate || today(),
    locationName: memory?.locationName || '',
    latitude: memory?.latitude ?? null,
    longitude: memory?.longitude ?? null,
    starType: memory?.starType || 'CLASSIC',
    tags: memory ? [...memory.tags] : [],
    mediaFileIds: memory ? memory.media.map((item) => item.fileId) : [],
  })
  tagsText.value = memory?.tags.join('，') || ''
  selectedFiles.value = memory?.media.map((item) => item.file) || []
}

watch(
  () => [props.modelValue, props.memory],
  ([visible]) => {
    if (visible) populateForm()
  },
  { immediate: true },
)

async function handleFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  if (selectedFiles.value.length + files.length > 9) {
    ElMessage.warning('每段回忆最多添加 9 张图片')
    return
  }

  uploading.value = true
  try {
    for (const file of files) {
      if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
        ElMessage.error(`${file.name} 不是支持的图片格式`)
        continue
      }
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.error(`${file.name} 超过 10MB`)
        continue
      }
      try {
        const uploaded = await uploadImage(file)
        selectedFiles.value.push(uploaded)
        form.mediaFileIds.push(uploaded.id)
      } catch (cause) {
        ElMessage.error(cause instanceof Error ? cause.message : `${file.name} 上传失败`)
      }
    }
  } finally {
    uploading.value = false
  }
}

function removeFile(index: number) {
  selectedFiles.value.splice(index, 1)
  form.mediaFileIds.splice(index, 1)
}

async function save() {
  if (submitting.value || uploading.value) return
  if (!form.title.trim()) {
    ElMessage.warning('请填写回忆标题')
    return
  }
  if (!form.eventDate) {
    ElMessage.warning('请选择回忆日期')
    return
  }
  const latitude = form.latitude === null || String(form.latitude).trim() === ''
    ? null
    : Number(form.latitude)
  const longitude = form.longitude === null || String(form.longitude).trim() === ''
    ? null
    : Number(form.longitude)
  if ((latitude !== null && !Number.isFinite(latitude))
    || (longitude !== null && !Number.isFinite(longitude))) {
    ElMessage.warning('请输入有效的经纬度')
    return
  }
  if ((latitude === null) !== (longitude === null)) {
    ElMessage.warning('纬度和经度需要同时填写')
    return
  }
  if (latitude !== null && (latitude < -90 || latitude > 90)) {
    ElMessage.warning('纬度必须在 -90 到 90 之间')
    return
  }
  if (longitude !== null && (longitude < -180 || longitude > 180)) {
    ElMessage.warning('经度必须在 -180 到 180 之间')
    return
  }
  form.latitude = latitude
  form.longitude = longitude
  const tags = tagsText.value
    .split(/[,，\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
  if (tags.length > 10) {
    ElMessage.warning('每段回忆最多添加 10 个标签')
    return
  }
  if (tags.some((tag) => tag.length > 50)) {
    ElMessage.warning('单个标签不能超过 50 个字符')
    return
  }
  form.tags = tags

  submitting.value = true
  try {
    if (props.memory) {
      await updateMemory(props.memory.id, { ...form })
      ElMessage.success('回忆已更新')
    } else {
      await createMemory({ ...form })
      ElMessage.success('新的回忆已点亮')
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="memory ? '编辑回忆' : '记录新回忆'"
    width="min(680px, calc(100vw - 32px))"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="200" placeholder="给这段回忆一个名字" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="日期">
          <el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="星光类型">
          <el-select v-model="form.starType" style="width: 100%">
            <el-option label="经典星光" value="CLASSIC" />
            <el-option label="暖金星光" value="GOLDEN" />
            <el-option label="玫瑰星光" value="ROSE" />
            <el-option label="蔚蓝星光" value="BLUE" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="地点（可选）">
        <el-input v-model="form.locationName" maxlength="255" placeholder="例如：第一次旅行的海边" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="纬度（可选）">
          <el-input v-model="form.latitude" type="number" placeholder="-90 到 90" />
        </el-form-item>
        <el-form-item label="经度（可选）">
          <el-input v-model="form.longitude" type="number" placeholder="-180 到 180" />
        </el-form-item>
      </div>
      <el-form-item label="回忆内容">
        <el-input v-model="form.content" type="textarea" :rows="5" maxlength="20000" />
      </el-form-item>
      <el-form-item label="标签（最多 10 个，用逗号或空格分隔）">
        <el-input v-model="tagsText" placeholder="旅行，第一次，海边" />
      </el-form-item>
      <el-form-item label="图片（最多 9 张，JPEG / PNG / GIF，单张不超过 10MB）">
        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif" multiple hidden @change="handleFiles" />
        <div class="upload-row">
          <el-button :loading="uploading" @click="fileInput?.click()">选择并上传图片</el-button>
          <span>{{ selectedFiles.length }} / 9</span>
        </div>
        <div v-if="selectedFiles.length" class="selected-files">
          <div v-for="(file, index) in selectedFiles" :key="file.id" class="selected-file">
            <MemoryImage :file-id="file.id" :alt="file.originalName" />
            <el-button text @click="removeFile(index)">移除</el-button>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting || uploading" @click="save">保存回忆</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.upload-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  color: #818aa1;
  font-size: 11px;
}

.selected-files {
  display: grid;
  width: 100%;
  margin-top: 12px;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.selected-file {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: #0a0e1d;
}

.selected-file :deep(img),
.selected-file :deep(.image-error) {
  height: 100px;
}

.selected-file :deep(.el-button) {
  width: 100%;
  margin: 0;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .selected-files {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
