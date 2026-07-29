import { request } from '@/utils/request'

export interface SystemMessage {
  id: number
  type: string
  title: string
  content?: string
  level?: string
  businessType?: string
  businessId?: number
  readFlag: boolean
  readTime?: string
  createTime: string
  alarmStatus?: string
  deviceId?: string
  channelId?: string
}
interface MessagePage { list?: SystemMessage[]; items?: SystemMessage[]; total?: number }

export const queryMessages = (params: Record<string, unknown>) => request<MessagePage>({ url: '/api/message/list', data: params })
export const unreadMessageCount = () => request<number>({ url: '/api/message/unread/count' })
export const readMessage = (id: number) => request<void>({ url: `/api/message/${id}/read`, method: 'POST' })
export const readAllMessages = () => request<void>({ url: '/api/message/read-all', method: 'POST' })
