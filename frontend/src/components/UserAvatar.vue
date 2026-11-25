<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { fetchFileBlob } from '@/api/files'

const props = defineProps<{
  fileId: number | null
  name: string
  size?: number
}>()

const imageUrl = ref('')
let loadGeneration = 0

function revoke() {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
}

async function load() {
  const generation = ++loadGeneration
  revoke()
  if (props.fileId === null) return
  try {
    const blob = await fetchFileBlob(props.fileId)
    if (generation !== loadGeneration) return
    imageUrl.value = URL.createObjectURL(blob)
  } catch {
    if (generation === loadGeneration) imageUrl.value = ''
  }
}

watch(() => props.fileId, load, { immediate: true })
onBeforeUnmount(() => {
  loadGeneration += 1
  revoke()
})
</script>

<template>
  <span
    class="user-avatar"
    :style="{ width: `${size || 36}px`, height: `${size || 36}px` }"
    role="img"
    :aria-label="`${name}的头像`"
  >
    <img v-if="imageUrl" :src="imageUrl" alt="" />
    <span v-else aria-hidden="true">{{ name.trim().charAt(0).toUpperCase() || '星' }}</span>
  </span>
</template>

<style scoped>
.user-avatar {
  display: inline-grid;
  flex: none;
  overflow: hidden;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.22);
  border-radius: 50%;
  background: radial-gradient(circle at 30% 25%, #27304b, #101525 70%);
  color: #d8c6a4;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 13px;
}

.user-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
