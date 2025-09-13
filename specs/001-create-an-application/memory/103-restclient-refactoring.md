# Decision 001: RestClient Refactoring for Mockability

**Date**: September 13, 2025  
**Task Context**: T040 - AuthController OAuth endpoints implementation  
**Decision Type**: Refactoring for testability

## User Request
```
Read the constitution, the spec and the tasks list and understanding we're still implementing T040 but we're doing some refactoring. Alright, we need to do some more work to refactor some stuff. RestClient chains are nearly impossible to mock so lets refactor RestClient calls out to *Client classes and then we can mock those classes instead of mocking the RestClient. So Let's create an AuthClient and refactor the two RestClient calls into this class. Then quit. I'll instruct you further after that.
```

## Problem Identified
- RestClient chains in AuthService are difficult to mock for unit testing
- Direct RestClient usage in AuthService violates testability principles
- Need to extract HTTP client operations to enable proper mocking

## Decision Made
Refactor RestClient calls from AuthService into a dedicated AuthClient abstraction:

1. **Created AuthClient interface** - Defines contracts for OAuth provider interactions
2. **Implemented RestClientAuthClient** - Concrete implementation using RestClient
3. **Updated AuthService** - Now depends on AuthClient instead of RestClient directly
4. **Maintained Spring configuration** - RestClient -> RestClientAuthClient -> AuthService

## Technical Changes

### Files Created:
- `/backend/src/main/java/com/namedraw/client/AuthClient.java`
- `/backend/src/main/java/com/namedraw/client/RestClientAuthClient.java`

### Files Modified:
- `/backend/src/main/java/com/namedraw/service/AuthService.java`

### Key Refactoring Details:
- **AuthService.exchangeCodeForToken()** - Simplified to delegate to `authClient.exchangeCodeForToken()`
- **AuthService.getUserInfoFromProvider()** - Simplified to delegate to `authClient.getUserInfo()`
- Removed HTTP-related imports and boilerplate from AuthService
- Extracted all RestClient logic to RestClientAuthClient

## Constitutional Compliance
- **Principle 8**: Mock services conservatively - External system interactions (RestClient) now properly abstracted for mocking
- **Principle 2**: Test-First - Enables proper unit testing of AuthService without HTTP calls

## Verification Results
- ✅ Code compiles without errors
- ✅ Existing tests run with same results (no new failures)
- ✅ Spring dependency injection properly configured
- ✅ Clean separation of concerns achieved

## Benefits Achieved
1. **Testability** - AuthService can now be unit tested with mocked AuthClient
2. **Maintainability** - HTTP client logic isolated to dedicated class
3. **Mockability** - Interface-based design enables easy mocking
4. **Constitution Compliance** - Follows conservative mocking principles

## Next Steps
Continue with T040 implementation using the newly refactored, testable AuthService.