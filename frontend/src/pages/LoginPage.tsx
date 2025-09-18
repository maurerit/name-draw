import { useMemo, useState } from 'react'
import { authService, type OAuthProvider } from '../services/authService'

function useAppName(): string {
  return useMemo(() => {
    const name = (import.meta as any)?.env?.VITE_APP_NAME as string | undefined
    return name || 'Name Draw'
  }, [])
}

export default function LoginPage() {
  const appName = useAppName()
  const [loadingProvider, setLoadingProvider] = useState<OAuthProvider | null>(null)
  const isAuthed = authService.isAuthenticated()
  const user = authService.getUser()

  const onLogin = (provider: OAuthProvider) => {
    setLoadingProvider(provider)
    // Redirect to backend OAuth login endpoint
    authService.initiateLogin(provider)
  }

  const onLogout = async () => {
    setLoadingProvider(null)
    await authService.logout()
    // Simple reload to clear any app state
    window.location.reload()
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 420, padding: 24, borderRadius: 12, boxShadow: '0 6px 24px rgba(0,0,0,0.08)', background: 'white' }}>
        <h1 style={{ margin: 0, fontSize: 24 }}>{appName}</h1>
        <p style={{ color: '#555', marginTop: 8 }}>Sign in to continue</p>

        {isAuthed && (
          <div style={{ marginTop: 16, padding: 12, border: '1px solid #e5e7eb', borderRadius: 8 }}>
            <div style={{ fontSize: 14, marginBottom: 8 }}>
              Logged in as <strong>{user?.name || 'User'}</strong>
            </div>
            <button onClick={onLogout} style={buttonStyle('#ef4444')}>
              Log out
            </button>
          </div>
        )}

        {!isAuthed && (
          <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
            <button
              onClick={() => onLogin('google')}
              disabled={loadingProvider !== null}
              style={buttonStyle('#2563eb')}
            >
              {loadingProvider === 'google' ? 'Redirecting…' : 'Continue with Google'}
            </button>

            <button
              onClick={() => onLogin('facebook')}
              disabled={loadingProvider !== null}
              style={buttonStyle('#3b82f6')}
            >
              {loadingProvider === 'facebook' ? 'Redirecting…' : 'Continue with Facebook'}
            </button>
          </div>
        )}

        <p style={{ color: '#6b7280', fontSize: 12, marginTop: 16 }}>
          By continuing you agree to the Terms and acknowledge the Privacy Policy.
        </p>
      </div>
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
