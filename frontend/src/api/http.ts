import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

export const TOKEN_STORAGE_KEY = 'our_galaxy_access_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
  },
})

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

http.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  try {
    const response = await http.request<ApiResponse<T>>({ ...config, method, url, data })
    const body = response.data

    if (body.code !== 0) {
      throw new ApiError(body.message || '请求失败', response.status)
    }

    return body.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
        clearStoredToken()
      }
      const serverMessage = error.response?.data?.message
      const message = serverMessage || (error.code === 'ECONNABORTED'
        ? '请求超时，请稍后重试'
        : error.response?.status === 401
          ? '登录状态已失效，请重新登录'
          : '暂时无法连接到 Our Galaxy，请稍后重试')
      throw new ApiError(message, error.response?.status)
    }

    throw error
  }
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request<T>('GET', url, undefined, config)
}

export function post<T>(url: string, data?: unknown): Promise<T> {
  return request<T>('POST', url, data)
}

export function upload<T>(url: string, formData: FormData): Promise<T> {
  return request<T>('POST', url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function put<T>(url: string, data?: unknown): Promise<T> {
  return request<T>('PUT', url, data)
}

export function deleteRequest<T>(url: string): Promise<T> {
  return request<T>('DELETE', url)
}
