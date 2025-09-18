// Draw management service
// Provides typed functions for listing, creating, retrieving, updating, joining, opening,
// performing a draw, fetching results, and fetching the current user's result.

import { api } from './apiService'
import type { UserDTO } from './authService'

// Types mirror OpenAPI components
export type DrawState = 'JOINING' | 'OPEN' | 'ARCHIVED'

export type Draw = {
  id: string
  title: string
  description?: string | null
  state: DrawState
  drawDate: string // yyyy-MM-dd
  participantCount: number
  maxParticipants: number
  creator: UserDTO
  participants: UserDTO[]
  canJoin: boolean
  canDraw: boolean
  createdAt: string // date-time
  openedAt?: string | null
  archivedAt?: string | null
}

export type DrawPage = {
  content: Draw[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export type Participation = {
  id: string
  draw: Draw
  joinedAt: string // date-time
}

export type DrawResult = {
  drawId: string
  drawnUser: UserDTO
  drawnAt: string // date-time
}

export type CreateDrawRequest = {
  title: string
  description?: string | null
  drawDate: string // yyyy-MM-dd
  maxParticipants?: number // default 30
}

export type UpdateDrawRequest = {
  title?: string
  description?: string | null
  drawDate?: string // yyyy-MM-dd
  maxParticipants?: number
}

export type ListDrawsParams = {
  state?: DrawState
  page?: number
  size?: number
}

export type UserDrawsParams = {
  role?: 'creator' | 'participant' | 'all'
  state?: DrawState | 'all'
}

// API functions
async function listDraws(params: ListDrawsParams = {}): Promise<DrawPage> {
  return api.get<DrawPage>('/draws', { query: params })
}

async function createDraw(payload: CreateDrawRequest): Promise<Draw> {
  return api.post<Draw>('/draws', payload)
}

async function getDraw(drawId: string): Promise<Draw> {
  return api.get<Draw>(`/draws/${encodeURIComponent(drawId)}`)
}

async function updateDraw(drawId: string, payload: UpdateDrawRequest): Promise<Draw> {
  return api.put<Draw>(`/draws/${encodeURIComponent(drawId)}`, payload)
}

async function joinDraw(drawId: string): Promise<Participation> {
  return api.post<Participation>(`/draws/${encodeURIComponent(drawId)}/join`)
}

async function openDraw(drawId: string): Promise<Draw> {
  return api.post<Draw>(`/draws/${encodeURIComponent(drawId)}/open`)
}

async function performDraw(drawId: string): Promise<DrawResult> {
  return api.post<DrawResult>(`/draws/${encodeURIComponent(drawId)}/draw`)
}

async function getResults(drawId: string): Promise<DrawResult[]> {
  return api.get<DrawResult[]>(`/draws/${encodeURIComponent(drawId)}/results`)
}

async function getMyResult(drawId: string): Promise<DrawResult> {
  return api.get<DrawResult>(`/draws/${encodeURIComponent(drawId)}/my-result`)
}

// Users endpoint for their draws (optional helper)
async function listMyDraws(params: UserDrawsParams = {}): Promise<Draw[]> {
  return api.get<Draw[]>('/users/me/draws', { query: params as any })
}

export const drawService = {
  listDraws,
  createDraw,
  getDraw,
  updateDraw,
  joinDraw,
  openDraw,
  performDraw,
  getResults,
  getMyResult,
  listMyDraws,
}
