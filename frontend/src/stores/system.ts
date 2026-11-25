import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchSystemHealth } from '@/api/system'
import type { SystemHealth } from '@/types/api'

export const useSystemStore = defineStore('system', () => {
  const health = ref<SystemHealth | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadHealth() {
    loading.value = true
    error.value = null

    try {
      health.value = await fetchSystemHealth()
    } catch (reason) {
      error.value = reason instanceof Error ? reason.message : '健康检查失败'
    } finally {
      loading.value = false
    }
  }

  return { health, loading, error, loadHealth }
})
