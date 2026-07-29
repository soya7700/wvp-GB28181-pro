import { request } from '@/utils/request'

export interface StreamInfo {
  app?: string; stream?: string; mediaServerId?: string; hls?: string; https_hls?: string
  rtc?: string; rtcs?: string; ws_flv?: string; wss_flv?: string; flv?: string; https_flv?: string
}
export const startPlay = (deviceId: string, channelId: string) => request<StreamInfo>({ url: `/api/play/start/${deviceId}/${channelId}` })
export const stopPlay = (deviceId: string, channelId: string) => request<void>({ url: `/api/play/stop/${deviceId}/${channelId}` })
export const chooseH5Stream = (stream: StreamInfo) => {
  const secure = typeof location !== 'undefined' && location.protocol === 'https:'
  return secure ? stream.https_hls || stream.rtcs || stream.wss_flv || '' : stream.hls || stream.rtc || stream.ws_flv || stream.https_hls || ''
}
