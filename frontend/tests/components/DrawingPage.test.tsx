import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import '@testing-library/jest-dom/vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import React from 'react'
import DrawingPage from '../../src/pages/DrawingPage'

// Mock authService used by DrawingPage
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

// Mock drawService used by DrawingPage
vi.mock('../../src/services/drawService', async () => {
  const actual = await vi.importActual<typeof import('../../src/services/drawService')>(
    '../../src/services/drawService'
  )
  return {
    ...actual,
    drawService: {
      ...actual.drawService,
      getDraw: vi.fn(),
      getMyResult: vi.fn(),
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

describe('DrawingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pushUrl('/')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders login prompts when unauthenticated', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)
    render(<DrawingPage />)

    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with facebook/i })).toBeInTheDocument()
    expect(screen.getByText(/please log in to draw a name/i)).toBeInTheDocument()
  })

  it('shows message when no draw id provided', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })

    render(<DrawingPage />)

    expect(
      screen.getByText(/no draw selected\. provide an id via query string/i)
    ).toBeInTheDocument()
  })

  it('enables drawing when OPEN and eligible; shows result after draw', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u2', name: 'Bob', isActive: true })

    // Initial: OPEN and canDraw true
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd1', state: 'OPEN', canDraw: true, creator: { id: 'u0', name: 'Organizer', isActive: true } })
    )

    const result: DrawResult = {
      drawId: 'd1',
      drawnUser: { id: 'u3', name: 'Carol', isActive: true },
      drawnAt: new Date('2025-09-01T12:00:00.000Z').toISOString(),
    }
    ;(drawService.performDraw as any).mockResolvedValue(result)

    // After draw, refresh draw: canDraw false
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd1', state: 'OPEN', canDraw: false })
    )

    pushUrl('/?id=d1')
    render(<DrawingPage />)

    // Wait for draw button
    const btn = await screen.findByRole('button', { name: /draw name/i })
    expect(btn).toBeEnabled()

    fireEvent.click(btn)

    await waitFor(() => {
      expect(drawService.performDraw).toHaveBeenCalledWith('d1')
      expect(screen.getByText(/your draw/i)).toBeInTheDocument()
      expect(screen.getByText(/carol/i)).toBeInTheDocument()
    })
  })

  it('shows ineligible hint when OPEN but cannot draw yet', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u4', name: 'Dana', isActive: true })

    ;(drawService.getDraw as any).mockResolvedValue(
      makeDraw({ id: 'd2', state: 'OPEN', canDraw: false })
    )

    pushUrl('/?id=d2')
    render(<DrawingPage />)

    // Button is disabled and hint is present
    await screen.findByText(/state/i)
    const button = screen.getByRole('button', { name: /draw name/i })
    expect(button).toBeDisabled()
    expect(
      screen.getByText(/you may not be eligible to draw yet/i)
    ).toBeInTheDocument()
  })

  it('shows not open message when draw is not OPEN', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u5', name: 'Eve', isActive: true })

    ;(drawService.getDraw as any).mockResolvedValue(
      makeDraw({ id: 'd3', state: 'JOINING' })
    )

    pushUrl('/?id=d3')
    render(<DrawingPage />)

    await waitFor(() => {
      expect(
        screen.getByText(/this draw is not open yet\. please check back later\./i)
      ).toBeInTheDocument()
    })
  })

  it('shows organizer note when current user is creator', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u0', name: 'Organizer', isActive: true })

    ;(drawService.getDraw as any).mockResolvedValue(
      makeDraw({ id: 'd4', state: 'JOINING', creator: { id: 'u0', name: 'Organizer', isActive: true } })
    )

    pushUrl('/?id=d4')
    render(<DrawingPage />)

    await waitFor(() => {
      expect(
        screen.getByText(/organizer note:/i)
      ).toBeInTheDocument()
    })
  })

  it('shows error when perform draw fails', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u6', name: 'Frank', isActive: true })

    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd5', state: 'OPEN', canDraw: true })
    )
    ;(drawService.performDraw as any).mockRejectedValue(new Error('Draw failed'))

    // After failure, component still tries to refresh draw; provide stub
    ;(drawService.getDraw as any).mockResolvedValueOnce(
      makeDraw({ id: 'd5', state: 'OPEN', canDraw: true })
    )

    pushUrl('/?id=d5')
    render(<DrawingPage />)

    const btn = await screen.findByRole('button', { name: /draw name/i })
    fireEvent.click(btn)

    await waitFor(() => {
      expect(screen.getByText(/draw failed/i)).toBeInTheDocument()
    })
  })
})
