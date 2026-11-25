<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { fetchFileBlob } from '@/api/files'

const props = defineProps<{
  fileId: number
  alt: string
}>()

const imageUrl = ref('')
const error = ref('')
let loadGeneration = 0

function revoke() {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
}

async function load() {
  const generation = ++loadGeneration
  revoke()
  error.value = ''
  try {
    const blob = await fetchFileBlob(props.fileId)
    if (generation !== loadGeneration) return
    imageUrl.value = URL.createObjectURL(blob)
  } catch (cause) {
    if (generation === loadGeneration) {
      error.value = cause instanceof Error ? cause.message : '图片加载失败'
    }
  }
}

watch(() => props.fileId, load)
onMounted(load)
onBeforeUnmount(() => {
  loadGeneration += 1
  revoke()
})
</script>

<template>
  <img v-if="imageUrl" :src="imageUrl" :alt="alt" loading="lazy" />
  <span v-else class="image-error">{{ error || '图片加载中…' }}</span>
</template>

<style scoped>
img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-error {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 120px;
  place-items: center;
  color: #7f889f;
  font-size: 11px;
}
</style>
