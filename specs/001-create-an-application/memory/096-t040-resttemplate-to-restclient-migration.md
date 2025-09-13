# 096-T040-RestTemplate-to-RestClient-Migration

## User Request
You asked me to fix T040 (AuthController) with two specific issues:
1. Replace RestTemplate with RestClient (as RestTemplate is deprecated)
2. Fix contract test mocking strategy - tests should mock external dependencies (repositories, HTTP clients) not services

## My Response

### 1. Replaced RestTemplate with RestClient in AuthService
- Updated imports from `RestTemplate` to `RestClient`
- Changed field declaration from inline `new RestTemplate()` to injected `RestClient`
- Updated HTTP calls to use RestClient's fluent API:
  - Token exchange: `restClient.post().uri().headers().body().retrieve().toEntity()`
  - User info: `restClient.get().uri().headers().retrieve().toEntity()`
- Created `HttpClientConfig` class to provide RestClient bean

### 2. Fixed Contract Test Mocking Strategy
- Created `ContractTestConfig` to provide mock beans for external dependencies
- Added `spring.main.allow-bean-definition-overriding=true` in test properties
- Made `HttpClientConfig` exclude test profile with `@Profile("!test")`
- Updated test to mock `ClientRegistrationRepository` and `RestClient` instead of services
- Added proper setup methods for Google and Facebook ClientRegistration mocks

### Results
- All 5 tests in AuthLoginContractTest now pass
- Tests properly mock external dependencies while testing controller/service integration
- RestClient modernization completed

## Decisions Made
1. **RestClient Migration**: Migrated from deprecated RestTemplate to modern RestClient API
2. **Test Configuration Strategy**: Used profile-based exclusion and bean overriding for test mocking
3. **External Dependency Mocking**: Contract tests now properly mock external systems (HTTP clients, repositories) instead of internal services

## Files Modified
- `/backend/src/main/java/com/namedraw/service/AuthService.java`
- `/backend/src/main/java/com/namedraw/config/HttpClientConfig.java` (created)
- `/backend/src/test/java/contract/ContractTestConfig.java` (created)
- `/backend/src/test/java/contract/AuthLoginContractTest.java`
- `/backend/src/test/resources/application-test.properties` (created)

## Constitution Compliance
- ✅ PRINCIPLE_2: Tests were already written and failing before implementation
- ✅ PRINCIPLE_3: Properly logging this interaction in memory
- ✅ PRINCIPLE_6: All code style checks passed during build

**Date**: 2025-09-13
**Status**: T040 fixes completed successfully