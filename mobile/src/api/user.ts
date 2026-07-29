import type { LoginResult } from '@/types/api'
import { request } from '@/utils/request'

export const login = (username: string, password: string) => request<LoginResult>({
  url: '/api/user/login', data: { username, password }, skipAuth: true,
})
export const logout = () => request<void>({ url: '/api/user/logout' })
