// Authentication service with OAuth integration
// Implements login URL generation, OAuth callback handling, token storage, refresh, and logout

export type OAuthProvider = 'google' | 'facebook'

export type UserDTO = {
  id: string
  name: string
  profilePictureUrl?: string | null
  isActive: boolean
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string // e.g., "Bearer"
  expiresIn: number // seconds until expiry
  user: UserDTO
}

const STORAGE_KEYS = {
  accessToken: 'nd_accessToken',
  refreshToken: 'nd_refreshToken',
  tokenType: 'nd_tokenType',
  tokenExpiry: 'nd_tokenExpiry', // epoch millis
  user: 'nd_user',
}

function getApiBase(): string {
  // Prefer configured base URL, fallback to same-origin /api/v1
  const base = (import.meta as any)?.env?.VITE_API_BASE_URL as string | undefined
  return (base && base.replace(/\/$/, '')) || '/api/v1'
}

function buildUrl(path: string): string {
  return `${getApiBase()}${path}`
}

function saveSession(auth: AuthResponse) {
  localStorage.setItem(STORAGE_KEYS.accessToken, auth.accessToken)
  localStorage.setItem(STORAGE_KEYS.refreshToken, auth.refreshToken)
  localStorage.setItem(STORAGE_KEYS.tokenType, auth.tokenType)
  // Convert expiresIn (seconds) to epoch millis; be tolerant if already millis
  const now = Date.now()
  const expMillis = auth.expiresIn > 1_000_000_000 ? now + auth.expiresIn : now + auth.expiresIn * 1000
  localStorage.setItem(STORAGE_KEYS.tokenExpiry, String(expMillis))
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(auth.user))
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEYS.accessToken)
  localStorage.removeItem(STORAGE_KEYS.refreshToken)
  localStorage.removeItem(STORAGE_KEYS.tokenType)
  localStorage.removeItem(STORAGE_KEYS.tokenExpiry)
  localStorage.removeItem(STORAGE_KEYS.user)
}

function getAccessToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.accessToken)
}

function getRefreshToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.refreshToken)
}

function getTokenType(): string | null {
  return localStorage.getItem(STORAGE_KEYS.tokenType)
}

function getExpiry(): number | null {
  const v = localStorage.getItem(STORAGE_KEYS.tokenExpiry)
  return v ? Number(v) : null
}

function isTokenExpired(skewMs = 30_000): boolean {
  const exp = getExpiry()
  if (!exp) return true
  return Date.now() >= exp - skewMs
}

export function getLoginUrl(provider: OAuthProvider): string {
  return buildUrl(`/auth/login/${provider}`)
}

export function initiateLogin(provider: OAuthProvider): void {
  window.location.href = getLoginUrl(provider)
}

export async function handleOAuthCallback(provider: OAuthProvider, code: string, state: string): Promise<AuthResponse> {
  const res = await fetch(buildUrl(`/auth/callback/${provider}`), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, state }),
  })

  if (!res.ok) {
    const err = await safeJson(res)
    throw new Error(err?.message || `OAuth callback failed (${res.status})`)
  }

  const auth = (await res.json()) as AuthResponse
  saveSession(auth)
  return auth
}

async function safeJson(res: Response): Promise<any | null> {
  try {
    return await res.json()
  } catch {
    return null
  }
}

export async function refreshToken(): Promise<AuthResponse> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new Error('No refresh token available')

  const res = await fetch(buildUrl('/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (!res.ok) {
    // On refresh failure, clear session
    clearSession()
    const err = await safeJson(res)
    throw new Error(err?.message || `Refresh failed (${res.status})`)
  }

  const auth = (await res.json()) as AuthResponse
  saveSession(auth)
  return auth
}

export async function getValidAccessToken(): Promise<string | null> {
  if (!getAccessToken()) return null
  if (!isTokenExpired()) return getAccessToken()
  try {
    const updated = await refreshToken()
    return updated.accessToken
  } catch {
    return null
  }
}

export async function logout(): Promise<void> {
  const token = getAccessToken()
  const type = getTokenType() || 'Bearer'
  try {
    if (token) {
      await fetch(buildUrl('/auth/logout'), {
        method: 'POST',
        headers: { Authorization: `${type} ${token}` },
      })
    }
  } catch {
    // ignore network errors on logout
  } finally {
    clearSession()
  }
}

export function isAuthenticated(): boolean {
  const token = getAccessToken()
  if (!token) return false
  return !isTokenExpired()
}

export function getUser(): UserDTO | null {
  const raw = localStorage.getItem(STORAGE_KEYS.user)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserDTO
  } catch {
    return null
  }
}

export const authService = {
  getLoginUrl,
  initiateLogin,
  handleOAuthCallback,
  getAccessToken,
  getValidAccessToken,
  refreshToken,
  logout,
  isAuthenticated,
  getUser,
  clearSession,
}
