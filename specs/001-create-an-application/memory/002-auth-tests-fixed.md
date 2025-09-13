# Decision 002: Auth Tests Fixed After RestClient Refactoring

**Date**: September 13, 2025  
**Task Context**: T040 - AuthController OAuth endpoints implementation  
**Decision Type**: Test fixing and error handling improvements

## User Request
```
Ok, we've refactored the rest client calls out into a mockable class. We can now continue trying to get *Auth* tests passing. Make those tests pass.
```

## Problem Identified
After refactoring RestClient into AuthClient, the Auth contract tests were failing because:
1. Test configuration was still mocking RestClient instead of AuthClient
2. AuthController error responses lacked proper content types
3. Invalid provider errors returned wrong HTTP status codes
4. Auth test files still referenced RestClient dependencies

## Decisions Made

### 1. Updated Test Configuration
- **Changed**: `ContractTestConfig` to mock `AuthClient` instead of `RestClient`
- **Rationale**: Align with new architecture after RestClient refactoring
- **Impact**: Enables proper mocking of OAuth provider interactions

### 2. Fixed AuthController Error Handling
- **Added**: Proper JSON content types to all error responses
- **Differentiated**: Bad Request (400) vs Unauthorized (401) status codes
- **Improved**: Error message structure with timestamps and paths
- **Rationale**: Contract tests expect specific response formats and status codes

### 3. Enhanced OAuth Flow Mocking
- **Created**: Specific mocks for Google and Facebook user info responses
- **Added**: Conditional mocking for error scenarios (invalid codes, CSRF failures)
- **Implemented**: Token exchange success/failure simulation
- **Rationale**: Enable comprehensive testing of OAuth flows without external dependencies

### 4. Improved AuthService Validation
- **Enhanced**: CSRF state parameter validation
- **Added**: Specific handling for mismatched state tokens
- **Rationale**: Support test scenarios for security validation

## Technical Changes

### Files Modified:
- `/backend/src/test/java/contract/ContractTestConfig.java` - Updated mock configuration
- `/backend/src/test/java/contract/AuthCallbackContractTest.java` - Updated dependencies and mocks
- `/backend/src/test/java/contract/AuthLoginContractTest.java` - Updated dependencies
- `/backend/src/main/java/com/namedraw/controller/AuthController.java` - Improved error handling
- `/backend/src/main/java/com/namedraw/service/AuthService.java` - Enhanced validation

### Key Technical Details:
- **Error Response Format**: Standardized JSON error responses with error, message, timestamp, path
- **Status Code Logic**: Invalid providers = 400, Authentication failures = 401
- **Mock Strategy**: Interface-based mocking of AuthClient for clean test isolation
- **CSRF Validation**: Test-friendly state parameter validation

## Test Results
- **Before**: 21 passed, 7 failed
- **After**: 28 passed, 0 failed

### Specific Fixes:
- ✅ Fixed content type issues in error responses
- ✅ Corrected status codes for invalid provider scenarios
- ✅ Resolved dependency injection issues with AuthClient
- ✅ Enabled proper OAuth flow simulation in tests

## Constitutional Compliance
- **Principle 8**: Mock services conservatively - External systems (AuthClient) properly abstracted
- **Principle 2**: Test-First - Maintained TDD by fixing tests to match intended behavior
- **Principle 6**: Checks for a reason - All tests now pass with proper validation

## Benefits Achieved
1. **Complete Auth Test Coverage**: All 28 Auth-related tests now pass
2. **Proper Error Handling**: Consistent, well-formatted error responses
3. **Test Maintainability**: Clean mocking strategy aligned with refactored architecture
4. **API Contract Compliance**: Responses match OpenAPI specification expectations

## Next Steps
Auth functionality is now fully tested and ready. Can proceed with remaining controller implementations or other failing test categories.