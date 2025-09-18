import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import '@testing-library/jest-dom/vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import React from 'react'
import DashboardPage from '../../src/pages/DashboardPage'

// Mock authService used by DashboardPage (ESM-friendly)
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

// Mock drawService used by DashboardPage
vi.mock('../../src/services/drawService', async () => {
  const actual = await vi.importActual<typeof import('../../src/services/drawService')>(
    '../../src/services/drawService'
  )
  return {
    ...actual,
    drawService: {
      ...actual.drawService,
      listDraws: vi.fn(),
    },
  }
})

import { authService } from '../../src/services/authService'
import { drawService, type DrawPage, type Draw } from '../../src/services/drawService'

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

function makePage(content: Draw[]): DrawPage {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 10,
    number: 0,
    first: true,
    last: true,
  }
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    // restore any spies on window.location
    vi.restoreAllMocks()
  })

  it('renders app name and login options when unauthenticated', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)
    ;(authService.getUser as any).mockReturnValue(null)

    render(<DashboardPage />)

    expect(screen.getByText('Name Draw')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with facebook/i })).toBeInTheDocument()
    expect(screen.getByText(/please log in to view and manage draws/i)).toBeInTheDocument()
  })

  it('initiates provider login on click (Google)', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)

    render(<DashboardPage />)

    fireEvent.click(screen.getByRole('button', { name: /login with google/i }))
    expect(authService.initiateLogin).toHaveBeenCalledWith('google')
  })

  it('shows draws when authenticated and list loads', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.listDraws as any).mockResolvedValue(
      makePage([
        makeDraw({ id: 'd1', title: 'Family Exchange', participantCount: 3, maxParticipants: 10 }),
        makeDraw({ id: 'd2', title: 'Friends Exchange', participantCount: 5, maxParticipants: 8 }),
      ])
    )

    render(<DashboardPage />)

    // Loading state appears first
    expect(screen.getByText(/loading draws/i)).toBeInTheDocument()

    // Then the list renders
    await waitFor(() => {
      // Greeting is split across nodes ("Hello, " + <strong>Alice</strong>)
      expect(screen.getByText('Alice')).toBeInTheDocument()
      expect(screen.getByText(/family exchange/i)).toBeInTheDocument()
      expect(screen.getByText(/friends exchange/i)).toBeInTheDocument()
    })

    // Assert API was called with default params
    expect(drawService.listDraws).toHaveBeenCalledWith({ state: 'JOINING', page: 0, size: 10 })
  })

  it('shows empty state when no draws available', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.listDraws as any).mockResolvedValue(makePage([]))

    render(<DashboardPage />)

    await waitFor(() => {
      expect(screen.getByText(/no draws available yet/i)).toBeInTheDocument()
    })
  })

  it('shows error message when list fails', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.listDraws as any).mockRejectedValue(new Error('Boom'))

    render(<DashboardPage />)

    await waitFor(() => {
      expect(screen.getByText(/boom/i)).toBeInTheDocument()
    })
  })

  it('logs out when clicking Logout', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.listDraws as any).mockResolvedValue(makePage([]))

    render(<DashboardPage />)

    // Wait until authed header appears
    await screen.findByText('Alice')

    fireEvent.click(screen.getByRole('button', { name: /logout/i }))

    await waitFor(() => {
      expect(authService.logout).toHaveBeenCalled()
    })
  })
})
