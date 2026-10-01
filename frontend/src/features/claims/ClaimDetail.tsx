import { useCallback } from 'react'
import { getClaim } from './claimsApi'
import { useClaimRead } from './useClaimRead'

export default function ClaimDetail({ claimNumber }: { claimNumber: string }) {
  const load = useCallback((signal: AbortSignal) => getClaim(claimNumber, signal), [claimNumber])
  const state = useClaimRead(load)
  return <section className="claim-card read-card" aria-label="Claim detail">
    {state.kind === 'loading' && <p role="status">Loading claim…</p>}
    {state.kind === 'error' && <p role="alert">This claim is unavailable or could not be loaded. Please return to the list.</p>}
    {state.kind === 'success' && <dl className="claim-detail">
      <dt>Claim number</dt><dd>{state.data.claimNumber}</dd>
      <dt>Policy number</dt><dd>{state.data.policyNumber}</dd>
      <dt>Incident type</dt><dd>{state.data.incidentType}</dd>
      <dt>Incident date</dt><dd>{state.data.incidentDate}</dd>
      <dt>Description</dt><dd className="claim-description">{state.data.description}</dd>
      <dt>Status</dt><dd>{state.data.status}</dd>
    </dl>}
  </section>
}
