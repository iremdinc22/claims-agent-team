import { useCallback, useState } from 'react'
import { listClaims } from './claimsApi'
import { useClaimRead } from './useClaimRead'
import { CLAIM_STATUSES, type ClaimStatusFilter } from './types'

interface Props {
  page: number
  status: ClaimStatusFilter
  onStatus: (status: ClaimStatusFilter) => void
  onPage: (page: number) => void
  onSelect: (claimNumber: string) => void
}

export default function ClaimList({ page, status, onStatus, onPage, onSelect }: Props) {
  const [attempt, setAttempt] = useState(0)
  const load = useCallback((signal: AbortSignal) => listClaims(page, signal, status === 'All' ? undefined : status), [page, status, attempt])
  const state = useClaimRead(load)
  return (
    <section className="claim-card read-card" aria-label="Your claims">
      <div className="status-filter">
        <label htmlFor="claim-status-filter">Status</label>
        <select id="claim-status-filter" value={status} onChange={(event) => onStatus(event.target.value as ClaimStatusFilter)}>
          <option value="All">All</option>
          {CLAIM_STATUSES.map((value) => <option key={value} value={value}>{value}</option>)}
        </select>
      </div>
      {state.kind === 'loading' && <p role="status">Loading claims…</p>}
      {state.kind === 'error' && <div role="alert"><p>We could not load your claims. Please try again.</p><button onClick={() => setAttempt((value) => value + 1)}>Retry</button></div>}
      {state.kind === 'success' && (state.data.totalItems === 0 ? <p role="status">{status === 'All' ? 'You have no claims yet.' : 'No claims found'}</p> : <>
        {state.data.items.length === 0 ? <div role="status"><p>This page is no longer available.</p><button onClick={() => onPage(1)}>Go to first page</button></div> :
          <div className="table-scroll"><table>
            <caption>Your claims, newest incident first</caption>
            <thead><tr><th scope="col">Claim number</th><th scope="col">Policy number</th><th scope="col">Incident type</th><th scope="col">Incident date</th><th scope="col">Status</th></tr></thead>
            <tbody>{state.data.items.map((claim) => <tr key={claim.claimNumber}>
              <td><button className="claim-link" onClick={() => onSelect(claim.claimNumber)}>{claim.claimNumber}</button></td>
              <td>{claim.policyNumber}</td><td>{claim.incidentType}</td><td>{claim.incidentDate}</td><td>{claim.status}</td>
            </tr>)}</tbody>
          </table></div>}
        {state.data.items.length > 0 && <nav className="pagination" aria-label="Claim pages">
          <button disabled={page <= 1} onClick={() => onPage(page - 1)}>Previous</button>
          <span>Page {page} of {state.data.totalPages} · {state.data.totalItems} claims</span>
          <button disabled={page >= state.data.totalPages} onClick={() => onPage(page + 1)}>Next</button>
        </nav>}
      </>)}
    </section>
  )
}
