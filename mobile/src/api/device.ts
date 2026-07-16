import { request } from '@/utils/request'

export interface Device { id?: number; deviceId: string; name?: string; ip?: string; port?: number; status?: boolean | string; channelCount?: number }
export interface Channel { id?: number; deviceId: string; channelDeviceId?: string; deviceDbId?: string; name?: string; status?: boolean | string }
export interface ListResult<T> { list?: T[]; items?: T[]; total?: number }

export const queryDevices = (params: Record<string, unknown>) => request<ListResult<Device>>({ url: '/api/device/query/devices', data: params })
export const queryChannels = (deviceId: string, params: Record<string, unknown>) => request<ListResult<Channel>>({
  url: `/api/device/query/devices/${deviceId}/channels`, data: params,
})
