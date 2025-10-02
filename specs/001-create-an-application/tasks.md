# Tasks: Name Draw Application with Social Login

**Input**: Design documents from `/specs/001-create-an-application/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Java Spring Boot + React + Vite, H2 Database, Docker
   → Structure: Web application (backend + frontend)
2. Load design documents:
   → data-model.md: User, Draw, Participation, DrawnName, DrawQueue entities
   → contracts/api.yaml: 13 API endpoints across Authentication, Users, Draws, Drawing
   → research.md: H2 database, OAuth2, JWT, in-memory queue decisions
   → quickstart.md: 3 test scenarios (creator flow, participant flow, edge cases)
3. Generate tasks by category:
   → Setup: Spring Boot + React projects, dependencies, Docker
   → Tests: contract tests for 13 endpoints, integration tests for 3 scenarios
   → Core: 5 entity models, 4 services, 5 controllers, queue management
   → Integration: OAuth, JWT, database, Docker compose
   → Polish: unit tests, performance, documentation
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Tests before implementation (TDD)
   → Models before services before controllers
5. Generated 42 numbered tasks (T001-T042)
6. Dependencies mapped: Setup → Tests → Core → Integration → Polish
7. Parallel execution examples provided
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Backend**: `backend/src/main/java/com/namedraw/`
- **Frontend**: `frontend/src/`
- **Tests Backend**: `backend/src/test/java/com/namedraw/`
- **Tests Frontend**: `frontend/tests/`
- **Docker**: Repository root

## Phase 3.1: Project Setup
- [x] T001 Create Spring Boot backend project structure in `backend/` with Maven, H2, Spring Security OAuth2, Spring Data JPA dependencies
- [x] T002 Create React + Vite frontend project structure in `frontend/` with TypeScript, OAuth integration, testing dependencies
- [x] T003 [P] Configure backend linting (Checkstyle) and formatting (Google Java Format) in `backend/pom.xml`
- [x] T004 [P] Configure frontend linting (ESLint) and formatting (Prettier) in `frontend/.eslintrc.js` and `frontend/.prettierrc`
- [x] T005 [P] Create Docker configuration files: `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`

## Phase 3.2: Database and Configuration
- [x] T006 [P] Create H2 database configuration in `backend/src/main/resources/application.yml` and `application-dev.yml`
- [x] T007 [P] Create Flyway migration scripts in `backend/src/main/resources/db/migration/` for all entities
- [x] T008 [P] Configure OAuth2 providers (Facebook, Google) in `backend/src/main/resources/application.yml`

## Phase 3.3: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.4
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Authentication Contract Tests
- [x] T009 [P] Contract test GET /auth/login/{provider} in `backend/src/test/java/contract/AuthLoginContractTest.java`
- [x] T010 [P] Contract test POST /auth/callback/{provider} in `backend/src/test/java/contract/AuthCallbackContractTest.java`
- [x] T011 [P] Contract test POST /auth/refresh in `backend/src/test/java/contract/AuthRefreshContractTest.java`
- [x] T012 [P] Contract test POST /auth/logout in `backend/src/test/java/contract/AuthLogoutContractTest.java`

### User Management Contract Tests
- [x] T013 [P] Contract test GET /users/me in `backend/src/test/java/contract/UserProfileContractTest.java`
- [x] T014 [P] Contract test GET /users/me/draws in `backend/src/test/java/contract/UserDrawsContractTest.java`

### Draw Management Contract Tests
- [x] T015 [P] Contract test GET /draws in `backend/src/test/java/contract/DrawsListContractTest.java`
- [x] T016 [P] Contract test POST /draws in `backend/src/test/java/contract/DrawsCreateContractTest.java`
- [x] T017 [P] Contract test GET /draws/{drawId} in `backend/src/test/java/contract/DrawsGetContractTest.java`
- [x] T018 [P] Contract test PUT /draws/{drawId} in `backend/src/test/java/contract/DrawsUpdateContractTest.java`
- [x] T019 [P] Contract test POST /draws/{drawId}/join in `backend/src/test/java/contract/DrawsJoinContractTest.java`
- [x] T020 [P] Contract test POST /draws/{drawId}/open in `backend/src/test/java/contract/DrawsOpenContractTest.java`

### Drawing Operation Contract Tests
- [x] T021 [P] Contract test POST /draws/{drawId}/draw in `backend/src/test/java/contract/DrawingPerformContractTest.java`
- [x] T022 [P] Contract test GET /draws/{drawId}/results in `backend/src/test/java/contract/DrawingResultsContractTest.java`
- [x] T023 [P] Contract test GET /draws/{drawId}/my-result in `backend/src/test/java/contract/DrawingMyResultContractTest.java`

### Integration Tests
- [x] T024 [P] Integration test draw creator flow in `backend/src/test/java/integration/DrawCreatorFlowTest.java`
- [x] T025 [P] Integration test participant flow in `backend/src/test/java/integration/ParticipantFlowTest.java`
- [x] T026 [P] Integration test edge cases (self-draws, concurrent access) in `backend/src/test/java/integration/EdgeCasesTest.java`

## Phase 3.4: Entity Models (ONLY after tests are failing)
- [x] T027 [P] User entity model in `backend/src/main/java/model/User.java`
- [x] T028 [P] Draw entity model in `backend/src/main/java/model/Draw.java`
- [x] T029 [P] Participation entity model in `backend/src/main/java/model/Participation.java`
- [x] T030 [P] DrawnName entity model in `backend/src/main/java/model/DrawnName.java`
- [x] T031 [P] DrawQueue entity model (in-memory) in `backend/src/main/java/model/DrawQueue.java`

## Phase 3.5: Repository Layer
- [x] T032 [P] UserRepository JPA interface in `backend/src/main/java/repository/UserRepository.java`
- [x] T033 [P] DrawRepository JPA interface in `backend/src/main/java/repository/DrawRepository.java`
- [x] T034 [P] ParticipationRepository JPA interface in `backend/src/main/java/repository/ParticipationRepository.java`
- [x] T035 [P] DrawnNameRepository JPA interface in `backend/src/main/java/repository/DrawnNameRepository.java`
- [x] T035a [P] DrawQueueRepository JPA interface in `backend/src/main/java/repository/DrawQueueRepository.java` for atomic insert/delete operations

## Phase 3.6: Service Layer
- [x] T036 UserService business logic in `backend/src/main/java/com/namedraw/service/UserService.java`
- [x] T037 DrawService business logic (depends on UserService) in `backend/src/main/java/com/namedraw/service/DrawService.java`
- [x] T038 DrawingService with queue management (depends on DrawService) in `backend/src/main/java/com/namedraw/service/DrawingService.java`
- [x] T039 AuthService OAuth integration (depends on UserService) in `backend/src/main/java/com/namedraw/service/AuthService.java`

## Phase 3.7: Controller Layer
- [x] T040 AuthController OAuth endpoints in `backend/src/main/java/controller/AuthController.java`
- [x] T041 UserController profile endpoints in `backend/src/main/java/controller/UserController.java`
- [x] T042 DrawController management endpoints in `backend/src/main/java/controller/DrawController.java`
- [x] T043 DrawingController drawing operations in `backend/src/main/java/controller/DrawingController.java`

## Phase 3.8: Frontend Core Components
- [x] T044 [P] Authentication service with OAuth integration in `frontend/src/services/authService.ts`
- [x] T045 [P] API client service in `frontend/src/services/apiService.ts`
- [x] T046 [P] Draw management service in `frontend/src/services/drawService.ts`
- [x] T047 [P] Login page component in `frontend/src/pages/LoginPage.tsx`
- [x] T048 [P] Dashboard page component in `frontend/src/pages/DashboardPage.tsx`
- [x] T049 [P] Create draw page component in `frontend/src/pages/CreateDrawPage.tsx`
- [x] T050 [P] Draw detail page component in `frontend/src/pages/DrawDetailPage.tsx`
- [x] T051 [P] Drawing page component in `frontend/src/pages/DrawingPage.tsx`

## Phase 3.9: Frontend Component Tests
- [x] T052 [P] Login page component test in `frontend/tests/components/LoginPage.test.tsx`
- [x] T053 [P] Dashboard page component test in `frontend/tests/components/DashboardPage.test.tsx`
- [x] T054 [P] Create draw page component test in `frontend/tests/components/CreateDrawPage.test.tsx`
- [x] T055 [P] Draw detail page component test in `frontend/tests/components/DrawDetailPage.test.tsx`
- [x] T056 [P] Drawing page component test in `frontend/tests/components/DrawingPage.test.tsx`

## Phase 3.10: Integration and Security
- [x] T057 JWT token service integration in `backend/src/main/java/com/namedraw/security/JwtTokenService.java`
- [x] T058 Spring Security configuration in `backend/src/main/java/com/namedraw/security/SecurityConfig.java`
- [x] T059 CORS configuration in `backend/src/main/java/com/namedraw/config/CorsConfig.java`
- [x] T060 Error handling and validation in `backend/src/main/java/com/namedraw/controller/GlobalExceptionHandler.java`

## Phase 3.11: Docker and Deployment
- [ ] T061 [P] Backend Docker image optimization in `backend/Dockerfile`
- [ ] T062 [P] Frontend Docker image optimization in `frontend/Dockerfile`
- [ ] T063 Docker compose development setup in `docker-compose.yml`
- [ ] T064 Docker compose production setup with Let's Encrypt in `docker-compose.prod.yml`

## Phase 3.12: Performance and Polish
- [ ] T065 [P] Backend unit tests for service layer in `backend/src/test/java/unit/`
- [ ] T066 [P] Frontend unit tests for service layer in `frontend/tests/unit/`
- [ ] T067 [P] API performance tests (<200ms) in `backend/src/test/java/performance/ApiPerformanceTest.java`
- [ ] T068 [P] Frontend E2E tests for complete user flows in `frontend/tests/e2e/`
- [ ] T069 [P] Update OpenAPI documentation in `specs/001-create-an-application/contracts/api.yaml`
- [ ] T070 [P] Update README with setup instructions
- [ ] T071 Code review and refactoring (remove duplication, optimize)
- [ ] T072 Run complete test suite and manual testing scenarios from quickstart.md

## Dependencies
- **Setup first**: T001-T008 must complete before any other phases
- **Tests before implementation**: T009-T026 must complete and FAIL before T027-T043
- **Models before services**: T027-T035 before T036-T039
- **Services before controllers**: T036-T039 before T040-T043
- **Backend core before frontend**: T027-T043 before T044-T051
- **Components before tests**: T044-T051 before T052-T056
- **Core before integration**: T027-T056 before T057-T060
- **Everything before polish**: T001-T064 before T065-T072

## Parallel Execution Examples

### Contract Tests (Phase 3.3)
```bash
# Launch authentication contract tests together:
Task: "Contract test GET /auth/login/{provider} in backend/src/test/java/contract/AuthLoginContractTest.java"
Task: "Contract test POST /auth/callback/{provider} in backend/src/test/java/contract/AuthCallbackContractTest.java"
Task: "Contract test POST /auth/refresh in backend/src/test/java/contract/AuthRefreshContractTest.java"
Task: "Contract test POST /auth/logout in backend/src/test/java/contract/AuthLogoutContractTest.java"
```

### Entity Models (Phase 3.4)
```bash
# Launch all entity models together:
Task: "User entity model in backend/src/main/java/model/User.java"
Task: "Draw entity model in backend/src/main/java/model/Draw.java"
Task: "Participation entity model in backend/src/main/java/model/Participation.java"
Task: "DrawnName entity model in backend/src/main/java/model/DrawnName.java"
Task: "DrawQueue entity model in backend/src/main/java/model/DrawQueue.java"
```

### Frontend Components (Phase 3.8)
```bash
# Launch frontend service layer together:
Task: "Authentication service with OAuth integration in frontend/src/services/authService.ts"
Task: "API client service in frontend/src/services/apiService.ts"
Task: "Draw management service in frontend/src/services/drawService.ts"
```

## Notes
- **[P] tasks**: Different files, no dependencies, can run in parallel
- **TDD Critical**: Verify all contract and integration tests FAIL before implementing
- **Commit strategy**: Commit after each task completion
- **H2 Database**: Will be created automatically, use H2 console for debugging
- **OAuth Setup**: Requires Facebook and Google developer accounts
- **Docker**: Development setup uses HTTP, production uses HTTPS with Let's Encrypt

## Validation Checklist
- [ ] All 13 API endpoints have contract tests
- [ ] All 5 entities have model implementations
- [ ] All 3 user scenarios have integration tests
- [ ] OAuth integration works for both Facebook and Google
- [ ] In-memory queue prevents concurrent draws
- [ ] Docker setup works for both development and production
- [ ] Performance requirements met (<200ms API response)
- [ ] All constitutional requirements followed (TDD, decision auditing)
