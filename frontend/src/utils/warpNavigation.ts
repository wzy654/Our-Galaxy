import { reactive } from 'vue'
import type { Router } from 'vue-router'

export const warpState = reactive({
  active: false,
  arriving: false,
})

let generation = 0

function wait(duration: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, duration))
}

export async function navigateWithWarp(router: Router, to: string) {
  if (warpState.active) return

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    await router.push(to)
    return
  }

  const currentGeneration = ++generation
  const memoriesChunk = to === '/memories'
    ? import('@/views/memories/MemoriesView.vue').catch(() => undefined)
    : Promise.resolve()

  warpState.active = true
  warpState.arriving = false
  try {
    await wait(900)
    if (currentGeneration !== generation) return
    await memoriesChunk
    if (currentGeneration !== generation) return

    await router.push(to)
    if (currentGeneration !== generation) return

    warpState.arriving = true
    await wait(650)
  } catch {
    if (currentGeneration === generation) cancelWarp()
    return
  } finally {
    if (currentGeneration === generation) {
      warpState.active = false
      warpState.arriving = false
    }
  }
}

export function cancelWarp() {
  generation += 1
  warpState.active = false
  warpState.arriving = false
}
