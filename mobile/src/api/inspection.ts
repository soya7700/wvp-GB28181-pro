import { request } from '@/utils/request'

export type InspectionCapabilityStatus = 'AVAILABLE' | 'PLANNED' | 'WAITING'

export interface InspectionCapability {
  code: string
  name: string
  status: InspectionCapabilityStatus
  description: string
}

export interface InspectionOverview {
  enabled: boolean
  serviceStatus: 'READY' | 'NOT_CONFIGURED'
  serviceUrl?: string
  phase: string
  capabilities: InspectionCapability[]
}

export const getInspectionOverview = () => request<InspectionOverview>({ url: '/api/ai/inspection/overview' })

export interface InspectionPlan {
  id: number
  name: string
  enabled: boolean
  intervalMinutes: number
  detectionTypes: string
  channelIds?: string
}

export interface InspectionTask {
  id: number
  planId: number
  planName?: string
  status: string
  channelTotal: number
  successCount: number
  abnormalCount: number
  startTime?: string
  endTime?: string
}

export interface InspectionResult {
  id: number
  taskId: number
  deviceId?: string
  channelId: string
  detectionType: string
  confidence?: number
  status: string
  evidenceUrl?: string
  markedUrl?: string
  createTime: string
}

interface PageResult<T> { list?: T[]; items?: T[]; total?: number }

export const queryInspectionPlans = (page = 1, count = 20) => request<PageResult<InspectionPlan>>({ url: '/api/ai/inspection/plans', data: { page, count } })
export const queryInspectionTasks = (page = 1, count = 20) => request<PageResult<InspectionTask>>({ url: '/api/ai/inspection/tasks', data: { page, count } })
export const queryInspectionResults = (page = 1, count = 20, status?: string) => request<PageResult<InspectionResult>>({ url: '/api/ai/inspection/results', data: { page, count, status } })
export const runInspectionPlan = (id: number) => request<InspectionTask>({ url: `/api/ai/inspection/plans/${id}/run`, method: 'POST' })
