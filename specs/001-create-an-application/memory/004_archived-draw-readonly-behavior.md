# Decision Record: Archived Draw Read-Only Behavior

**Date**: September 8, 2025
**Prompt**: "What happens when the draw date passes and the draw is archived?" - No further interactions can be done on this drawing. Users may see who they drew and any associated data to that drawing and outside users can see who participated but no further actions can be taken on that drawing

## User Request
Define behavior for archived draws once the draw date has passed.

## Decision Made
- Archived draws become completely read-only with no interactive actions allowed
- Participants can view their drawn names and associated draw data
- Non-participants can view participant lists but not draw results
- No joining, drawing, or state changes are permitted in archived draws
- This preserves historical data while preventing any modifications

## Requirements Added
- **FR-032**: System MUST allow participants to view their drawn names in archived draws
- **FR-033**: System MUST allow non-participants to view participant lists in archived draws  
- **FR-034**: System MUST prevent all interactive actions (joining, drawing, state changes) in archived draws

## Acceptance Scenarios Added
- Organizer Flow #9: Read-only viewing of archived draws with full visibility
- Participant Flow #7: View drawn names but no actions in archived draws
- Non-Participant Viewing #4: Limited visibility and no actions in archived draws

## Edge Case Resolved
- Updated edge case to specify read-only behavior with clear visibility rules

## Impact
- Preserves historical draw data for future reference
- Provides clear closure to completed draws
- Maintains transparency while preventing unwanted modifications
- Supports family memories by keeping draw results accessible
- Eliminates confusion about what actions are available on old draws
