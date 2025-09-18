import { useEffect, useMemo, useState } from 'react'
import { authService } from '../services/authService'
import { drawService, type Draw, type ListDrawsParams } from '../services/drawService'

function useAppName(): string {
  return useMemo(() => {
    const name = (import.meta as any)?.env?.VITE_APP_NAME as string | undefined
    return name || 'Name Draw'
  }, [])
}

export default function DashboardPage() {
  const appName = useAppName()
  const isAuthed = authService.isAuthenticated()
  const user = authService.getUser()
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const [draws, setDraws] = useState<Draw[]>([])

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!isAuthed) return
      setLoading(true)
      setError(null)
      try {
        const params: ListDrawsParams = { state: 'JOINING', page: 0, size: 10 }
        const page = await drawService.listDraws(params)
        if (!cancelled) setDraws(page.content)
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Failed to load draws')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [isAuthed])

  const onLogout = async () => {
    await authService.logout()
    window.location.reload()
  }

  const onLogin = (provider: 'google' | 'facebook') => {
    authService.initiateLogin(provider)
  }

  return (
    <div style={{ maxWidth: 960, margin: '24px auto', padding: '0 16px' }}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <h1 style={{ margin: 0 }}>{appName}</h1>
        <div>
          {isAuthed ? (
            <>
              <span style={{ marginRight: 12 }}>Hello, <strong>{user?.name || 'User'}</strong></span>
              <button onClick={onLogout}>Logout</button>
            </>
          ) : (
            <>
              <button onClick={() => onLogin('google')} style={{ marginRight: 8 }}>Login with Google</button>
              <button onClick={() => onLogin('facebook')}>Login with Facebook</button>
            </>
          )}
        </div>
      </header>

      {!isAuthed && (
        <div style={{ padding: 16, border: '1px solid #e5e7eb', borderRadius: 8 }}>
          <p>Please log in to view and manage draws.</p>
        </div>
      )}

      {isAuthed && (
        <section>
          <h2 style={{ marginTop: 0 }}>Available Draws</h2>
          {loading && <p>Loading draws…</p>}
          {error && (
            <p style={{ color: '#b91c1c' }}>{error}</p>
          )}
          {!loading && !error && (
            draws.length > 0 ? (
              <ul>
                {draws.map((d) => (
                  <li key={d.id}>
                    <strong>{d.title}</strong> — {d.state} — {d.participantCount}/{d.maxParticipants}
                  </li>
                ))}
              </ul>
            ) : (
              <p>No draws available yet.</p>
            )
          )}
        </section>
      )}
    </div>
  )
}
