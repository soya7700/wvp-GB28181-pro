import { request } from '@/utils/request'
import type { StreamInfo } from './play'

export interface RecordItem { startTime?: string; endTime?: string; name?: string; type?: string; filePath?: string }
export interface RecordResult { recordList?: RecordItem[]; list?: RecordItem[]; sumNum?: number }
export const queryGbRecords = (deviceId: string, channelId: string, startTime: string, endTime: string) => request<RecordResult>({
  url: `/api/gb_record/query/${deviceId}/${channelId}`, data: { startTime, endTime },
})
export const startPlayback = (deviceId: string, channelId: string, startTime: string, endTime: string) => request<StreamInfo>({ url: `/api/playback/start/${deviceId}/${channelId}`, data: { startTime, endTime } })
export const stopPlayback = (deviceId: string, channelId: string, stream: string) => request<void>({ url: `/api/playback/stop/${deviceId}/${channelId}/${stream}` })
export const pausePlayback = (stream: string) => request<void>({ url: `/api/playback/pause/${stream}` })
export const resumePlayback = (stream: string) => request<void>({ url: `/api/playback/resume/${stream}` })
export const speedPlayback = (stream: string, speed: number) => request<void>({ url: `/api/playback/speed/${stream}/${speed}` })
