import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchSystemHealth } from '@/api/system'
import { useSystemStore } from './system'

vi.mock('@/api/system', () => ({
  fetchSystemHealth: vi.fn(),
}))

const mockedFetchHealth = vi.mocked(fetchSystemHealth)

describe('system store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedFetchHealth.mockReset()
  })

  it('loads backend health through the real API contract', async () => {
    mockedFetchHealth.mockResolvedValue({
      status: 'UP',
      service: 'our-galaxy-backend',
      version: '0.1.0',
      scope: 'APPLICATION',
      timestamp: '2026-11-25T00:00:00Z',
    })

    const store = useSystemStore()
    await store.loadHealth()

    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.health?.status).toBe('UP')
    expect(store.health?.service).toBe('our-galaxy-backend')
  })

  it('exposes a retryable error when the backend is unavailable', async () => {
    mockedFetchHealth.mockRejectedValue(new Error('backend unavailable'))
    const store = useSystemStore()

    await store.loadHealth()

    expect(store.loading).toBe(false)
    expect(store.health).toBeNull()
    expect(store.error).toBe('backend unavailable')
  })
})
