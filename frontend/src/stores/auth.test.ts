import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  clearStoredToken,
  getStoredToken,
  setStoredToken,
} from '@/api/http'
import {
  fetchMe,
  login,
  logout,
} from '@/api/auth'
import { useAuthStore } from './auth'

vi.mock('@/api/http', () => ({
  clearStoredToken: vi.fn(),
  getStoredToken: vi.fn(() => null),
  setStoredToken: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  fetchMe: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}))

const mockedGetToken = vi.mocked(getStoredToken)
const mockedSetToken = vi.mocked(setStoredToken)
const mockedClearToken = vi.mocked(clearStoredToken)
const mockedLogin = vi.mocked(login)
const mockedLogout = vi.mocked(logout)
const mockedFetchMe = vi.mocked(fetchMe)

const user = {
  id: 1,
  email: 'star@example.com',
  nickname: '小星',
  avatarFileId: null,
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockedGetToken.mockReturnValue(null)
  })

  it('stores the access token and authenticated user after login', async () => {
    mockedLogin.mockResolvedValue({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-11-25T01:00:00Z',
      user,
    })

    const store = useAuthStore()
    await store.login({ email: user.email, password: 'TestPassword123!' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.user).toEqual(user)
    expect(mockedSetToken).toHaveBeenCalledWith('jwt-token')
  })

  it('clears local authentication after logout', async () => {
    mockedLogin.mockResolvedValue({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-11-25T01:00:00Z',
      user,
    })
    mockedLogout.mockResolvedValue(undefined)

    const store = useAuthStore()
    await store.login({ email: user.email, password: 'TestPassword123!' })
    await store.logout()

    expect(mockedLogout).toHaveBeenCalledOnce()
    expect(store.isAuthenticated).toBe(false)
    expect(mockedClearToken).toHaveBeenCalledOnce()
  })

  it('drops an invalid persisted token during bootstrap', async () => {
    mockedGetToken.mockReturnValue('expired-token')
    mockedFetchMe.mockRejectedValue(new Error('登录状态已失效'))

    const store = useAuthStore()
    await store.bootstrap()

    expect(store.initialized).toBe(true)
    expect(store.isAuthenticated).toBe(false)
    expect(store.error).toBe('登录状态已失效')
    expect(mockedClearToken).toHaveBeenCalledOnce()
  })
})
