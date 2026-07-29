import { request } from '@/utils/request'

export interface Device { id?: number; deviceId: string; name?: string; ip?: string; port?: number; status?: boolean | string; channelCount?: number }
export interface Channel {
  id?: number
  /** 后端 DeviceChannel.deviceId，实际表示国标通道编号 */
  deviceId: string
  name?: string
  status?: boolean | string
  streamId?: string
  ptzType?: number
  ptzTypeText?: string
  hasAudio?: boolean
}
export interface ListResult<T> { list?: T[]; items?: T[]; total?: number }

export const queryDevices = (params: Record<string, unknown>) => request<ListResult<Device>>({ url: '/api/device/query/devices', data: params })
export const queryChannels = (deviceId: string, params: Record<string, unknown>) => request<ListResult<Channel>>({
  url: `/api/device/query/devices/${deviceId}/channels`, data: params,
})
