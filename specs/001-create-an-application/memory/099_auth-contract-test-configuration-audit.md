# Auth Contract Test Configuration Audit
**Date**: 2025-09-13  
**Context**: Fixing auth contract tests to comply with PRINCIPLE_8 (no service mocking in integration tests)

## Problem Statement
Auth contract tests were failing with "User not found" errors after removing UserService mock per constitution PRINCIPLE_8. The tests use runtime-generated JWT tokens but the ApplicationContext couldn't find users because:

1. UserService mock was removed (correctly, per PRINCIPLE_8)
2. UserRepository mock was attempted but not being picked up by ApplicationContext 
3. Tests need a way to provide user data without violating constitution principles

## Decisions Made

### 1. Mock Strategy Update
- **Decision**: Replace @MockBean (deprecated) with manual @Bean/@Primary UserRepository mock
- **Rationale**: PRINCIPLE_8 allows mocking external systems (repositories) but not services
- **Implementation**: Created manual mock bean in ContractTestConfig with findById() and findByEmail() methods

### 2. Test Data Approach
- **Decision**: Use static test user matching JWT token claims
- **Rationale**: Real JWTs require consistent user IDs between token generation and mock setup
- **Implementation**: TestTokenGenerator uses ContractTestConfig.getTestUser() for consistent UUID

### 3. Constitution Compliance
- **PRINCIPLE_8**: "Never mock services in integration tests" - ✅ Removed UserService mock
- **PRINCIPLE_3**: "Document all interactions" - ✅ This memory file
- **Repository mocking**: ✅ Allowed as external system mock

## Technical Implementation
```java
// ContractTestConfig.java - Manual UserRepository mock
@Bean
@Primary
public UserRepository userRepository() {
  UserRepository mockRepo = Mockito.mock(UserRepository.class);
  User testUser = getTestUser();
  Mockito.when(mockRepo.findByEmail("test@example.com"))
         .thenReturn(Optional.of(testUser));
  Mockito.when(mockRepo.findById(testUser.getId()))
         .thenReturn(Optional.of(testUser));
  return mockRepo;
}
```

## Outstanding Issues
1. UserRepository mock still not being picked up by ApplicationContext
2. Tests continue to fail with "User not found" despite proper mock setup
3. Need to investigate why @Bean/@Primary doesn't override default repository bean

## Next Steps
1. Investigate Spring test context configuration
2. Consider @TestConfiguration vs @Configuration
3. Verify mock is being registered properly in ApplicationContext
4. Ensure no real UserRepository bean is overriding our mock

## Key Learning
Integration test mocking is complex when following strict constitution principles. External system mocks (repositories) are allowed but require careful Spring context configuration to ensure they're properly injected.