# Memory Entry 026: Implement T018 - Update Draw Contract Test

## User Request
Read the constitution and familiar yourself with our rules. Then implement T018 using the T016 test as a template to get your started. Log your memory file with number 026-

## Constitution Understanding
- **2 Modules**: Java Spring Boot backend + React Vite frontend
- **Test-First**: TDD mandatory - Tests written → User approved → Tests fail → Then implement
- **Audit the AI**: Track decisions in memory files numbered 001-999 
- **No hallucinated decisions**: Only document actual decisions made

## Analysis of T016 Template (DrawsCreateContractTest.java)
- Tests POST /draws endpoint with comprehensive scenarios
- Structure: Package `contract`, Spring Boot test setup, MockMvc usage
- Test patterns: Valid requests, missing auth, invalid tokens, validation errors
- Follows Given-When-Then pattern in comments
- Uses MockMvcRequestBuilders.post and comprehensive assertions

## T018 Requirements from tasks.md
- Contract test PUT /draws/{drawId} in `backend/src/test/java/contract/DrawsUpdateContractTest.java`
- Must be written first (TDD) and MUST FAIL before implementation

## API Contract Analysis from api.yaml
- **PUT /draws/{drawId}** endpoint  
- **Requires Bearer authentication**
- **Request body**: UpdateDrawRequest with fields: title, description, drawDate, maxParticipants (all optional)
- **Responses**: 200 (success), 400 (invalid), 401 (unauthorized), 403 (forbidden), 404 (not found)
- **Business rules**: Only creator can update, only in JOINING state

## Implementation Decision
Create contract test following T016 pattern with these test scenarios:
1. Valid update request (full and partial)
2. Authentication failures (no token, invalid token)
3. Authorization failures (not creator, wrong state)
4. Validation failures (field constraints)
5. Resource not found
6. Malformed requests

## Final Response
Implemented DrawsUpdateContractTest.java following TDD principles and constitution requirements.
