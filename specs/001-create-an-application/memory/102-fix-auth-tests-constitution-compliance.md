# Fix Auth Test Failures Following Constitution

**Date**: 2025-09-13  
**Session**: Fix all tests identified by `mvn test -Dtest="*Auth*"`  
**User Request**: "Now, read the constitution and follow it word for word. And fix all tests identified by this maven command: `mvn test -Dtest="*Auth*"`. There are edits by you that I interrupted in the ContractTestConfig where you started to mock services. I then updated my constitution to put in a principle to mock services conservatively meaning NEVER in an integration test which is the type of tests we're trying to get to pass."

## Constitution Principles Applied

### PRINCIPLE_8: Mock services conservatively
- **Constitutional Requirement**: "During integration tests, services should never be mocked, only external systems should be mocked such as Repository interactions or External System interactions through RestClient, WebClient or RestTemplate."
- **Previous Violation**: Agent had started mocking `UserService` (internal service) 
- **Corrective Action**: Removed `UserService` mock and replaced with `UserRepository` mock (external system)

### PRINCIPLE_7: Test Data will be structured properly
- **Current Issue**: Tests expecting 200 status getting 401 "User not found"
- **Root Cause**: UserRepository mock not properly configured to return test user

### PRINCIPLE_3: Audit the AI  
- **Compliance**: Creating this memory file as required

## Decisions Made

### 1. Constitution Compliance Correction
- **Decision**: Replace UserService mock with UserRepository mock 
- **Rationale**: Follow PRINCIPLE_8 - only mock external systems in integration tests
- **Implementation**: Updated ContractTestConfig.java to mock UserRepository.findById() instead of UserService.findById()

### 2. Test Data Configuration  
- **Current State**: TestTokenGenerator creates valid JWT tokens for user ID "123e4567-e89b-12d3-a456-426614174000"
- **Current Issue**: UserRepository mock returns test user, but service still reports "User not found"
- **Analysis**: UserService.findById() calls userRepository.findById(id) then filters by isActive=true

## Test Failures Analysis

### AuthRefreshContractTest Results
- Tests expecting 200 status getting 401 "User not found"
- JWT token parsing works correctly
- UserRepository mock configured but service still can't find user
- Test user has isActive=true so filtering should pass

### Other Auth Test Classes
- AuthLogoutContractTest: Multiple 500 errors instead of expected 401/200
- AuthCallbackContractTest: Multiple 400 errors instead of expected 200/401

## Current Status

- ✅ Constitution compliance corrected (removed service mock)
- ✅ UserRepository mock implemented  
- ❌ Tests still failing - UserService not finding mocked user
- ❌ Need to investigate mock configuration or add additional repository method mocks

## Next Steps Required

1. Debug why UserRepository mock is not working in integration test context
2. Check if additional repository methods need mocking  
3. Verify mock bean configuration is being applied correctly
4. Fix remaining test data issues following PRINCIPLE_7

## Files Modified

- `/backend/src/test/java/contract/ContractTestConfig.java`: Replaced UserService mock with UserRepository mock