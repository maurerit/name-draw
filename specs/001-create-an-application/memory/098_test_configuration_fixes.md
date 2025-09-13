# Test Configuration Fixes

## Issue Summary
All contract tests were failing with ApplicationContext errors due to incorrect test configuration.

## Root Causes Identified
1. Contract tests used `@AutoConfigureWebMvc` instead of `@AutoConfigureMockMvc`
2. Contract tests did not include `ContractTestConfig.class` in their `@SpringBootTest` classes array
3. AuthRefreshContractTest referenced non-existent `com.namedraw.config.TestConfig.class`

## Fixes Applied
1. **Annotation Fix**: Replaced `@AutoConfigureWebMvc` with `@AutoConfigureMockMvc` in all contract tests
2. **Configuration Import**: Updated all contract tests to include `ContractTestConfig.class`:
   ```java
   @SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
   ```
3. **AuthRefreshContractTest**: Fixed to use `ContractTestConfig.class` instead of non-existent `TestConfig.class`

## Files Modified
- All contract test files in `/src/test/java/contract/` directory
- Fixed 12 contract test classes to include proper configuration

## Results
- ApplicationContext errors reduced from 119 to 16
- Contract tests now run successfully (failing due to unimplemented endpoints, which is expected)
- Only integration tests still have ApplicationContext issues

## Contract Test Configuration Pattern
Working pattern established:
```java
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
```

## Next Steps
- Integration tests still need similar configuration fixes
- All tests can pass once controllers are properly implemented