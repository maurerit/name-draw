# Decision Record: Automatic Draw Start at Maximum Capacity

**Date**: September 8, 2025
**Prompt**: "What happens when a user tries to join a draw that has reached the 30 participant limit?" - Once the 30 person limit has been reached then we will remove any possible interaction from the drawing and the drawing will automatically start.

## User Request
Handle behavior when draws reach the 30 participant maximum limit.

## Decision Made
- Draws automatically transition from "joining" to "open" state when 30 participants are reached
- No further joining is allowed once maximum capacity is reached
- System provides clear messaging to users attempting to join full draws
- This prevents organizers from needing to manually start draws that reach capacity

## Requirements Added
- **FR-030**: System MUST automatically transition draws from "joining" to "open" state when 30 participants are reached
- **FR-031**: System MUST prevent any joining attempts once a draw has reached maximum capacity and automatically started

## Acceptance Scenarios Added
- Organizer Flow #8: Automatic transition when 30 participants join
- Non-Participant Viewing #3: Clear messaging for full draw attempts

## Edge Case Resolved
- Updated edge case to reflect automatic transition behavior

## Impact
- Eliminates manual intervention requirement for popular draws
- Provides immediate draw start for maximum engagement
- Ensures fair treatment of all participants (no preferential late joining)
- Clear user feedback for capacity limits
- Streamlines organizer experience by reducing manual management
