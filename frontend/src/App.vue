<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import WarpTransition from '@/components/cosmic/WarpTransition.vue'

const CosmicBackdrop = defineAsyncComponent(() => import('@/components/cosmic/CosmicBackdrop.vue'))
const route = useRoute()
const isHome = computed(() => route.name === 'home')
const isMemories = computed(() => route.name === 'memories')
</script>

<template>
  <div class="app-shell" :class="{ 'is-galaxy': isMemories }">
    <CosmicBackdrop v-if="isHome" />
    <div v-else-if="!isMemories" class="cosmic-background" aria-hidden="true" />
    <AppHeader v-if="!isMemories" />
    <main class="app-main">
      <RouterView />
    </main>
    <footer v-if="!isHome && !isMemories" class="app-footer">
      <span>OUR GALAXY</span>
      <span>只属于两个人的私人宇宙</span>
    </footer>
    <WarpTransition />
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  position: relative;
  min-height: 100svh;
  overflow: hidden;
  background: #070a14;
}

.app-shell.is-galaxy {
  height: 100svh;
  overflow: hidden;
  background: #03050b;
}

.app-shell.is-galaxy .app-main {
  height: 100%;
}

.cosmic-background {
  position: fixed;
  z-index: 0;
  inset: 0;
  background:
    radial-gradient(ellipse at 16% 18%, rgba(58, 62, 112, 0.2), transparent 38%),
    radial-gradient(ellipse at 84% 26%, rgba(34, 64, 105, 0.17), transparent 36%),
    radial-gradient(ellipse at 52% 88%, rgba(77, 51, 91, 0.12), transparent 42%),
    #070a14;
  pointer-events: none;
}

.cosmic-background::after {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 35%, rgba(3, 5, 12, 0.48) 100%);
  content: '';
}

.app-main,
.app-footer {
  position: relative;
  z-index: 1;
}

.app-footer {
  display: flex;
  width: min(1280px, calc(100% - 64px));
  margin: 0 auto;
  padding: 26px 0 30px;
  justify-content: space-between;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: #4d556a;
  font-size: 9px;
  letter-spacing: 0.15em;
}

.app-footer span:first-child {
  color: #666052;
  font-family: Georgia, serif;
  letter-spacing: 0.2em;
}

@media (max-width: 768px) {
  .app-footer {
    width: calc(100% - 40px);
  }
}

@media (max-width: 480px) {
  .app-footer {
    width: calc(100% - 32px);
    padding-block: 22px;
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}
</style>
