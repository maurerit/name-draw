# Decision Record: Remove Draw Button After Drawing

**Date**: September 8, 2025
**Prompt**: This scenario really shouldn't happen as the 'draw button' should be removed after the name has been drawn.

## User Request
Clarify that draw button should be removed after a user has drawn a name, making certain scenarios impossible.

## Decision Made
- Draw button/functionality is removed after a participant successfully draws a name
- This prevents users from attempting to draw multiple times
- Eliminates unrealistic scenarios from acceptance criteria
- Provides clear UI indication that drawing is complete for that user
- Simplifies user experience by removing impossible actions

## Specification Changes Made
- Removed Participant Flow scenario #4: "Given I try to draw a name, When I have already drawn one, Then I see my previously drawn name instead"
- Removed Participant Flow scenario #6: "Given I have successfully drawn a name, When I try to draw again, Then the system prevents me because my queue record has been removed"
- Renumbered remaining scenarios appropriately
- Added FR-038: System MUST remove draw button/functionality after successful draw

## Requirements Added
- **FR-038**: System MUST remove draw button/functionality after a participant has successfully drawn a name

## Impact
- Improves user experience by preventing confusion
- Eliminates impossible user actions from interface
- Provides clear visual feedback about draw completion status
- Reduces need for backend prevention logic (though queue system still needed for data integrity)
- Simplifies acceptance criteria by removing unrealistic scenarios
