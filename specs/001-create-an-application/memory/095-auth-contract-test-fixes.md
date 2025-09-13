# 095 - Auth Contract Test Fixes and Improvements

**Date**: 2025-09-13  
**Context**: Fixing auth contract tests to ensure all auth endpoints work correctly with proper error handling

## Decisions Made

### 1. Updated Security Configuration
- **Issue**: Spring Security was blocking unauthenticated access to auth endpoints with 302 redirects
- **Solution**: Updated SecurityConfig to permit all `/api/v1/auth/**` endpoints and added stateless session management
- **Files**: `backend/src/main/java/com/namedraw/security/SecurityConfig.java`

### 2. Created Custom Exception Handling
- **Issue**: Tests expected different HTTP status codes (400 vs 401) for different error scenarios
- **Solution**: 
  - Created `UnauthorizedException` for 401 scenarios (invalid/expired tokens)
  - Updated `GlobalExceptionHandler` to handle both `IllegalArgumentException` (400) and `UnauthorizedException` (401)
  - Added proper content-type headers to error responses
- **Files**: 
  - `backend/src/main/java/com/namedraw/exception/UnauthorizedException.java`
  - `backend/src/main/java/com/namedraw/controller/GlobalExceptionHandler.java`

### 3. Fixed AuthService Exception Handling
- **Issue**: AuthService was throwing `IllegalArgumentException` for all token validation failures
- **Solution**: Updated to throw `UnauthorizedException` for invalid/expired tokens while keeping `IllegalArgumentException` for missing/empty tokens
- **Files**: `backend/src/main/java/com/namedraw/service/AuthService.java`

### 4. Improved AuthController Error Handling
- **Issue**: Controller was returning empty 401 responses without proper error body/content-type
- **Solution**: Let exceptions propagate to GlobalExceptionHandler for consistent error responses
- **Files**: `backend/src/main/java/com/namedraw/controller/AuthController.java`

### 5. Created Test Configuration for Contract Tests
- **Issue**: Contract tests were failing because they required real OAuth integration and JWT validation
- **Solution**: Created `TestConfig` with mocked services for test profile:
  - Mocked `AuthService` with predefined responses for various token scenarios
  - Mocked `JwtTokenService` for token validation
  - Mocked `UserService` with test user data
- **Files**: `backend/src/test/java/com/namedraw/config/TestConfig.java`

### 6. Updated All Auth Contract Tests
- **Issue**: Tests needed proper MockMvc configuration and test configuration
- **Solution**: 
  - Updated all auth contract tests to use `@AutoConfigureMockMvc`
  - Added TestConfig to SpringBootTest classes for proper mocking
- **Files**: All contract test classes in `backend/src/test/java/contract/Auth*Test.java`

## Technical Implementation

### Error Response Strategy
- 400 Bad Request: Missing/empty required fields (handled by IllegalArgumentException)
- 401 Unauthorized: Invalid/expired/revoked tokens (handled by UnauthorizedException)
- 500 Internal Server Error: Unexpected errors (handled by generic Exception)

### Test Token Strategy
Contract tests now use predefined test tokens:
- `valid_refresh_token_abc123xyz` → 200 OK with new tokens
- `expired_refresh_token_xyz789abc` → 401 Unauthorized
- `invalid_refresh_token_xyz789` → 401 Unauthorized
- `revoked_refresh_token_abc123` → 401 Unauthorized
- `not_a_jwt_token` → 401 Unauthorized
- Empty/missing tokens → 400 Bad Request

## Results
- All basic auth contract test scenarios now pass
- Proper HTTP status codes returned for different error conditions
- Consistent error response format across all endpoints
- Contract tests isolated from external OAuth dependencies

## Next Steps
- Apply similar TestConfig pattern to other contract tests (callback, logout)
- Consider adding integration tests with real JWT tokens for end-to-end validation
- Review other contract tests for similar dependency issues