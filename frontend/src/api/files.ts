import { ApiError, getStoredToken, upload } from './http'
import type { FileInfo } from '@/types/api'

export function uploadImage(file: File): Promise<FileInfo> {
  const formData = new FormData()
  formData.append('file', file)
  return upload<FileInfo>('/files', formData)
}

export async function fetchFileBlob(id: number): Promise<Blob> {
  const token = getStoredToken()
  const response = await fetch(`/api/files/${id}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) {
    let message = '图片加载失败'
    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      message = '图片加载失败'
    }
    throw new ApiError(message, response.status)
  }
  return response.blob()
}
