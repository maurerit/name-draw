# Memory File 028: Implement T020 Open Draw Contract Test

## User Request
Read the constitution and familiar yourself with our rules. Then implement T020 using the T019 test as a template to get your started. Log your memory file with number 028-

## AI Response Summary
Following the constitution principles of Test-First development and 2-module architecture, I implemented T020 (Contract test POST /draws/{drawId}/open) using T019 (DrawsJoinContractTest.java) as a template.

## Key Implementation Details

### API Contract Analysis
From `/specs/001-create-an-application/contracts/api.yaml`:
- **Endpoint**: POST /api/v1/draws/{drawId}/open
- **Purpose**: Transition draw from JOINING to OPEN state (creator only)
- **Security**: Requires Bearer authentication
- **Success Response**: 200 with Draw JSON
- **Error Cases**: 400 (wrong state/insufficient participants), 401 (unauthorized), 403 (not creator), 404 (not found)

### Test Structure (Based on T019 Template)
1. **Happy Path**: Valid creator opening draw in JOINING state → 200 with Draw JSON
2. **Authentication Tests**: No token → 401, Invalid token → 401
3. **Authorization Test**: Non-creator trying to open → 403
4. **Not Found Test**: Non-existent draw → 404
5. **Business Logic Tests**: 
   - Wrong state (already OPEN/ARCHIVED) → 400
   - Insufficient participants → 400
   - Invalid UUID format → 400

### Test Data Strategy
- Valid JWT token (same as T019)
- Different draw UUIDs for each scenario
- Following pattern: 550e8400-e29b-41d4-a716-44665544000X

## Files Created
- `/home/maurerit/Projects/specs/name-draw/backend/src/test/java/contract/DrawsOpenContractTest.java`

## Constitution Compliance
- ✅ **PRINCIPLE_2**: Test-First - Created contract test that will FAIL until implementation
- ✅ **PRINCIPLE_3**: Audit the AI - Documented in this memory file
- ✅ **PRINCIPLE_4**: No hallucination - Used actual API contract from api.yaml

## Next Steps
This test should FAIL when run (as required by TDD), then proceed to implement the actual DrawController.openDraw() method in Phase 3.7.
