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
const labelHost = ref<HTMLDivElement>()
const ready = ref(false)
const error = ref('')
let handle: MemoryGalaxyHandle | null = null

onMounted(() => {
  if (!sceneHost.value || !canvasHost.value || !labelHost.value) return
  try {
    handle = mountMemoryGalaxy(canvasHost.value, labelHost.value, props.memories, {
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
  zoom: (factor: number) => handle?.zoom(factor),
})
</script>

<template>
  <div ref="sceneHost" class="memory-galaxy-scene">
    <div ref="canvasHost" class="memory-scene-canvas" aria-hidden="true" />
    <div ref="labelHost" class="memory-star-labels" />

    <div v-if="!ready && !error" class="scene-loading" role="status">
      <span class="scene-loading-orbit" />
      <strong>正在点亮回忆星系</strong>
      <small>每一颗星星，都在等待与你重逢</small>
    </div>

    <div v-if="error" class="scene-error" role="status">
      <strong>三维星图暂时无法启动</strong>
      <small>你仍可通过右侧回忆索引和新增按钮使用全部功能</small>
    </div>
  </div>
</template>

<style scoped lang="scss">
.memory-galaxy-scene,
.memory-scene-canvas,
.memory-star-labels {
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

.memory-star-labels {
  z-index: 2;
  overflow: hidden;
  pointer-events: none;
}

.memory-star-labels :global(.memory-star-label) {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  max-width: 180px;
  padding: 7px 10px;
  align-items: baseline;
  border: 1px solid rgba(164, 184, 220, 0.12);
  border-radius: 6px;
  background: rgba(7, 10, 20, 0.48);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  color: #b6c0d4;
  gap: 8px;
  text-align: left;
  backdrop-filter: blur(8px);
  cursor: pointer;
  pointer-events: auto;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease, opacity 180ms ease;
}

.memory-star-labels :global(.memory-star-label:hover),
.memory-star-labels :global(.memory-star-label.is-selected) {
  border-color: rgba(216, 187, 128, 0.46);
  background: rgba(16, 17, 28, 0.82);
  color: #eee3cc;
}

.memory-star-labels :global(.memory-star-label[hidden]) {
  display: none;
}

.memory-star-labels :global(.label-title) {
  overflow: hidden;
  font-size: 10px;
  letter-spacing: 0.05em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memory-star-labels :global(.memory-star-label small) {
  flex: none;
  color: #6f7a92;
  font-size: 8px;
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

.scene-loading strong,
.scene-error strong {
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 18px;
  font-weight: 400;
  letter-spacing: 0.1em;
}

.scene-loading small,
.scene-error small {
  color: #747f96;
  font-size: 10px;
}

.scene-loading-orbit {
  width: 46px;
  height: 46px;
  margin-bottom: 8px;
  border: 1px solid rgba(216, 187, 128, 0.18);
  border-top-color: rgba(216, 187, 128, 0.9);
  border-radius: 50%;
  animation: scene-orbit 1.4s linear infinite;
}

.scene-error {
  width: min(360px, calc(100% - 48px));
  padding: 28px;
  border: 1px solid rgba(216, 187, 128, 0.18);
  border-radius: 14px;
  background: rgba(7, 10, 20, 0.72);
  backdrop-filter: blur(16px);
}

@keyframes scene-orbit {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .memory-star-labels :global(.memory-star-label) {
    max-width: 120px;
    padding: 5px 7px;
    gap: 5px;
  }

  .memory-star-labels :global(.memory-star-label small) {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scene-loading-orbit {
    animation: none;
  }

  .memory-star-labels :global(.memory-star-label) {
    transition: none;
  }
}
</style>
