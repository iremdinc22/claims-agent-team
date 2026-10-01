import { useState } from 'react'
import CreateClaimForm from './features/claims/CreateClaimForm'
import ClaimList from './features/claims/ClaimList'
import ClaimDetail from './features/claims/ClaimDetail'

type Screen = { kind: 'list' } | { kind: 'create' } | { kind: 'detail'; claimNumber: string }

export default function App() {
  const [screen, setScreen] = useState<Screen>({ kind: 'list' })
  const [page, setPage] = useState(1)
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
      {screen.kind === 'list' && <ClaimList key={page} page={page} onPage={setPage} onSelect={(claimNumber) => setScreen({ kind: 'detail', claimNumber })} />}
      {screen.kind === 'detail' && <ClaimDetail key={screen.claimNumber} claimNumber={screen.claimNumber} />}
      {screen.kind === 'create' && <CreateClaimForm />}
    </main>
  )
}
