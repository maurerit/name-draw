import { useMemo, useState } from 'react'
import { authService } from '../services/authService'
import { drawService, type CreateDrawRequest, type Draw } from '../services/drawService'

function useAppName(): string {
  return useMemo(() => {
    const name = (import.meta as any)?.env?.VITE_APP_NAME as string | undefined
    return name || 'Name Draw'
  }, [])
}

export default function CreateDrawPage() {
  const appName = useAppName()
  const isAuthed = authService.isAuthenticated()
  const user = authService.getUser()

  const today = useMemo(() => new Date(), [])
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  const todayStr = `${yyyy}-${mm}-${dd}`

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [drawDate, setDrawDate] = useState(todayStr)
  const [maxParticipants, setMaxParticipants] = useState<number>(30)

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [created, setCreated] = useState<Draw | null>(null)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isAuthed) return

    // Basic client-side validation
    const errs: string[] = []
    if (!title.trim()) errs.push('Title is required')
    if (!drawDate) errs.push('Draw date is required')
    const mp = Number(maxParticipants)
    if (Number.isNaN(mp) || mp < 1 || mp > 30) errs.push('Max participants must be between 1 and 30')
    if (errs.length > 0) {
      setError(errs.join('. '))
      return
    }

    const payload: CreateDrawRequest = {
      title: title.trim(),
      description: description.trim() ? description.trim() : undefined,
      drawDate: drawDate,
      maxParticipants: mp || undefined,
    }

    setSubmitting(true)
    setError(null)
    setCreated(null)
    try {
      const d = await drawService.createDraw(payload)
      setCreated(d)
      // Clear form but keep date default and max participants
      setTitle('')
      setDescription('')
    } catch (e: any) {
      setError(e?.message || 'Failed to create draw')
    } finally {
      setSubmitting(false)
    }
  }

  const onLogout = async () => {
    await authService.logout()
    window.location.reload()
  }

  const onLogin = (provider: 'google' | 'facebook') => {
    authService.initiateLogin(provider)
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
          <p>Please log in to create a draw.</p>
        </div>
      )}

      {isAuthed && (
        <section>
          <h2 style={{ marginTop: 0 }}>Create a New Draw</h2>

          {error && <p style={{ color: '#b91c1c' }}>{error}</p>}
          {created && (
            <div style={{ marginBottom: 12, padding: 12, border: '1px solid #16a34a', borderRadius: 8, background: '#f0fdf4' }}>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>Draw created successfully!</div>
              <div>
                <strong>{created.title}</strong> — {created.state} — {created.participantCount}/{created.maxParticipants}
              </div>
            </div>
          )}

          <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12, maxWidth: 640 }}>
            <div>
              <label htmlFor="title" style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Title</label>
              <input
                id="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g., Family Gift Exchange"
                required
                style={inputStyle}
              />
            </div>

            <div>
              <label htmlFor="description" style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Description (optional)</label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Add context or rules..."
                rows={3}
                style={{ ...inputStyle, resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label htmlFor="drawDate" style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Draw Date</label>
                <input
                  id="drawDate"
                  type="date"
                  value={drawDate}
                  onChange={(e) => setDrawDate(e.target.value)}
                  min={todayStr}
                  required
                  style={inputStyle}
                />
              </div>

              <div>
                <label htmlFor="maxParticipants" style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Max Participants (1-30)</label>
                <input
                  id="maxParticipants"
                  type="number"
                  inputMode="numeric"
                  value={maxParticipants}
                  onChange={(e) => setMaxParticipants(Number(e.target.value))}
                  min={1}
                  max={30}
                  style={inputStyle}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <button type="submit" disabled={submitting} style={buttonStyle('#16a34a')}>
                {submitting ? 'Creating…' : 'Create Draw'}
              </button>
              <button type="button" disabled={submitting} onClick={() => { setTitle(''); setDescription(''); setDrawDate(todayStr); setMaxParticipants(30); setError(null); }} style={buttonStyle('#6b7280')}>
                Reset
              </button>
            </div>
          </form>
        </section>
      )}
    </div>
  )
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px 12px',
  borderRadius: 8,
  border: '1px solid #e5e7eb',
  outline: 'none',
  fontSize: 14,
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
