import { deleteRequest, get, post, put } from './http'
import type { Memory, MemoryInput } from '@/types/api'

export function fetchMemories(): Promise<Memory[]> {
  return get<Memory[]>('/memories')
}

export function fetchMemory(id: number): Promise<Memory> {
  return get<Memory>(`/memories/${id}`)
}

export function createMemory(input: MemoryInput): Promise<Memory> {
  return post<Memory>('/memories', input)
}

export function updateMemory(id: number, input: MemoryInput): Promise<Memory> {
  return put<Memory>(`/memories/${id}`, input)
}

export function deleteMemory(id: number): Promise<void> {
  return deleteRequest<void>(`/memories/${id}`)
}
