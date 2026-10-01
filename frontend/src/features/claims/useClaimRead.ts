import { useEffect, useState } from 'react'

type ReadState<T> = { kind: 'loading' } | { kind: 'error' } | { kind: 'success'; data: T }

export function useClaimRead<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [state, setState] = useState<ReadState<T>>({ kind: 'loading' })
  useEffect(() => {
    const controller = new AbortController()
    setState({ kind: 'loading' })
    load(controller.signal).then(
      (data) => { if (!controller.signal.aborted) setState({ kind: 'success', data }) },
      () => { if (!controller.signal.aborted) setState({ kind: 'error' }) },
    )
    return () => controller.abort()
  }, [load])
  return state
}
