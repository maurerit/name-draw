import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import '@testing-library/jest-dom/vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import React from 'react'
import DrawDetailPage from '../../src/pages/DrawDetailPage'

// Mock authService used by DrawDetailPage
vi.mock('../../src/services/authService', async () => {
  const actual = await vi.importActual<typeof import('../../src/services/authService')>(
    '../../src/services/authService'
  )
  return {
    ...actual,
    authService: {
      ...actual.authService,
      isAuthenticated: vi.fn(() => false),
      getUser: vi.fn(() => null),
      initiateLogin: vi.fn(),
      logout: vi.fn(),
    },
  }
})

// Mock drawService used by DrawDetailPage
vi.mock('../../src/services/drawService', async () => {
  const actual = await vi.importActual<typeof import('../../src/services/drawService')>(
    '../../src/services/drawService'
  )
  return {
    ...actual,
    drawService: {
      ...actual.drawService,
      getDraw: vi.fn(),
      getResults: vi.fn(),
      getMyResult: vi.fn(),
      joinDraw: vi.fn(),
      openDraw: vi.fn(),
      performDraw: vi.fn(),
    },
  }
})

import { authService } from '../../src/services/authService'
import { drawService, type Draw, type DrawResult } from '../../src/services/drawService'

function makeDraw(overrides: Partial<Draw> = {}): Draw {
  return {
    id: overrides.id ?? 'd1',
    title: overrides.title ?? 'Family Gift Exchange',
    description: overrides.description ?? null,
    state: overrides.state ?? 'JOINING',
    drawDate: overrides.drawDate ?? '2025-12-20',
    participantCount: overrides.participantCount ?? 2,
    maxParticipants: overrides.maxParticipants ?? 10,
    creator: overrides.creator ?? { id: 'u0', name: 'Organizer', isActive: true },
    participants: overrides.participants ?? [],
    canJoin: overrides.canJoin ?? true,
    canDraw: overrides.canDraw ?? false,
    createdAt: overrides.createdAt ?? '2025-09-01T00:00:00.000Z',
    openedAt: overrides.openedAt ?? null,
    archivedAt: overrides.archivedAt ?? null,
  }
}

function pushUrl(path: string) {
  const url = new URL(path, window.location.origin)
  window.history.pushState({}, '', url.toString())
}

describe('DrawDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // reset URL to base
    pushUrl('/')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders login prompts when unauthenticated', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)
    render(<DrawDetailPage />)

    // Header login buttons
    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with facebook/i })).toBeInTheDocument()

    // Body prompt
    expect(screen.getByText(/please log in to view this draw/i)).toBeInTheDocument()
  })

  it('shows message when no draw id provided', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })

    render(<DrawDetailPage />)

    expect(
      screen.getByText(/no draw selected\. provide an id via query string/i)
    ).toBeInTheDocument()
  })

  it('loads and displays draw details when authenticated with id', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u2', name: 'Bob', isActive: true })

    ;(drawService.getDraw as any).mockResolvedValue(
      makeDraw({
        id: 'd1',
        title: 'Holiday Exchange',
        participantCount: 3,
        maxParticipants: 10,
        participants: [
          { id: 'u2', name: 'Bob', isActive: true },
          { id: 'u3', name: 'Carol', isActive: true },
          { id: 'u4', name: 'Dave', isActive: true },
        ],
      })
    )

    pushUrl('/?id=d1')
    render(<DrawDetailPage />)

    // loading indicator appears first (copy contains the word "Loading")
    expect(screen.getByText(/loading/i)).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText(/holiday exchange/i)).toBeInTheDocument()
      expect(screen.getByText('Carol')).toBeInTheDocument()
      expect(screen.getByText('Dave')).toBeInTheDocument()
    })

    expect(drawService.getDraw).toHaveBeenCalledWith('d1')
  })

  it('joins the draw when clicking Join Draw', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u5', name: 'Eve', isActive: true })

    // First load: can join
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd2', title: 'Friends Exchange', canJoin: true, participantCount: 1 })
    )
    // After join, refresh: cannot join, participant count increased
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd2', title: 'Friends Exchange', canJoin: false, participantCount: 2 })
    )
    ;(drawService.joinDraw as any).mockResolvedValue({ id: 'p1', draw: makeDraw({ id: 'd2' }), joinedAt: new Date().toISOString() })

    pushUrl('/?id=d2')
    render(<DrawDetailPage />)

    // Wait for initial load
    await screen.findByText(/friends exchange/i)

    const joinBtn = screen.getByRole('button', { name: /join draw/i })
    fireEvent.click(joinBtn)

    await waitFor(() => {
      expect(drawService.joinDraw).toHaveBeenCalledWith('d2')
      expect(drawService.getDraw).toHaveBeenCalledTimes(2)
    })
  })

  it('opens the draw when creator clicks Open for Drawing', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u0', name: 'Organizer', isActive: true })

    // Initial: JOINING and current user is creator
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd3', state: 'JOINING', creator: { id: 'u0', name: 'Organizer', isActive: true } })
    )
    // After opening: state OPEN
    ;(drawService.openDraw as any).mockResolvedValue(
      makeDraw({ id: 'd3', state: 'OPEN', creator: { id: 'u0', name: 'Organizer', isActive: true } })
    )
    ;(drawService.getResults as any).mockResolvedValue([])

    pushUrl('/?id=d3')
    render(<DrawDetailPage />)

    await screen.findByText(/organizer/i)

    const openBtn = screen.getByRole('button', { name: /open for drawing/i })
    fireEvent.click(openBtn)

    await waitFor(() => {
      expect(drawService.openDraw).toHaveBeenCalledWith('d3')
      expect(drawService.getResults).toHaveBeenCalledWith('d3')
    })
  })

  it('performs a draw and shows my result', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u6', name: 'Frank', isActive: true })

    // Initial: OPEN and canDraw true
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd4', state: 'OPEN', canDraw: true })
    )
    const result: DrawResult = {
      drawId: 'd4',
      drawnUser: { id: 'u7', name: 'Grace', isActive: true },
      drawnAt: new Date('2025-09-01T12:00:00.000Z').toISOString(),
    }
    ;(drawService.performDraw as any).mockResolvedValue(result)
    // After draw, refresh draw: canDraw false
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd4', state: 'OPEN', canDraw: false })
    )

    pushUrl('/?id=d4')
    render(<DrawDetailPage />)

    await screen.findByText(/draw name/i)
    fireEvent.click(screen.getByRole('button', { name: /draw name/i }))

    await waitFor(() => {
      expect(drawService.performDraw).toHaveBeenCalledWith('d4')
      expect(screen.getByText(/your draw/i)).toBeInTheDocument()
      expect(screen.getByText(/grace/i)).toBeInTheDocument()
    })
  })

  it('shows error when loading draw fails', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u9', name: 'Henry', isActive: true })
    ;(drawService.getDraw as any).mockRejectedValue(new Error('Failed to load draw'))

    pushUrl('/?id=bad')
    render(<DrawDetailPage />)

    await waitFor(() => {
      expect(screen.getByText(/failed to load draw/i)).toBeInTheDocument()
    })
  })
})
