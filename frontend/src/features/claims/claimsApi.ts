import type {
  ApiErrorResponse,
  CreateClaimRequest,
  CreateClaimResponse,
} from './types'

export class ClaimsApiError extends Error {
  constructor(
    readonly status: number,
    readonly response: ApiErrorResponse | null,
  ) {
    super(response?.message ?? 'Claim creation failed')
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
