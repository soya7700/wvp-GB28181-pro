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
  scheduleDays: string
  startTime: string
  endTime: string
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
  workflowStatus: string
  priority: string
  assigneeId?: number
  occurrenceCount: number
  evidenceUrl?: string
  markedUrl?: string
  createTime: string
  alarmId?: number
}

export interface InspectionReport { taskCount: number; completedCount: number; abnormalCount: number; pendingCount: number; confirmedCount: number; falsePositiveCount: number }
export interface AiModel { id: number; name: string; version: string; capabilities: string; status: string; serviceEndpoint?: string }
export interface AiRule { id: number; name: string; planId?: number; detectionType: string; confidenceThreshold: number; regionPoints?: string; enabled: boolean }
export interface DetectionEffect { detectionType: string; totalCount: number; confirmedCount: number; falsePositiveCount: number; confirmationRate: number }
export interface InspectionAnalytics { daily: Array<{ day: string; taskCount: number; abnormalCount: number; confirmedCount: number }>; topChannels: Array<{ channelId: string; abnormalCount: number; confirmedCount: number }> }
export interface InspectionHealth { migrationReady: boolean; tableCount: number; aiConfigured: boolean; serviceUrl?: string; status: string }
export interface ChannelHealth {
  id: number; deviceId?: string; channelId: string; online: boolean; streamAvailable: boolean
  firstFrameMillis?: number; videoQualityScore: number; recordingComplete: boolean
  healthScore: number; healthStatus: 'HEALTHY' | 'WARNING' | 'CRITICAL'
  diagnostic?: string; snapshotUrl?: string; checkTime: string
}
export interface HealthDashboard {
  total: number; healthy: number; warning: number; critical: number
  averageScore: number; problemChannels: ChannelHealth[]
}

interface PageResult<T> { list?: T[]; items?: T[]; total?: number }

export const queryInspectionPlans = (page = 1, count = 20) => request<PageResult<InspectionPlan>>({ url: '/api/ai/inspection/plans', data: { page, count } })
export const createInspectionPlan = (plan: Omit<InspectionPlan, 'id'>) => request<InspectionPlan, Omit<InspectionPlan, 'id'>>({ url: '/api/ai/inspection/plans', method: 'POST', data: plan })
export const updateInspectionPlan = (plan: InspectionPlan) => request<InspectionPlan, InspectionPlan>({ url: `/api/ai/inspection/plans/${plan.id}`, method: 'PUT', data: plan })
export const toggleInspectionPlan = (id: number, enabled: boolean) => request<InspectionPlan>({ url: `/api/ai/inspection/plans/${id}/enabled?enabled=${enabled}`, method: 'PUT' })
export const copyInspectionPlan = (id: number) => request<InspectionPlan>({ url: `/api/ai/inspection/plans/${id}/copy`, method: 'POST' })
export const deleteInspectionPlan = (id: number) => request<void>({ url: `/api/ai/inspection/plans/${id}`, method: 'DELETE' })
export const queryInspectionTasks = (page = 1, count = 20) => request<PageResult<InspectionTask>>({ url: '/api/ai/inspection/tasks', data: { page, count } })
export const queryInspectionResults = (page = 1, count = 20, status?: string) => request<PageResult<InspectionResult>>({ url: '/api/ai/inspection/results', data: { page, count, status } })
export const runInspectionPlan = (id: number) => request<InspectionTask>({ url: `/api/ai/inspection/plans/${id}/run`, method: 'POST' })
export const reviewInspectionResult = (id: number, status: 'CONFIRMED' | 'FALSE_POSITIVE', note?: string) => request<InspectionResult>({ url: `/api/ai/inspection/results/${id}/review?status=${status}${note ? `&note=${encodeURIComponent(note)}` : ''}`, method: 'POST' })
export const claimInspectionResult = (id: number) => request<InspectionResult>({ url: `/api/ai/inspection/results/${id}/claim`, method: 'POST' })
export const handleInspectionResult = (id: number, status: 'PROCESSING' | 'CLOSED', note?: string) => request<InspectionResult>({ url: `/api/ai/inspection/results/${id}/handle?status=${status}${note ? `&note=${encodeURIComponent(note)}` : ''}`, method: 'POST' })
export const getInspectionReport = (day: string) => request<InspectionReport>({ url: '/api/ai/inspection/report', data: { day } })
export const queryAiModels = () => request<AiModel[]>({ url: '/api/ai/inspection/models' })
export const queryAiRules = () => request<AiRule[]>({ url: '/api/ai/inspection/rules' })
export const queryDetectionEffects = () => request<DetectionEffect[]>({ url: '/api/ai/inspection/effects' })
export const getInspectionAnalytics = (days = 7) => request<InspectionAnalytics>({ url: '/api/ai/inspection/analytics', data: { days } })
export const getInspectionHealth = () => request<InspectionHealth>({ url: '/api/ai/inspection/health' })
export const getChannelHealth = () => request<HealthDashboard>({ url: '/api/ai/inspection/channel-health' })
