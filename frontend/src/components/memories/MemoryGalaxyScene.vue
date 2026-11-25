<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { mountMemoryGalaxy, type MemoryGalaxyHandle } from '@/utils/memoryGalaxyScene'
import { fetchFileBlob } from '@/api/files'
import type { Memory } from '@/types/api'

const props = defineProps<{
  memories: Memory[]
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [memoryId: number]
}>()

const sceneHost = ref<HTMLDivElement>()
const canvasHost = ref<HTMLDivElement>()
const ready = ref(false)
const error = ref('')
let handle: MemoryGalaxyHandle | null = null

onMounted(() => {
  if (!sceneHost.value || !canvasHost.value) return
  try {
    handle = mountMemoryGalaxy(canvasHost.value, props.memories, {
      onSelect: (memoryId) => emit('select', memoryId),
      loadImage: fetchFileBlob,
      onReady: () => {
        ready.value = true
      },
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '三维星图初始化失败'
  }
})

watch(
  () => props.memories,
  (memories) => handle?.updateMemories(memories),
)

watch(
  () => props.selectedId,
  (memoryId) => handle?.setSelected(memoryId),
)

onBeforeUnmount(() => {
  handle?.dispose()
  handle = null
})

defineExpose({
  resetView: () => handle?.resetView(),
})
</script>

<template>
  <div ref="sceneHost" class="memory-galaxy-scene">
    <div ref="canvasHost" class="memory-scene-canvas" aria-hidden="true" />

    <div v-if="!ready && !error" class="scene-loading" role="status">
      <span class="scene-loading-orbit" />
    </div>

    <div v-if="error" class="scene-error" role="status">
      <strong>星海暂时无法展开</strong>
      <small>请刷新页面后重试</small>
    </div>
  </div>
</template>

<style scoped lang="scss">
.memory-galaxy-scene,
.memory-scene-canvas {
  position: absolute;
  inset: 0;
}

.memory-scene-canvas :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
  cursor: grab;
  touch-action: none;
}

.memory-scene-canvas :deep(canvas.is-dragging) {
  cursor: grabbing;
}

.scene-loading,
.scene-error {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 4;
  display: flex;
  align-items: center;
  flex-direction: column;
  color: #d9d2c4;
  gap: 10px;
  text-align: center;
  transform: translate(-50%, -50%);
}

.scene-error strong {
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.scene-error small {
  color: #747f96;
  font-size: 10px;
}

.scene-loading-orbit {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(216, 187, 128, 0.14);
  border-top-color: rgba(216, 187, 128, 0.75);
  border-radius: 50%;
  animation: scene-orbit 1.4s linear infinite;
}

@keyframes scene-orbit {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .scene-loading-orbit {
    animation: none;
  }
}
</style>
