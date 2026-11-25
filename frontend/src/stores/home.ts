import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMemories } from '@/api/memories'
import { fetchCurrentSpace } from '@/api/spaces'
import type { CoupleSpace } from '@/types/api'
import { getDaysTogether, getNextAnniversaryDays } from '@/utils/anniversary'

export const useHomeStore = defineStore('home', () => {
  const space = ref<CoupleSpace | null>(null)
  const memoryCount = ref<number | null>(null)
  const loading = ref(false)
  const error = ref('')
  let requestId = 0

  const hasSpace = computed(() => space.value !== null)
  const hasAnniversary = computed(() => Boolean(space.value?.anniversaryDate))
  const daysTogether = computed(() => getDaysTogether(space.value?.anniversaryDate ?? null))
  const nextAnniversaryDays = computed(() => getNextAnniversaryDays(space.value?.anniversaryDate ?? null))

  async function load() {
    if (loading.value) return
    const currentRequest = ++requestId
    space.value = null
    memoryCount.value = null
    error.value = ''
    loading.value = true

    try {
      const currentSpace = await fetchCurrentSpace()
      if (currentRequest !== requestId) return
      space.value = currentSpace

      if (currentSpace) {
        const memories = await fetchMemories()
        if (currentRequest !== requestId) return
        memoryCount.value = memories.length
      }
    } catch (reason) {
      if (currentRequest !== requestId) return
      memoryCount.value = null
      error.value = reason instanceof Error ? reason.message : '宇宙档案暂时无法读取'
    } finally {
      if (currentRequest === requestId) loading.value = false
    }
  }

  function reset() {
    requestId += 1
    space.value = null
    memoryCount.value = null
    loading.value = false
    error.value = ''
  }

  return {
    space,
    memoryCount,
    loading,
    error,
    hasSpace,
    hasAnniversary,
    daysTogether,
    nextAnniversaryDays,
    load,
    reset,
  }
})
