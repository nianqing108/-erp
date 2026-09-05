import request from './request'
import type { UserVO } from '@/types'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  realName?: string
  inviteCode?: string
}

export const login = (data: LoginPayload) =>
  request.post<{ token: string }>('/auth/login', data)

export const register = (data: RegisterPayload) => request.post<UserVO>('/auth/register', data)

export const me = () => request.get<UserVO>('/auth/me')
