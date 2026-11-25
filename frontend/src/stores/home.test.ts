import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchMemories } from '@/api/memories'
import { fetchCurrentSpace } from '@/api/spaces'
import { useHomeStore } from './home'

vi.mock('@/api/memories', () => ({ fetchMemories: vi.fn() }))
vi.mock('@/api/spaces', () => ({ fetchCurrentSpace: vi.fn() }))

const mockedFetchMemories = vi.mocked(fetchMemories)
const mockedFetchSpace = vi.mocked(fetchCurrentSpace)

const space = {
  id: 1,
  name: '两个人的宇宙',
  anniversaryDate: '2026-01-01',
  description: null,
  role: 'OWNER' as const,
  memberCount: 2,
  members: [],
}

describe('home archive store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedFetchMemories.mockReset()
    mockedFetchSpace.mockReset()
  })

  it('loads the real space and counts real memories', async () => {
    mockedFetchSpace.mockResolvedValue(space)
    mockedFetchMemories.mockResolvedValue([{ id: 1 }, { id: 2 }] as Awaited<ReturnType<typeof fetchMemories>>)

    const store = useHomeStore()
    await store.load()

    expect(store.hasSpace).toBe(true)
    expect(store.memoryCount).toBe(2)
    expect(store.error).toBe('')
  })

  it('does not request memories before a space exists', async () => {
    mockedFetchSpace.mockResolvedValue(null)

    const store = useHomeStore()
    await store.load()

    expect(store.hasSpace).toBe(false)
    expect(store.memoryCount).toBeNull()
    expect(mockedFetchMemories).not.toHaveBeenCalled()
  })

  it('clears archive data when authentication is lost', async () => {
    mockedFetchSpace.mockResolvedValue(space)
    mockedFetchMemories.mockResolvedValue([])

    const store = useHomeStore()
    await store.load()
    store.reset()

    expect(store.space).toBeNull()
    expect(store.memoryCount).toBeNull()
    expect(store.daysTogether).toBeNull()
  })

  it('ignores a pending response after reset', async () => {
    let resolveSpace!: (value: typeof space) => void
    mockedFetchSpace.mockReturnValue(new Promise((resolve) => {
      resolveSpace = resolve
    }))

    const store = useHomeStore()
    const pending = store.load()
    store.reset()
    resolveSpace(space)
    await pending

    expect(store.space).toBeNull()
    expect(store.loading).toBe(false)
    expect(mockedFetchMemories).not.toHaveBeenCalled()
  })

  it('exposes a retryable error without fake archive values', async () => {
    mockedFetchSpace.mockRejectedValue(new Error('读取失败'))

    const store = useHomeStore()
    await store.load()

    expect(store.error).toBe('读取失败')
    expect(store.hasSpace).toBe(false)
    expect(store.memoryCount).toBeNull()
  })

  it('keeps a loaded space when only the memory request fails', async () => {
    mockedFetchSpace.mockResolvedValue(space)
    mockedFetchMemories.mockRejectedValue(new Error('回忆读取失败'))

    const store = useHomeStore()
    await store.load()

    expect(store.hasSpace).toBe(true)
    expect(store.memoryCount).toBeNull()
    expect(store.error).toBe('回忆读取失败')
  })
})
