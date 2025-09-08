# Decision Record: Self-Draw Loop Prevention in Small Groups

**Date**: September 8, 2025
**Prompt**: "How does the system handle multiple consecutive self-draws in very small groups?" - From a user perspective, this never happens. Once the user clicks the draw button the system will give them a name that is not their's. Simple as that so on the backend the system will continue until it does not pick the users name. If we get stuck in a loop, so if we have attempted more draws than there are users in the drawing then we should give up and show the user an error.

## User Request
Handle multiple consecutive self-draws in very small groups with loop prevention.

## Decision Made
- Users always receive a non-self name when clicking draw (transparent backend handling)
- Backend continues re-drawing until a non-self name is found
- System limits re-draw attempts to the number of participants to prevent infinite loops
- If maximum attempts exceeded, display error message to user
- This ensures good user experience while protecting against edge case scenarios

## Requirements Added
- **FR-035**: System MUST present users with a non-self name when they click draw, handling self-draws transparently in the backend
- **FR-036**: System MUST limit re-draw attempts to the number of participants in the draw to prevent infinite loops
- **FR-037**: System MUST display an error message if unable to find a non-self name after maximum re-draw attempts

## Acceptance Scenario Added
- Participant Flow #8: Error handling when draw cannot be completed after maximum attempts

## Edge Case Resolved
- Updated edge case to specify loop prevention mechanism and attempt limits

## Impact
- Protects system from infinite loops in extreme edge cases
- Maintains smooth user experience by hiding backend complexity
- Provides clear error feedback when draws cannot be completed
- Ensures system reliability even in unusual participant configurations
- Supports small family groups while preventing technical failures
