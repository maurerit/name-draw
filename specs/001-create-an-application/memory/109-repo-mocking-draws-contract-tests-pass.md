# Interaction Log: Repository mocking for Draws tests (no service mocks)

Date: 2025-09-18
Spec: 001-create-an-application

## User Prompt
- Why is this service method mocked? You should never mock service methods in integration tests. Find a way to let the service run and mock the required repository calls.
- Remove hardcoded JWT tokens from Draws contract tests and use TestTokenGenerator.
- Make all Draws* contract tests pass.
- Log this interaction according to our constitution.

## Final Response
Refactored the Draws contract tests to run real service logic while mocking only persistence and external clients. Replaced custom bean-based repo mock with a proper context-wide mock, stubbed `save(...)` behavior, and verified the entire `*Draws*` suite passes. Logged outcomes here per the constitution.

## actions taken

- Replaced custom `@Bean @Primary` Mockito factory for `DrawRepository` with `@MockBean` in `ContractTestConfig` so Spring Test replaces the JPA bean across the context.
- Ensured `DrawsCreateContractTest` stubs `drawRepository.save(any(Draw))` to emulate persistence (assigns `id` and `createdAt`).
- Kept `@MockBean` for `UserRepository`, `AuthClient`, and `ClientRegistrationRepository` to avoid external dependencies.
- Verified the controller runs with the real `DrawService`; removed any service-level mocks.
- Confirmed `TestTokenGenerator` is used for JWTs in tests (no hardcoded tokens).

## test results

- Single test run: `contract.DrawsCreateContractTest` — PASS (13/13)
- Full suite run: `mvn test -Dtest="*Draws*"` — PASS
  - Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
- Checkstyle: 0 violations

## notes

- Root cause of earlier failures was a custom bean-based repo mock that didn’t fully replace the Spring Data proxy, causing calls to hit the real JPA repository. Using `@MockBean` guarantees replacement in the context, so stubs are honored.
- `@MockBean` is deprecated in Spring Boot 3.4+ but continues to work in 3.5.x; plan a migration when Boot provides an official replacement pattern.
- Logs show controller → service flow executing normally, with repository interactions intercepted by Mockito.

## completion summary

- Services are not mocked in integration tests; only repositories/external clients are mocked.
- JWTs are generated via `TestTokenGenerator`.
- All Draws* contract tests pass under the new setup.

## follow-ups

- Replace deprecated `@MockBean` usage when upgrading beyond Boot 3.5.x (e.g., switch to recommended test-time bean replacement strategy).
