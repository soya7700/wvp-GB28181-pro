import { request } from '@/utils/request'

export type PtzCommand = 'left' | 'right' | 'up' | 'down' | 'upleft' | 'upright' | 'downleft' | 'downright' | 'zoomin' | 'zoomout' | 'stop'
export interface Preset { presetId: number; presetName?: string }

export const controlPtz = (deviceId: string, channelId: string, command: PtzCommand) => request<void>({
  url: `/api/front-end/ptz/${deviceId}/${channelId}`,
  data: { command, horizonSpeed: 100, verticalSpeed: 100, zoomSpeed: 10 },
})
export const queryPresets = (deviceId: string, channelId: string) => request<Preset[]>({ url: `/api/front-end/preset/query/${deviceId}/${channelId}` })
export const callPreset = (deviceId: string, channelId: string, presetId: number) => request<void>({
  url: `/api/front-end/preset/call/${deviceId}/${channelId}`, data: { presetId },
})
