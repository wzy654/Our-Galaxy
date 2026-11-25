import { get, post } from './http'
import type { CoupleSpace, Invitation } from '@/types/api'

export function fetchCurrentSpace(): Promise<CoupleSpace | null> {
  return get<CoupleSpace | null>('/spaces/current')
}

export function createSpace(input: {
  name: string
  anniversaryDate: string | null
  description: string
}): Promise<CoupleSpace> {
  return post<CoupleSpace>('/spaces', input)
}

export function createInvitation(): Promise<Invitation> {
  return post<Invitation>('/spaces/invitations')
}

export function joinSpace(code: string): Promise<CoupleSpace> {
  return post<CoupleSpace>('/spaces/join', { code })
}
