import type { ApiResponse } from '@/types/api'
import { storage } from './storage'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export interface RequestOptions<TBody = unknown> {
  url: string
  method?: HttpMethod
  data?: TBody
  header?: Record<string, string>
  skipAuth?: boolean
}

const baseURL = import.meta.env.VITE_API_BASE_URL || ''
let redirecting = false

function redirectToLogin() {
  storage.clearAuth()
  if (redirecting) return
  redirecting = true
  uni.reLaunch({ url: '/pages/login/index', complete: () => { redirecting = false } })
}

export function request<T = unknown, TBody = unknown>(options: RequestOptions<TBody>) {
  const token = storage.get('token')
  const requestData = options.data && typeof options.data === 'object' && !Array.isArray(options.data)
    ? Object.fromEntries(Object.entries(options.data as Record<string, unknown>).filter(([, value]) => value !== '' && value !== null && value !== undefined))
    : options.data
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${baseURL}${options.url}`,
      method: options.method || 'GET',
      data: requestData as UniApp.RequestOptions['data'],
      timeout: 30000,
      header: { ...(token && !options.skipAuth ? { 'access-token': token } : {}), ...options.header },
      success(response: UniApp.RequestSuccessCallbackResult) {
        if (response.statusCode === 401) {
          redirectToLogin()
          reject(new Error('登录已过期'))
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`请求失败（${response.statusCode}）`))
          return
        }
        const raw = response.data as ApiResponse<T> | T
        if (raw && typeof raw === 'object' && 'code' in raw) {
          const result = raw as ApiResponse<T>
          if (result.code && result.code !== 0) {
            reject(new Error(result.msg || '业务请求失败'))
            return
          }
          resolve(result.data as T)
          return
        }
        resolve(raw as T)
      },
      fail(error: UniApp.GeneralCallbackResult) { reject(new Error(error.errMsg || '网络连接失败')) },
    })
  })
}

export function showRequestError(error: unknown) {
  uni.showToast({ title: error instanceof Error ? error.message : '请求失败', icon: 'none' })
}
