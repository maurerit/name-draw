# Decision Record 013: UI Prevents Impossible Join Scenarios

## User Prompt
"This scenario should never happen as the drawing should be listed as viewable only. Joining a started drawing should not be possible as all of these interactions should be removed from the presentation"

## Agent Response
Updated the specification to reflect that the UI should prevent users from attempting to join draws that are already open or archived, rather than handling these as error scenarios:

1. **Removed impossible user scenarios**: Eliminated acceptance scenarios where users try to join open/full draws since UI should prevent these attempts
2. **Updated functional requirements**: Removed FR-021 and FR-031 about messaging for impossible scenarios; this created a gap in numbering (missing FR-023) that was later corrected  
3. **Clarified edge cases**: Updated language to emphasize UI prevention rather than error handling
4. **Maintained core functionality**: Kept all legitimate business logic about automatic transitions and viewing capabilities

The spec now correctly reflects that presentation layer removes joining functionality when draws are open/archived, making error scenarios impossible rather than just handled.

## Files Modified
- `/specs/001-create-an-application/spec.md`: Removed impossible scenarios, updated functional requirements, clarified edge cases
