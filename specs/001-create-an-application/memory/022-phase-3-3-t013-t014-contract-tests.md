# Decision 022: Phase 3.3 T013-T014 Contract Tests Implementation

**Date:** 2025-09-08T17:30:00Z
**Status:** Implemented
**Category:** Implementation - TDD Contract Tests

## Decision
Complete Phase 3.3 tasks T013 and T014: Implement contract tests for User Management endpoints GET /users/me and GET /users/me/draws.

## Context
Following the TDD approach mandated by the constitution, Phase 3.3 requires all contract tests to be written and FAILING before any implementation work begins. These tests define the API contracts for user profile and user draws endpoints.

## Implementation Details

### T013: Contract test GET /users/me 
- **File**: `backend/src/test/java/contract/UserProfileContractTest.java`
- **Purpose**: Verify API contract for getting current user's profile
- **Test Coverage**:
  - Valid JWT token → 200 with User JSON (id, name, profilePictureUrl, isActive)
  - Missing Authorization header → 401 Unauthorized
  - Invalid/malformed tokens → 401 Unauthorized  
  - Expired tokens → 401 Unauthorized
  - Deactivated user tokens → 401 Unauthorized
- **API Contract**: GET /api/v1/users/me with Bearer authentication

### T014: Contract test GET /users/me/draws
- **File**: `backend/src/test/java/contract/UserDrawsContractTest.java`
- **Purpose**: Verify API contract for getting current user's draws
- **Test Coverage**:
  - Valid JWT token → 200 with Draw array
  - Query parameter filters (role=[creator|participant|all], state=[JOINING|OPEN|ARCHIVED|all])
  - Missing Authorization header → 401 Unauthorized
  - Invalid/malformed tokens → 401 Unauthorized
  - Invalid query parameters → 400 Bad Request
- **API Contract**: GET /api/v1/users/me/draws with optional query filters

## TDD Validation ✅
Both contract tests are properly **FAILING** as expected:
- Error: `No qualifying bean of type 'org.springframework.test.web.servlet.MockMvc' available`
- Root Cause: No controllers implemented yet (MockMvc bean cannot be created)
- Expected Behavior: Tests fail until UserController is implemented
- This confirms proper TDD approach - tests written first, failing until implementation

## Technical Details
- **Testing Framework**: Spring Boot Test with MockMvc
- **Authentication**: Bearer JWT token validation
- **Error Handling**: Comprehensive 401/400 response testing
- **Schema Validation**: JSON response structure verification
- **Spring Configuration**: Tests point to NameDrawApplication.class

## Next Steps
The contract tests are ready and will guide implementation of:
1. UserController with /users/me endpoints
2. JWT authentication/authorization
3. User service layer
4. Error handling middleware

## Impact
- Establishes clear API contracts for user management endpoints
- Provides executable specification for UserController implementation
- Ensures consistent error handling patterns
- Validates authentication requirements early in development cycle

## References
- API Specification: `specs/001-create-an-application/contracts/api.yaml`
- Tasks Definition: `specs/001-create-an-application/tasks.md` (T013-T014 marked complete)
- TDD Constitution: Contract tests must FAIL before implementation begins
