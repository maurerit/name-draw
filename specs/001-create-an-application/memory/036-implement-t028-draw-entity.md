# Decision Memory 001: Implement T028 Draw Entity

**Date**: 2025-09-13  
**Interaction**: User asked to familiarize with constitution and implement T028

## User Request
```
Alright claude, familiarize yourself with our constitution and then implement T028
```

## Constitution Review
- **PRINCIPLE_1**: 2 Modules (Java Spring Boot backend + React + Vite frontend)
- **PRINCIPLE_2**: Test-First TDD mandatory (Red-Green-Refactor cycle)
- **PRINCIPLE_3**: Audit the AI (track decisions in memory files)
- **PRINCIPLE_4**: AI will not hallucinate decisions (keep record of prompts)
- **PRINCIPLE_5**: Boilerplate go away (use Lombok in entities/DTOs)

## Task Analysis
- T028: Draw entity model in `backend/src/main/java/com/namedraw/model/Draw.java`
- Phase 3.4: Entity Models (only after tests are failing)
- Requirements from data-model.md reviewed
- Existing User entity patterns examined

## Decisions Made

### 1. Entity Structure Design
- **Decision**: Follow User entity patterns with Lombok annotations
- **Rationale**: Constitutional requirement (PRINCIPLE_5) to minimize boilerplate
- **Implementation**: Used @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor

### 2. DrawState Enum Implementation
- **Decision**: Create nested enum with JOINING, OPEN, ARCHIVED values
- **Rationale**: Tests expect these exact string values based on contract test analysis
- **Implementation**: Used @Enumerated(EnumType.STRING) for database storage

### 3. Validation Strategy
- **Decision**: Use Bean Validation annotations for all constraints
- **Rationale**: Data model specification defines specific validation rules
- **Implementation**: @Size(min=1, max=100) for title, @Min(2)/@Max(30) for participants

### 4. Relationship Handling
- **Decision**: Temporarily comment out OneToMany relationships
- **Rationale**: Participation and DrawnName entities not yet created (T029, T030)
- **Implementation**: Added TODO comments for future uncomment

### 5. Business Logic Methods
- **Decision**: Implement lifecycle management methods in entity
- **Rationale**: Data model specifies state transitions and business rules
- **Implementation**: openForDrawing(), archive(), addParticipant(), etc.

### 6. Checkstyle Compliance
- **Decision**: Break long exception message into multiple lines
- **Rationale**: Build failed due to 100-character line limit violation
- **Implementation**: Split string literal across lines with proper indentation

## TDD Verification Results
- ✅ Entity compiles successfully
- ✅ Spring Boot application starts with entity
- ✅ Database migrations run successfully  
- ✅ Tests fail as expected (MockMvc bean missing - no controllers yet)

## Final State
- T028 marked as completed
- Draw entity ready for next phase (repositories/services/controllers)
- Tests properly failing in TDD red phase
- Code follows all constitutional principles