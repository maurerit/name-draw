# Decision Record 015: Functional Requirements Renumbering Fix

## User Prompt
"It seems we have removed a Functional Requirement or a number was skipped. let's rectify that by renumbering all of the FR's after 22. So 24 becomes 23, 25 to 24 etc. Also, update the memory files to reflect these changes"

## Agent Response
Corrected the functional requirements numbering gap where FR-023 was missing:

**Original numbering issue**: FR-022 → FR-024 (missing FR-023)
**Fixed numbering**: Sequential from FR-001 through FR-037

**Renumbering changes made**:
- FR-024 → FR-023: Account validation before draw opens
- FR-025 → FR-024: Remove deactivated accounts  
- FR-026 → FR-025: Notify creators of removed participants
- FR-027 → FR-026: Maintain queue records for participants
- FR-028 → FR-027: Remove queue record after successful draw
- FR-029 → FR-028: Only allow draws with active queue records
- FR-030 → FR-029: Auto-transition at 30 participants
- FR-031 → FR-030: View drawn names in archived draws
- FR-032 → FR-031: Non-participants view archived participant lists
- FR-033 → FR-032: Prevent actions in archived draws
- FR-034 → FR-033: Handle self-draws transparently
- FR-035 → FR-034: Limit re-draw attempts
- FR-036 → FR-035: Error message for failed draws
- FR-037 → FR-036: Remove draw button after successful draw
- FR-038 → FR-037: Prevent concurrent draws (one hand in bag)

**Memory files updated**:
- Updated decision record 013 to note numbering gap creation
- Updated decision record 014 to reflect FR-037 instead of FR-038

## Files Modified
- `/specs/001-create-an-application/spec.md`: Renumbered FR-024 through FR-038 to close numbering gap
- `/specs/001-create-an-application/memory/013-ui-prevents-impossible-scenarios.md`: Updated to note numbering gap creation
- `/specs/001-create-an-application/memory/014-one-hand-in-bag-concurrency.md`: Updated FR reference from 038 to 037
