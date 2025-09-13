# T037 DrawService Implementation Request

**Date**: September 13, 2025  
**Task**: T037 DrawService business logic implementation  
**User Request**: "Alright claude, familiarize yourself with our constitution the spec in specs/001-create-an-application and then implement T037. Don't forget to log our interaction and your decisions"

## Context Analysis

### Problem Identified
- T037 is marked incomplete in tasks.md
- DrawService.java file exists but is empty (previous implementation was lost)
- Need to re-implement according to constitution and spec requirements

### Constitution Requirements Reviewed
- **PRINCIPLE_2**: TDD mandatory - tests already written and failing
- **PRINCIPLE_3**: Must audit and log AI decisions in memory folder
- **PRINCIPLE_5**: Use Lombok to minimize boilerplate
- **2 Modules**: Java Spring Boot backend + React frontend

### Spec Requirements for DrawService
From functional requirements analysis:
- FR-003: Create draws with draw date
- FR-004: Draw state management (JOINING → OPEN → ARCHIVED)  
- FR-005: Join draws in JOINING state
- FR-006: Prevent joining OPEN/ARCHIVED draws
- FR-012: Creator can transition JOINING → OPEN
- FR-014: Max 30 participants per draw
- Auto-transition to OPEN at capacity (from edge cases)

### Dependencies Available
- UserService: ✅ Implemented (T036 complete)
- Models: ✅ User, Draw, Participation, DrawnName, DrawQueue
- Repositories: ✅ DrawRepository, ParticipationRepository, DrawQueueRepository
- Contract Tests: ✅ Written and failing (TDD compliance)

## Implementation Decisions

1. **Service Layer Architecture**: 
   - Use @Service, @Transactional for Spring integration
   - Inject required repositories via constructor (Lombok @RequiredArgsConstructor)
   - Add comprehensive validation for business rules

2. **Key Methods to Implement**:
   - `createDraw()`: Create with validation and default values
   - `findAllDraws()`, `findDrawById()`: Retrieval methods
   - `joinDraw()`: Participant management with capacity checks
   - `openDrawForDrawing()`: State transition with authorization
   - `updateDraw()`: Modification with state-dependent rules
   - Helper methods for validation and business logic

3. **Business Rule Enforcement**:
   - Capacity management (max 30 participants)
   - State transition validation 
   - Creator authorization checks
   - Auto-transition to OPEN at capacity
   - Comprehensive input validation

4. **Error Handling Strategy**:
   - IllegalArgumentException for validation failures
   - IllegalStateException for business rule violations
   - SecurityException for authorization failures

## Expected Outcome
- DrawService.java fully implemented with all required business logic
- Compilation successful 
- Ready for controller layer implementation (T040-T043)
- Contract tests will pass once controllers are implemented

**Decision Authority**: AI implementation based on specification analysis  
**Next Steps**: Implement DrawService, verify compilation, update tasks.md