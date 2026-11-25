<script setup lang="ts">
import { onMounted, watch } from 'vue'
import ArchiveSection from '@/components/home/ArchiveSection.vue'
import HeroSection from '@/components/home/HeroSection.vue'
import { useAuthStore } from '@/stores/auth'
import { useHomeStore } from '@/stores/home'

const auth = useAuthStore()
const home = useHomeStore()

async function syncArchive() {
  if (auth.isAuthenticated) {
    await home.load()
  } else {
    home.reset()
  }
}

onMounted(async () => {
  await auth.bootstrap()
  await syncArchive()
})

watch(() => auth.isAuthenticated, () => {
  void syncArchive()
})
</script>

<template>
  <section class="home-view">
    <HeroSection :authenticated="auth.isAuthenticated" />
    <ArchiveSection
      :authenticated="auth.isAuthenticated"
      :space-name="home.space?.name ?? null"
      :has-space="home.hasSpace"
      :has-anniversary="home.hasAnniversary"
      :days-together="home.daysTogether"
      :memory-count="home.memoryCount"
      :next-anniversary-days="home.nextAnniversaryDays"
      :loading="home.loading"
      :error="home.error"
      @retry="home.load"
    />
  </section>
</template>

<style scoped lang="scss">
.home-view {
  position: relative;
  width: min(1280px, calc(100% - 64px));
  margin: 0 auto;
}

@media (max-width: 768px) {
  .home-view {
    width: calc(100% - 40px);
  }
}

@media (max-width: 480px) {
  .home-view {
    width: calc(100% - 32px);
  }
}
</style>
