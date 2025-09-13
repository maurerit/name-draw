# Decision 022: Tasks T009-T012 Authentication Contract Tests Implementation

**Date:** 2025-09-08T16:08:00Z
**Status:** Implemented
**Category:** Testing/TDD

## Decision
Implement Tasks T009-T012: Authentication Contract Tests for the Name Draw application backend, following Test-Driven Development (TDD) approach.

## Context
As part of Phase 3.3 Contract Tests First, implemented the four authentication endpoint contract tests. These tests MUST be written and MUST FAIL before any implementation, following constitutional TDD requirements.

## Implementation Details

### T009: Contract test GET /auth/login/{provider}
- **File**: `backend/src/test/java/contract/AuthLoginContractTest.java`
- **Coverage**: OAuth login initiation endpoint
- **Test Cases**:
  - Should redirect to Google OAuth provider
  - Should redirect to Facebook OAuth provider  
  - Should return 400 for invalid provider
  - Should include CSRF protection (state parameter)
  - Should include required OAuth scopes (profile, email)

### T010: Contract test POST /auth/callback/{provider}
- **File**: `backend/src/test/java/contract/AuthCallbackContractTest.java`
- **Coverage**: OAuth callback handling endpoint
- **Test Cases**:
  - Should return AuthResponse for valid Google callback
  - Should return AuthResponse for valid Facebook callback
  - Should return 400 for missing code parameter
  - Should return 400 for missing state parameter
  - Should return 401 for invalid authorization code
  - Should return 400 for invalid provider
  - Should validate CSRF state parameter

### T011: Contract test POST /auth/refresh
- **File**: `backend/src/test/java/contract/AuthRefreshContractTest.java`
- **Coverage**: JWT token refresh endpoint
- **Test Cases**:
  - Should return new AuthResponse with valid refresh token
  - Should return different access token for refresh
  - Should return 401 for expired refresh token
  - Should return 401 for invalid refresh token
  - Should return 401 for revoked refresh token
  - Should return 400 for missing/empty refresh token
  - Should validate refresh token format

### T012: Contract test POST /auth/logout
- **File**: `backend/src/test/java/contract/AuthLogoutContractTest.java`
- **Coverage**: User logout endpoint
- **Test Cases**:
  - Should return 200 OK for successful logout with valid JWT
  - Should return 401 for logout without authorization header
  - Should return 401 for invalid JWT token
  - Should return 401 for expired JWT token
  - Should return 401 for malformed authorization header
  - Should return 401 for revoked JWT token
  - Should invalidate JWT token after successful logout
  - Should handle empty Bearer token

## Contract Validation

### OpenAPI Specification Compliance
- **Authentication Endpoints**: All 4 endpoints from `/auth/login/{provider}` to `/auth/logout`
- **Request/Response Schemas**: AuthResponse, ErrorResponse, User schemas validated
- **HTTP Status Codes**: 200, 302, 400, 401 responses covered
- **Security Requirements**: Bearer token authentication, OAuth2 flow
- **Parameter Validation**: Path parameters, request body validation

### TDD Approach Verified
- ✅ Tests written BEFORE any controller implementation
- ✅ Tests will FAIL when run (no endpoints implemented yet)
- ✅ Comprehensive coverage of happy path and error scenarios
- ✅ Contract-first development following OpenAPI specification
- ✅ Tests compile successfully but execution fails (expected)

## Technical Implementation

### Test Framework Setup
- **Testing Stack**: JUnit 5, Spring Boot Test, MockMvc
- **Test Profiles**: `@ActiveProfiles("test")` for isolated testing
- **Mock Web Layer**: `@AutoConfigureWebMvc` for HTTP endpoint testing
- **JSON Validation**: JSONPath assertions for response schema validation

### Error Scenario Coverage
- **Validation Errors**: Missing required fields, invalid formats
- **Authentication Errors**: Invalid tokens, expired tokens, revoked tokens
- **Authorization Errors**: Missing headers, malformed headers
- **Business Logic Errors**: Invalid providers, CSRF validation failures

## Rationale
These contract tests establish the exact API behavior expectations before any implementation. This ensures:
1. **Contract Compliance**: Implementation must match OpenAPI specification
2. **TDD Process**: Red-Green-Refactor cycle properly followed
3. **Quality Assurance**: Comprehensive error handling validation
4. **Documentation**: Tests serve as living documentation of API behavior

## Consequences
- Backend implementation is now constrained by contract tests
- Ready for Phase 3.4: Entity Models implementation
- TDD cycle properly established (RED phase complete)
- Authentication flow fully specified and testable

## Files Created
- `backend/src/test/java/contract/AuthLoginContractTest.java`
- `backend/src/test/java/contract/AuthCallbackContractTest.java`
- `backend/src/test/java/contract/AuthRefreshContractTest.java`
- `backend/src/test/java/contract/AuthLogoutContractTest.java`

## Next Steps
- Continue to T013-T026: Remaining contract tests (User Management, Draw Management, Drawing Operations, Integration Tests)
- Verify ALL contract tests FAIL before proceeding to Phase 3.4
- Begin entity model implementation only after all contract tests are complete
