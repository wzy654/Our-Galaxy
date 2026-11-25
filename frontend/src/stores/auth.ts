import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  clearStoredToken,
  getStoredToken,
  setStoredToken,
} from '@/api/http'
import {
  fetchMe,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  updateProfile as updateProfileRequest,
  type LoginInput,
  type RegisterInput,
} from '@/api/auth'
import type { AuthUser, UserProfileInput } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const user = ref<AuthUser | null>(null)
  const loading = ref(false)
  const initialized = ref(false)
  const error = ref('')
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  async function bootstrap(force = false): Promise<void> {
    if (initialized.value && !force) return
    if (!token.value) {
      initialized.value = true
      return
    }

    loading.value = true
    error.value = ''
    try {
      user.value = await fetchMe()
    } catch (cause) {
      user.value = null
      token.value = null
      clearStoredToken()
      error.value = cause instanceof Error ? cause.message : '登录状态校验失败'
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  async function login(input: LoginInput): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      const result = await loginRequest(input)
      token.value = result.token
      user.value = result.user
      initialized.value = true
      setStoredToken(result.token)
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '登录失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function register(input: RegisterInput): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      await registerRequest(input)
      const result = await loginRequest({ email: input.email, password: input.password })
      token.value = result.token
      user.value = result.user
      initialized.value = true
      setStoredToken(result.token)
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '注册失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(input: UserProfileInput): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      user.value = await updateProfileRequest(input)
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '个人资料保存失败'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) await logoutRequest()
    } finally {
      token.value = null
      user.value = null
      initialized.value = true
      clearStoredToken()
    }
  }

  return {
    token,
    user,
    loading,
    initialized,
    error,
    isAuthenticated,
    bootstrap,
    login,
    register,
    updateProfile,
    logout,
  }
})
