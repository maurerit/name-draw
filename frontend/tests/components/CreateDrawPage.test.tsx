import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import '@testing-library/jest-dom/vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import React from 'react'
import CreateDrawPage from '../../src/pages/CreateDrawPage'

// Mock authService used by CreateDrawPage (ESM-friendly)
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

// Mock drawService used by CreateDrawPage
vi.mock('../../src/services/drawService', async () => {
  const actual = await vi.importActual<typeof import('../../src/services/drawService')>(
    '../../src/services/drawService'
  )
  return {
    ...actual,
    drawService: {
      ...actual.drawService,
      createDraw: vi.fn(),
    },
  }
})

import { authService } from '../../src/services/authService'
import { drawService, type Draw } from '../../src/services/drawService'

function makeDraw(overrides: Partial<Draw> = {}): Draw {
  return {
    id: overrides.id ?? 'd1',
    title: overrides.title ?? 'Family Gift Exchange',
    description: overrides.description ?? null,
    state: overrides.state ?? 'JOINING',
    drawDate: overrides.drawDate ?? '2025-12-20',
    participantCount: overrides.participantCount ?? 0,
    maxParticipants: overrides.maxParticipants ?? 30,
    creator: overrides.creator ?? { id: 'u0', name: 'Organizer', isActive: true },
    participants: overrides.participants ?? [],
    canJoin: overrides.canJoin ?? true,
    canDraw: overrides.canDraw ?? false,
    createdAt: overrides.createdAt ?? '2025-09-01T00:00:00.000Z',
    openedAt: overrides.openedAt ?? null,
    archivedAt: overrides.archivedAt ?? null,
  }
}

describe('CreateDrawPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders app name and login options when unauthenticated', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)
    ;(authService.getUser as any).mockReturnValue(null)

    render(<CreateDrawPage />)

    expect(screen.getByText('Name Draw')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /login with facebook/i })).toBeInTheDocument()
    expect(screen.getByText(/please log in to create a draw/i)).toBeInTheDocument()
  })

  it('initiates provider login on click (Google/Facebook)', () => {
    ;(authService.isAuthenticated as any).mockReturnValue(false)

    render(<CreateDrawPage />)

    fireEvent.click(screen.getByRole('button', { name: /login with google/i }))
    expect(authService.initiateLogin).toHaveBeenCalledWith('google')

    fireEvent.click(screen.getByRole('button', { name: /login with facebook/i }))
    expect(authService.initiateLogin).toHaveBeenCalledWith('facebook')
  })

  it('submits form and shows success when authenticated', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.createDraw as any).mockResolvedValue(
      makeDraw({ title: 'Holiday Draw', participantCount: 1, maxParticipants: 20 })
    )

    render(<CreateDrawPage />)

    // Fill out form
    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: '  Holiday Draw  ' } })
    fireEvent.change(screen.getByLabelText(/description/i), { target: { value: ' Fun gifts ' } })

    // Date defaults to today; still set explicitly to avoid timezone issues
    const today = new Date()
    const yyyy = today.getFullYear()
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const dd = String(today.getDate()).padStart(2, '0')
    const todayStr = `${yyyy}-${mm}-${dd}`
    fireEvent.change(screen.getByLabelText(/draw date/i), { target: { value: todayStr } })

    fireEvent.change(screen.getByLabelText(/max participants/i), { target: { value: 20 } })

    // Submit
    fireEvent.click(screen.getByRole('button', { name: /create draw/i }))

    await waitFor(() => {
      expect(drawService.createDraw).toHaveBeenCalledWith({
        title: 'Holiday Draw',
        description: 'Fun gifts',
        drawDate: todayStr,
        maxParticipants: 20,
      })
    })

    // Success banner and cleared inputs
    await waitFor(() => {
      expect(screen.getByText(/draw created successfully/i)).toBeInTheDocument()
      const titleInput = screen.getByLabelText(/title/i) as HTMLInputElement
      expect(titleInput).toHaveValue('')
    })
  })

  it('shows validation error when title is empty', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })

    render(<CreateDrawPage />)

  // Use whitespace to bypass native "required" constraint but trigger trim validation
  fireEvent.change(screen.getByLabelText(/title/i), { target: { value: '   ' } })
  fireEvent.click(screen.getByRole('button', { name: /create draw/i }))

    await waitFor(() => {
      expect(screen.getByText(/title is required/i)).toBeInTheDocument()
    })
  })

  it('shows API error when create fails', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.createDraw as any).mockRejectedValue(new Error('Server down'))

    render(<CreateDrawPage />)

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Oops' } })
    fireEvent.click(screen.getByRole('button', { name: /create draw/i }))

    await waitFor(() => {
      expect(screen.getByText(/server down/i)).toBeInTheDocument()
    })
  })

  it('logs out when clicking Logout', async () => {
    ;(authService.isAuthenticated as any).mockReturnValue(true)
    ;(authService.getUser as any).mockReturnValue({ id: 'u1', name: 'Alice', isActive: true })
    ;(drawService.createDraw as any).mockResolvedValue(makeDraw())

    render(<CreateDrawPage />)

    await screen.findByText('Alice')

    fireEvent.click(screen.getByRole('button', { name: /logout/i }))

    await waitFor(() => {
      expect(authService.logout).toHaveBeenCalled()
    })
  })
})
