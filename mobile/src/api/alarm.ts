import { request } from '@/utils/request'

export interface AlarmItem {
  id?: number
  deviceId?: string
  channelId?: string
  alarmPriority?: string
  alarmMethod?: string
  alarmType?: string
  alarmTime?: string
  alarmDescription?: string
  longitude?: number
  latitude?: number
  handlingStatus?: 'PENDING' | 'ACKNOWLEDGED' | 'RESOLVED' | 'FALSE_ALARM'
  handledByName?: string
  handledAt?: string
  handlingNote?: string
}
interface AlarmPage { list?: AlarmItem[]; items?: AlarmItem[]; total?: number }

export const queryAlarms = (params: Record<string, unknown>) => request<AlarmPage>({ url: '/api/alarm/all', data: params })
export const getAlarm = (id: number) => request<AlarmItem>({ url: `/api/alarm/${id}` })
export const handleAlarm = (id: number, status: 'ACKNOWLEDGED' | 'RESOLVED' | 'FALSE_ALARM', note?: string) => request<AlarmItem>({
  url: `/api/alarm/${id}/handle`, method: 'POST', data: { status, note },
})
