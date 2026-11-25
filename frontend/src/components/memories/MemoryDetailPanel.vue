<script setup lang="ts">
import { computed } from 'vue'
import MemoryImage from '@/components/MemoryImage.vue'
import type { Memory } from '@/types/api'

const props = defineProps<{
  modelValue: boolean
  memory: Memory | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  edit: [memory: Memory]
  delete: [memory: Memory]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const starLabels: Record<string, string> = {
  CLASSIC: '经典星光',
  GOLDEN: '暖金星光',
  ROSE: '玫瑰星光',
  BLUE: '蔚蓝星光',
}
</script>

<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="min(460px, 100vw)"
    :with-header="false"
    destroy-on-close
    class="memory-detail-drawer"
  >
    <article v-if="memory" class="memory-detail">
      <header class="detail-topline">
        <span>MEMORY / {{ String(memory.id).padStart(4, '0') }}</span>
        <button type="button" aria-label="关闭回忆详情" @click="visible = false">×</button>
      </header>

      <div class="detail-cover">
        <MemoryImage
          v-if="memory.media[0]"
          :file-id="memory.media[0].fileId"
          :alt="memory.title"
        />
        <div v-else class="detail-star">
          <span>✦</span>
          <small>{{ starLabels[memory.starType] || '记忆星光' }}</small>
        </div>
      </div>

      <div class="detail-heading">
        <p class="detail-date">{{ memory.eventDate }} · {{ starLabels[memory.starType] || '记忆星光' }}</p>
        <h2>{{ memory.title }}</h2>
        <p class="detail-location">{{ memory.locationName || '某个值得纪念的地方' }}</p>
      </div>

      <p class="detail-content">{{ memory.content || '这一刻，无需太多文字。' }}</p>

      <div v-if="memory.media.length > 1" class="detail-media">
        <MemoryImage
          v-for="media in memory.media"
          :key="media.id"
          :file-id="media.fileId"
          :alt="memory.title"
        />
      </div>

      <div v-if="memory.tags.length" class="detail-tags">
        <span v-for="tag in memory.tags" :key="tag"># {{ tag }}</span>
      </div>

      <dl v-if="memory.latitude !== null && memory.longitude !== null" class="detail-coordinates">
        <div>
          <dt>纬度</dt>
          <dd>{{ memory.latitude }}</dd>
        </div>
        <div>
          <dt>经度</dt>
          <dd>{{ memory.longitude }}</dd>
        </div>
      </dl>

      <footer class="detail-footer">
        <p>{{ memory.creatorName }} 记录于 {{ memory.createdAt.slice(0, 10) }}</p>
        <div>
          <el-button @click="emit('edit', memory)">编辑回忆</el-button>
          <el-button class="delete-button" @click="emit('delete', memory)">删除</el-button>
        </div>
      </footer>
    </article>
  </el-drawer>
</template>

<style scoped lang="scss">
:global(.memory-detail-drawer .el-drawer__body) {
  padding: 0;
  background: #080b16;
}

.memory-detail {
  display: flex;
  min-height: 100%;
  padding: 24px 28px 28px;
  flex-direction: column;
  background:
    radial-gradient(circle at 15% 0%, rgba(216, 187, 128, 0.08), transparent 34%),
    radial-gradient(circle at 90% 40%, rgba(79, 92, 148, 0.12), transparent 42%),
    #080b16;
  color: #c4cada;
}

.detail-topline {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
  justify-content: space-between;
  color: #726c61;
  font-size: 9px;
  letter-spacing: 0.18em;
}

.detail-topline button {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 50%;
  background: transparent;
  color: #a4adbf;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.detail-cover {
  position: relative;
  height: 210px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: #0a0e1d;
}

.detail-star {
  display: grid;
  height: 100%;
  place-content: center;
  gap: 12px;
  background:
    radial-gradient(circle at 50% 45%, rgba(216, 187, 128, 0.18), transparent 20%),
    radial-gradient(circle at 50% 45%, rgba(91, 109, 169, 0.14), transparent 45%),
    #090d19;
  text-align: center;
}

.detail-star span {
  color: #e0c58f;
  font-size: 42px;
  text-shadow: 0 0 26px rgba(216, 187, 128, 0.5);
}

.detail-star small {
  color: #7d8499;
  font-size: 9px;
  letter-spacing: 0.18em;
}

.detail-heading {
  margin-top: 24px;
}

.detail-date {
  margin: 0 0 9px;
  color: #a18e6b;
  font-size: 9px;
  letter-spacing: 0.12em;
}

.detail-heading h2 {
  margin: 0;
  color: #eee5d6;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 28px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.detail-location {
  margin: 10px 0 0;
  color: #7f899f;
  font-size: 11px;
}

.detail-content {
  margin: 22px 0;
  color: #a3acbf;
  font-size: 13px;
  line-height: 1.95;
  white-space: pre-wrap;
}

.detail-media {
  display: grid;
  margin: 4px 0 20px;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.detail-media :deep(img),
.detail-media :deep(.image-error) {
  height: 150px;
  border-radius: 9px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.detail-tags span {
  padding: 5px 9px;
  border: 1px solid rgba(216, 187, 128, 0.12);
  border-radius: 6px;
  background: rgba(216, 187, 128, 0.05);
  color: #ad9c7e;
  font-size: 10px;
}

.detail-coordinates {
  display: flex;
  margin: 20px 0 0;
  gap: 28px;
}

.detail-coordinates dt {
  color: #626d85;
  font-size: 9px;
}

.detail-coordinates dd {
  margin: 5px 0 0;
  color: #9aa4b9;
  font-family: Georgia, serif;
  font-size: 12px;
}

.detail-footer {
  margin-top: auto;
  padding-top: 28px;
}

.detail-footer p {
  margin: 0 0 12px;
  color: #626d85;
  font-size: 9px;
}

.detail-footer > div {
  display: flex;
  gap: 10px;
}

.delete-button {
  color: #c98f8f;
}

@media (max-width: 480px) {
  .memory-detail {
    padding: 20px;
  }

  .detail-cover {
    height: 180px;
  }

  .detail-heading h2 {
    font-size: 24px;
  }
}
</style>
