import { useEffect, useMemo, useState } from 'react'
import { authService } from '../services/authService'
import { drawService, type Draw, type DrawResult } from '../services/drawService'

// Drawing-focused page: shows current draw state and lets eligible participants draw exactly once.
// Routing is not wired yet; we read ?id=... or #id for the draw identifier (same pattern as DrawDetailPage).

function useAppName(): string {
  return useMemo(() => {
    const name = (import.meta as any)?.env?.VITE_APP_NAME as string | undefined
    return name || 'Name Draw'
  }, [])
}

function useDrawId(): string | null {
  const [id, setId] = useState<string | null>(null)
  useEffect(() => {
    function parse() {
      const url = new URL(window.location.href)
      const qId = url.searchParams.get('id')
      if (qId) return setId(qId)
      const hash = window.location.hash?.replace(/^#/, '')
      if (hash) return setId(hash)
      setId(null)
    }
    parse()
    window.addEventListener('hashchange', parse)
    window.addEventListener('popstate', parse)
    return () => {
      window.removeEventListener('hashchange', parse)
      window.removeEventListener('popstate', parse)
    }
  }, [])
  return id
}

export default function DrawingPage() {
  const appName = useAppName()
  const drawId = useDrawId()
  const isAuthed = authService.isAuthenticated()
  const user = authService.getUser()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [draw, setDraw] = useState<Draw | null>(null)
  const [myResult, setMyResult] = useState<DrawResult | null>(null)
  const [drawing, setDrawing] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!drawId || !isAuthed) return
      setLoading(true)
      setError(null)
      setMyResult(null)
      try {
        const d = await drawService.getDraw(drawId)
        if (cancelled) return
        setDraw(d)
        // Try to fetch my result if already drawn
        try {
          const mr = await drawService.getMyResult(d.id)
          if (!cancelled) setMyResult(mr)
        } catch {}
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Failed to load draw')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [drawId, isAuthed])

  const canDraw = !!draw && draw.state === 'OPEN' && draw.canDraw && !myResult
  const isCreator = !!draw && draw.creator?.id === user?.id

  async function handleDraw() {
    if (!draw) return
    setDrawing(true)
    setError(null)
    try {
      const result = await drawService.performDraw(draw.id)
      setMyResult(result)
      // Refresh draw to update eligibility flags
      const updated = await drawService.getDraw(draw.id)
      setDraw(updated)
    } catch (e: any) {
      // Per FR-034/035, surface a clear error if non-self could not be found
      setError(
        e?.message ||
          'Unable to find a non-self name after several attempts. Please try again later.'
      )
    } finally {
      setDrawing(false)
    }
  }

  const onLogin = (provider: 'google' | 'facebook') => authService.initiateLogin(provider)
  const onLogout = async () => {
    await authService.logout()
    window.location.reload()
  }

  return (
    <div style={{ maxWidth: 720, margin: '24px auto', padding: '0 16px' }}>
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
          <p>Please log in to draw a name.</p>
        </div>
      )}

      {isAuthed && !drawId && (
        <div style={{ padding: 16, border: '1px solid #e5e7eb', borderRadius: 8 }}>
          <p>No draw selected. Provide an id via query string (?id=...) or hash (#id).</p>
        </div>
      )}

      {isAuthed && drawId && (
        <section>
          {loading && <p>Loading…</p>}
          {error && <p style={{ color: '#b91c1c' }}>{error}</p>}
          {!loading && !error && draw && (
            <div style={{ display: 'grid', gap: 16 }}>
              <div style={{ padding: 16, border: '1px solid #e5e7eb', borderRadius: 8 }}>
                <h2 style={{ marginTop: 0 }}>{draw.title}</h2>
                {draw.description && (
                  <p style={{ margin: '0 0 8px 0', color: '#374151' }}>{draw.description}</p>
                )}
                <p style={{ margin: 0, color: '#6b7280' }}>
                  State: <strong>{draw.state}</strong> · Participants: <strong>{draw.participantCount}/{draw.maxParticipants}</strong> · Draw Date: <strong>{draw.drawDate}</strong>
                </p>
              </div>

              {isCreator && (
                <div style={{ padding: 12, border: '1px solid #f59e0b', background: '#fffbeb', borderRadius: 8 }}>
                  <strong>Organizer note:</strong> Organizers cannot draw; use the Draw Detail page to manage the event.
                </div>
              )}

              {!myResult && !isCreator && draw.state !== 'OPEN' && (
                <div style={{ padding: 12, border: '1px solid #e5e7eb', borderRadius: 8 }}>
                  This draw is not open yet. Please check back later.
                </div>
              )}

              {!isCreator && (
                <div style={{ padding: 16, border: '1px solid #e5e7eb', borderRadius: 8 }}>
                  <button
                    onClick={handleDraw}
                    disabled={!canDraw || drawing}
                    style={buttonStyle('#10b981')}
                  >
                    {drawing ? 'Drawing…' : myResult ? 'Already drawn' : 'Draw Name'}
                  </button>
                  {!canDraw && !myResult && draw.state === 'OPEN' && (
                    <p style={{ color: '#6b7280', marginTop: 8 }}>
                      You may not be eligible to draw yet. Ensure you joined this draw and haven't already drawn.
                    </p>
                  )}
                </div>
              )}

              {myResult && (
                <div style={{ padding: 16, border: '1px solid #10b981', borderRadius: 8, background: '#ecfdf5' }}>
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>Your Draw</div>
                  <div>You drew: <strong>{myResult.drawnUser.name}</strong></div>
                  <div style={{ color: '#6b7280', fontSize: 12 }}>at {new Date(myResult.drawnAt).toLocaleString()}</div>
                </div>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  )
}

function buttonStyle(bg: string): React.CSSProperties {
  return {
    appearance: 'none',
    border: 'none',
    cursor: 'pointer',
    borderRadius: 8,
    padding: '10px 14px',
    color: 'white',
    background: bg,
    fontSize: 16,
    fontWeight: 600,
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
  }
}
