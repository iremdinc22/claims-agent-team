import { useState } from 'react'
import CreateClaimForm from './features/claims/CreateClaimForm'
import ClaimList from './features/claims/ClaimList'
import ClaimDetail from './features/claims/ClaimDetail'
import type { ClaimStatusFilter } from './features/claims/types'

type Screen = { kind: 'list' } | { kind: 'create' } | { kind: 'detail'; claimNumber: string }

export default function App() {
  const [screen, setScreen] = useState<Screen>({ kind: 'list' })
  const [selection, setSelection] = useState<{ page: number; status: ClaimStatusFilter }>({ page: 1, status: 'All' })
  return (
    <main className="app-shell">
      <header className="page-header">
        <p className="eyebrow">Claims Agent Team</p>
        <h1>{screen.kind === 'list' ? 'Your claims' : screen.kind === 'detail' ? 'Claim details' : 'Report a motor claim'}</h1>
        {screen.kind === 'create' && <p className="page-intro">Provide the incident details below to create a reported claim.</p>}
        <div className="screen-navigation">
          {screen.kind === 'list' ? <button onClick={() => setScreen({ kind: 'create' })}>Report a claim</button> : <button onClick={() => setScreen({ kind: 'list' })}>Back to list</button>}
        </div>
      </header>
      {screen.kind === 'list' && <ClaimList
        key={`${selection.page}:${selection.status}`}
        page={selection.page}
        status={selection.status}
        onPage={(page) => setSelection((current) => ({ ...current, page }))}
        onStatus={(status) => setSelection((current) => current.status === status ? current : { page: 1, status })}
        onSelect={(claimNumber) => setScreen({ kind: 'detail', claimNumber })}
      />}
      {screen.kind === 'detail' && <ClaimDetail key={screen.claimNumber} claimNumber={screen.claimNumber} />}
      {screen.kind === 'create' && <CreateClaimForm />}
    </main>
  )
}
