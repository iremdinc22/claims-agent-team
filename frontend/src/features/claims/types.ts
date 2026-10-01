export const INCIDENT_TYPES = [
  'COLLISION',
  'THEFT',
  'GLASS_DAMAGE',
  'OTHER',
] as const

export type IncidentType = (typeof INCIDENT_TYPES)[number]

export interface CreateClaimRequest {
  policyNumber: string
  incidentType: IncidentType
  incidentDate: string
  description: string
}

export interface CreateClaimResponse {
  claimNumber: string
  status: 'REPORTED'
}

export interface ApiErrorResponse {
  code: string
  message: string
  fieldErrors: Record<string, string>
}

export const CLAIM_STATUSES = ['REPORTED', 'PENDING', 'APPROVED', 'REJECTED'] as const
export type ClaimStatus = (typeof CLAIM_STATUSES)[number]

export type ClaimStatusFilter = 'All' | ClaimStatus

export interface ClaimListItem {
  claimNumber: string
  policyNumber: string
  incidentType: IncidentType
  incidentDate: string
  status: ClaimStatus
}

export interface ClaimDetail extends ClaimListItem {
  description: string
}

export interface ClaimListResponse {
  items: ClaimListItem[]
  page: number
  pageSize: 10
  totalItems: number
  totalPages: number
}
