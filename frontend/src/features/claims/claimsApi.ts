import type {
  ApiErrorResponse,
  CreateClaimRequest,
  CreateClaimResponse,
  ClaimListItem,
  ClaimDetail,
  ClaimListResponse,
} from './types'
import { CLAIM_STATUSES, INCIDENT_TYPES } from './types'

export class ClaimsApiError extends Error {
  constructor(
    readonly status: number,
    readonly response: ApiErrorResponse | null,
  ) {
    super(response?.message ?? 'Claim request failed')
    this.name = 'ClaimsApiError'
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!isRecord(value) || !isRecord(value.fieldErrors)) {
    return false
  }

  return (
    typeof value.code === 'string' &&
    typeof value.message === 'string' &&
    Object.values(value.fieldErrors).every((message) => typeof message === 'string')
  )
}

function isCreateClaimResponse(value: unknown): value is CreateClaimResponse {
  return (
    isRecord(value) &&
    typeof value.claimNumber === 'string' &&
    value.claimNumber.length > 0 &&
    value.status === 'REPORTED'
  )
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    return null
  }
}

export async function createClaim(
  request: CreateClaimRequest,
): Promise<CreateClaimResponse> {
  const response = await fetch('/api/claims', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  const body = await readJson(response)

  if (response.status !== 201) {
    throw new ClaimsApiError(
      response.status,
      isApiErrorResponse(body) ? body : null,
    )
  }

  if (!isCreateClaimResponse(body)) {
    throw new ClaimsApiError(response.status, null)
  }

  return body
}

function isClaimListItem(value: unknown): value is ClaimListItem {
  if (!isRecord(value)) return false
  const date = value.incidentDate
  return (
    typeof value.claimNumber === 'string' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.claimNumber) &&
    typeof value.policyNumber === 'string' && value.policyNumber.trim().length > 0 &&
    INCIDENT_TYPES.some((type) => type === value.incidentType) &&
    CLAIM_STATUSES.some((status) => status === value.status) &&
    typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date) &&
    !Number.isNaN(Date.parse(date)) && new Date(date).toISOString().slice(0, 10) === date
  )
}

async function readClaimRequest(url: string, signal?: AbortSignal): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(url, { signal })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ClaimsApiError(0, null)
  }
  const body = await readJson(response)
  if (response.status !== 200) {
    throw new ClaimsApiError(response.status, isApiErrorResponse(body) ? body : null)
  }
  return body
}

export async function listClaims(page: number, signal?: AbortSignal): Promise<ClaimListResponse> {
  const body = await readClaimRequest(`/api/claims?page=${page}`, signal)
  if (!isRecord(body) || !Array.isArray(body.items) || !body.items.every(isClaimListItem) ||
      body.page !== page || body.pageSize !== 10 ||
      !Number.isSafeInteger(body.totalItems) || (body.totalItems as number) < 0 ||
      body.totalPages !== Math.ceil((body.totalItems as number) / 10) ||
      body.items.length !== Math.min(10, Math.max(0, (body.totalItems as number) - (page - 1) * 10))) {
    throw new ClaimsApiError(200, null)
  }
  return body as unknown as ClaimListResponse
}

export async function getClaim(claimNumber: string, signal?: AbortSignal): Promise<ClaimDetail> {
  const body = await readClaimRequest(`/api/claims/${encodeURIComponent(claimNumber)}`, signal)
  if (!isClaimListItem(body) || body.claimNumber !== claimNumber ||
      !isRecord(body) || typeof body.description !== 'string' || !body.description.trim()) {
    throw new ClaimsApiError(200, null)
  }
  return body as unknown as ClaimDetail
}
