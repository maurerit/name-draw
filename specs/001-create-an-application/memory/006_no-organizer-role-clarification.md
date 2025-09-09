# Decision Record: No Organizer Role - Everyone Can Create Draws

**Date**: September 8, 2025
**Prompt**: In the spec you mention that there is an organizer and participant flow. While I like this, because from a drawing perspective there is an organizer and participate, I don't like that this is called out like this. I don't want anyone thinking that we have an organizer role... EVERYONE can be an organizer

## User Request
Clarify that there are no special "organizer roles" - any user can create draws.

## Decision Made
- Remove any implication of special organizer roles or user types
- Change "organizer" terminology to "draw creator" or "user" throughout spec
- Emphasize that these are different user flows, not different user permissions
- Any authenticated user can create draws - no special role required
- This supports the family-focused approach where anyone can start a draw

## Specification Changes Made
- Updated Execution Flow: "organizers, participants" → "users (anyone can create or join draws)"
- Updated Primary User Stories: Both stories now use "As a user"
- Updated Acceptance Scenarios: "Organizer Flow" → "Draw Creator Flow"
- Updated Requirements: "organizers" → "draw creators" or "users" where appropriate
- Updated Key Entities: Draw created by "user" instead of "organizer"

## Impact
- Eliminates confusion about user roles or permissions
- Reinforces democratic approach where anyone can initiate draws
- Maintains functional clarity while removing hierarchical implications
- Supports family use case where any family member can start a draw
- Simplifies user model - only one user type with contextual capabilities
