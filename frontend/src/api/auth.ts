import { get, post, put } from './http'
import type { AuthUser, TokenResponse, UserProfileInput } from '@/types/api'

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput extends LoginInput {
  nickname: string
}

export function login(input: LoginInput): Promise<TokenResponse> {
  return post<TokenResponse>('/auth/login', input)
}

export function register(input: RegisterInput): Promise<AuthUser> {
  return post<AuthUser>('/auth/register', input)
}

export function fetchMe(): Promise<AuthUser> {
  return get<AuthUser>('/auth/me')
}

export function updateProfile(input: UserProfileInput): Promise<AuthUser> {
  return put<AuthUser>('/users/me', input)
}

export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}
