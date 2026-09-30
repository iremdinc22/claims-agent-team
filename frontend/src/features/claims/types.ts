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
