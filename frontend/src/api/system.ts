import { get } from './http'
import type { SystemHealth } from '@/types/api'

export function fetchSystemHealth(): Promise<SystemHealth> {
  return get<SystemHealth>('/system/health')
}
