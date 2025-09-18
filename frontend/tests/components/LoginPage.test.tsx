import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import React from 'react'
import LoginPage from '../../src/pages/LoginPage'

// Mock authService used by LoginPage (ESM-friendly)
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

import { authService } from '../../src/services/authService'

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders app name and login buttons', () => {
    render(<LoginPage />)
    expect(screen.getByText('Name Draw')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /continue with google/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /continue with facebook/i })).toBeInTheDocument()
  })

  it('initiates Google login on click', () => {
    render(<LoginPage />)

    const googleBtn = screen.getByRole('button', { name: /continue with google/i })
    fireEvent.click(googleBtn)

    expect(authService.initiateLogin).toHaveBeenCalledWith('google')
  })

  it('initiates Facebook login on click', () => {
    render(<LoginPage />)

    const fbBtn = screen.getByRole('button', { name: /continue with facebook/i })
    fireEvent.click(fbBtn)

    expect(authService.initiateLogin).toHaveBeenCalledWith('facebook')
  })
})
