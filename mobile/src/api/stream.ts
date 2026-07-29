import { request } from '@/utils/request'

export interface StreamItem { id: number; app?: string; stream?: string; name?: string; pushing?: boolean; pulling?: boolean; status?: boolean }
interface ListResult { list?: StreamItem[]; items?: StreamItem[]; total?: number }
export const queryPushes = (params: Record<string, unknown>) => request<ListResult>({ url: '/api/push/list', data: params })
export const queryProxies = (params: Record<string, unknown>) => request<ListResult>({ url: '/api/proxy/list', data: params })
