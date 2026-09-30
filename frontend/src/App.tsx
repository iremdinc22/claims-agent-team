import CreateClaimForm from './features/claims/CreateClaimForm'

export default function App() {
  return (
    <main className="app-shell">
      <header className="page-header">
        <p className="eyebrow">Claims Agent Team</p>
        <h1>Report a motor claim</h1>
        <p className="page-intro">
          Provide the incident details below to create a reported claim.
        </p>
      </header>
      <CreateClaimForm />
    </main>
  )
}
