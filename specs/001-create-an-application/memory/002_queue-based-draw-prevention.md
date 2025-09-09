# Decision Record: Queue-Based Draw Prevention

**Date**: September 8, 2025
**Prompt**: "How does the system prevent users from drawing multiple names?" - this will be handled by keeping a tally of who has drawn in the backend. So let's do it this way and have a 'queue table'. One record per user per drawing and when they draw their name their their 'queue record' is removed.

## User Request
Prevent users from drawing multiple names by implementing a queue-based tracking system.

## Decision Made
- Implement a DrawQueue entity to track drawing eligibility
- One queue record per user per draw
- Queue record is removed when user successfully draws a name
- Only participants with active queue records can draw names

## Requirements Added
- **FR-027**: System MUST maintain a queue record for each participant in each draw to track drawing eligibility
- **FR-028**: System MUST remove a participant's queue record when they successfully draw a name
- **FR-029**: System MUST only allow participants with active queue records to draw names

## Entity Added
- **DrawQueue**: Tracks participants eligible to draw names; one record per user per draw, removed after successful draw

## Acceptance Scenario Added
- Participant Flow #6: Queue record removal prevents subsequent draws

## Edge Case Resolved
- Updated edge case to explain queue-based prevention mechanism

## Impact
- Provides robust backend enforcement of single-draw rule
- Enables clear tracking of who has/hasn't drawn
- Supports organizer visibility into draw progress
- Prevents race conditions in concurrent access scenarios
