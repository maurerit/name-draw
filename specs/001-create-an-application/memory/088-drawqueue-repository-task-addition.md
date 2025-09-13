# DrawQueue Repository Task Addition

**Date**: September 13, 2025  
**Interaction**: User identified missing DrawQueueRepository task

## User Request
The user noticed that our spec and task list was missing a task for a DrawQueueRepository. They pointed out that we need a way to insert and delete DrawQueue records as they're effectively lock records for concurrency control during draw operations.

## Analysis
Upon review of the tasks.md file, I confirmed that:
- T031 exists for DrawQueue entity model implementation ✓
- Phase 3.5 has repositories for User, Draw, Participation, and DrawnName ✓  
- DrawQueueRepository was missing ✗

The DrawQueue entity (from data-model.md) is designed as an in-memory table for atomic operations:
- Single field: `draw_id` (UUID, Primary Key)
- Purpose: Concurrency control for draw operations
- Lifecycle: Insert when draw starts, delete when complete
- Primary key constraint ensures only one draw per draw_id

## Decision Made
Added T035a to Phase 3.5: Repository Layer:
- **T035a [P] DrawQueueRepository JPA interface in `backend/src/main/java/repository/DrawQueueRepository.java` for atomic insert/delete operations**

## Rationale
- DrawQueue requires repository operations for insert/delete lock management
- Fits logically in Phase 3.5 with other repository interfaces  
- Marked [P] for parallel execution since it's a separate file
- Task ID T035a maintains sequence while fitting after existing T035
- Repository will enable atomic operations needed by DrawingService

## Files Modified
- `/home/maurerit/Projects/specs/name-draw/specs/001-create-an-application/tasks.md`: Added T035a task

## Next Steps
- T035a should be implemented before Phase 3.6 (Service Layer)
- DrawingService will depend on DrawQueueRepository for concurrency control