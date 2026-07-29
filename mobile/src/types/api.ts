export interface ApiResponse<T = unknown> {
  code?: number
  msg?: string
  data: T
}

export interface LoginResult {
  accessToken: string
  username: string
  serverId: string
}
