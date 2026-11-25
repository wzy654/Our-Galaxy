<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { mountCosmicScene } from '@/utils/cosmicScene'

const canvasHost = ref<HTMLDivElement | null>(null)
const canvasReady = ref(false)
let cleanupScene: (() => void) | null = null

onMounted(() => {
  if (!canvasHost.value) return
  cleanupScene = mountCosmicScene(canvasHost.value, () => {
    canvasReady.value = true
  })
})

onBeforeUnmount(() => {
  cleanupScene?.()
  cleanupScene = null
})
</script>

<template>
  <div class="cosmic-backdrop" aria-hidden="true">
    <div class="cosmic-fallback" />
    <div ref="canvasHost" class="cosmic-canvas" :class="{ 'is-ready': canvasReady }" />
  </div>
</template>

<style scoped>
.cosmic-backdrop {
  position: fixed;
  z-index: 0;
  inset: 0;
  overflow: hidden;
  background: #070a15;
  pointer-events: none;
}

.cosmic-fallback {
  position: absolute;
  z-index: 0;
  inset: 0;
  background:
    radial-gradient(ellipse at 18% 24%, rgba(49, 67, 119, 0.2), transparent 43%),
    radial-gradient(ellipse at 82% 18%, rgba(55, 48, 104, 0.17), transparent 42%),
    radial-gradient(ellipse at 50% 82%, rgba(71, 49, 93, 0.11), transparent 45%),
    radial-gradient(ellipse at 28% 74%, rgba(116, 82, 43, 0.045), transparent 28%),
    #070a15;
}

.cosmic-canvas {
  position: absolute;
  z-index: 1;
  inset: 0;
  overflow: hidden;
  opacity: 0;
  transition: opacity 1800ms ease;
  pointer-events: none;
}

.cosmic-canvas.is-ready {
  opacity: 1;
}

.cosmic-canvas canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.cosmic-backdrop::after {
  position: absolute;
  z-index: 2;
  inset: 0;
  background:
    radial-gradient(ellipse at center, transparent 34%, rgba(2, 4, 12, 0.18) 70%, rgba(2, 4, 12, 0.68) 118%),
    linear-gradient(180deg, rgba(3, 5, 14, 0.12), transparent 28%, transparent 76%, rgba(3, 5, 14, 0.2));
  content: '';
  pointer-events: none;
}

@media (prefers-reduced-motion: reduce) {
  .cosmic-canvas {
    transition: none;
  }
}
</style>
