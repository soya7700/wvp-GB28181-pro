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
