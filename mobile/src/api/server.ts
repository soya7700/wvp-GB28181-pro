import { request } from '@/utils/request'
export const getSystemInfo = () => request<Record<string, unknown>>({ url: '/api/server/system/info' })
export const getResourceInfo = () => request<Record<string, unknown>>({ url: '/api/server/resource/info' })
