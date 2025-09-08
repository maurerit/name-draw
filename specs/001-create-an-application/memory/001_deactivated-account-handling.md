# Decision Record: Deactivated Social Account Handling

**Date**: September 8, 2025
**Prompt**: "What happens if a user's social account is deactivated after joining a draw?" if their social account is deactivated after joining the draw then we will have to remove them from the drawing. So, before the drawing starts we will need to 'ping' the social account used and ensure the account is active.

## User Request
Handle edge case where participants have deactivated social accounts after joining a draw.

## Decision Made
- System will validate all participant social accounts before transitioning draws from "joining" to "open" state
- Participants with deactivated accounts will be automatically removed from draws
- Organizers will be notified when participants are removed due to deactivated accounts

## Requirements Added
- **FR-024**: System MUST validate all participant social accounts before transitioning draws from "joining" to "open" state
- **FR-025**: System MUST automatically remove participants with deactivated social accounts from draws  
- **FR-026**: System MUST notify organizers when participants are removed due to deactivated accounts

## Acceptance Scenario Added
- Organizer Flow #7: Account validation notification scenario

## Edge Case Resolved
- Updated edge case list to reflect automatic removal of deactivated accounts

## Impact
- Ensures draw integrity by maintaining only active participants
- Provides transparency to organizers about participant changes
- Prevents technical issues during drawing phase
