// Generic API client service for the frontend
// - Sets API base from VITE_API_BASE_URL (or /api/v1 fallback)
// - Attaches Authorization header with valid access token
// - On 401, attempts refresh and retries once automatically
// - Provides typed helpers: get/post/put/patch/delete
// - Supports JSON request/response and query params

import { authService } from './authService'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export type ApiRequestOptions = {
  headers?: Record<string, string>
  query?: Record<string, string | number | boolean | undefined | null>
  body?: any
  // When true, do not attach Authorization header
  skipAuth?: boolean
  // If set, override default JSON handling
  rawResponse?: boolean
}

export type ApiError = Error & { status?: number; details?: unknown }

function getApiBase(): string {
  const base = (import.meta as any)?.env?.VITE_API_BASE_URL as string | undefined
  return (base && base.replace(/\/$/, '')) || '/api/v1'
}

function buildUrl(path: string, query?: ApiRequestOptions['query']): string {
  const url = new URL(`${getApiBase()}${path}`.replace(/\/+$|(^[^/])/, (m) => m))
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v === undefined || v === null) continue
      url.searchParams.set(k, String(v))
    }
  }
  return url.toString()
}

async function parseJsonSafe(res: Response): Promise<any | null> {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function request<T = unknown>(method: HttpMethod, path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { headers = {}, body, query, skipAuth, rawResponse } = options

  const finalHeaders: Record<string, string> = {
    'Accept': rawResponse ? '*/*' : 'application/json',
    ...headers,
  }

  let authToken: string | null = null
  if (!skipAuth) {
    authToken = await authService.getValidAccessToken()
    if (authToken) {
      finalHeaders['Authorization'] = `Bearer ${authToken}`
    }
  }

  const isJsonBody = body !== undefined && body !== null && !(body instanceof FormData)
  if (isJsonBody && !rawResponse) {
    finalHeaders['Content-Type'] = 'application/json'
  }

  const res = await fetch(buildUrl(path, query), {
    method,
    headers: finalHeaders,
    body: isJsonBody ? JSON.stringify(body) : (body as any),
  })

  if (res.status === 401 && !skipAuth) {
    // Try refresh once and retry
    try {
      const refreshed = await authService.refreshToken()
      const retryHeaders = { ...finalHeaders, Authorization: `Bearer ${refreshed.accessToken}` }
      const retry = await fetch(buildUrl(path, query), {
        method,
        headers: retryHeaders,
        body: isJsonBody ? JSON.stringify(body) : (body as any),
      })
      if (!retry.ok) {
        const details = await parseJsonSafe(retry)
        const err = new Error(`Request failed after refresh (${retry.status})`) as ApiError
        err.status = retry.status
        err.details = details
        throw err
      }
      return (rawResponse ? (retry as unknown as T) : ((await parseJsonSafe(retry)) as T))
    } catch (e) {
      // If refresh fails, ensure session cleared by authService and rethrow as unauthorized
      const err = new Error('Unauthorized') as ApiError
      err.status = 401
      err.details = (e as any)?.message
      throw err
    }
  }

  if (!res.ok) {
    const details = await parseJsonSafe(res)
    const err = new Error(`Request failed (${res.status})`) as ApiError
    err.status = res.status
    err.details = details
    throw err
  }

  return (rawResponse ? (res as unknown as T) : ((await parseJsonSafe(res)) as T))
}

export const api = {
  get: <T = unknown>(path: string, options?: ApiRequestOptions) => request<T>('GET', path, options),
  post: <T = unknown>(path: string, body?: any, options?: Omit<ApiRequestOptions, 'body'>) =>
    request<T>('POST', path, { ...options, body }),
  put: <T = unknown>(path: string, body?: any, options?: Omit<ApiRequestOptions, 'body'>) =>
    request<T>('PUT', path, { ...options, body }),
  patch: <T = unknown>(path: string, body?: any, options?: Omit<ApiRequestOptions, 'body'>) =>
    request<T>('PATCH', path, { ...options, body }),
  delete: <T = unknown>(path: string, options?: ApiRequestOptions) => request<T>('DELETE', path, options),
}

export type { ApiRequestOptions as RequestOptions }
