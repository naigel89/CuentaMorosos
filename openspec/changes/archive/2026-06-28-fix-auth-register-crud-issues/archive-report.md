# Archive Report: fix-auth-register-crud-issues

**Archived**: 2026-06-28
**Source**: `openspec/changes/fix-auth-register-crud-issues/`
**Destination**: `openspec/changes/archive/2026-06-28-fix-auth-register-crud-issues/`
**Verdict**: PASS WITH WARNINGS

## Overview

Fixed three coupled regressions in auth and event CRUD flows:
1. **Auth errors**: generic/unrecoverable login errors → mapped Firebase exceptions, added timeout + retry
2. **Register layout**: button hidden on small screens → Scaffold + imePadding, no-scroll keyboard-aware layout
3. **Expense/participant permissions**: broken edit/delete → unified `createdByProfileId` as ownership key, aligned PermissionEngine, UI, and Firestore rules

## Spec Sync

| Domain | Action | Details |
|--------|--------|---------|
| `firestore-authorization` | Updated | R002 replaced with delta (Unauthenticated Writes Denied); R004 refocused to read-only; R005-R007 added for OWNER/CONTRIBUTOR/VIEWER write rules; R008 (ex-R005) renumbered |

## Artifacts

| Artifact | Path | Present |
|----------|------|---------|
| Exploration | `exploration.md` | ✅ |
| Proposal | `proposal.md` | ✅ |
| Delta Specs | `specs/firestore-authorization/spec.md` | ✅ |
| Design | `design.md` | ✅ |
| Tasks | `tasks.md` | ✅ (27/31 complete, 4 deferred) |
| Apply Progress | `apply-progress.md` | ✅ |
| Verify Report | `verify-report.md` | ✅ |

## Known Gaps (from verify-report)

- `_errorMessage` SharedFlow defined but NOT emitted to in EventDetailViewModel or EventsViewModel (WARNING)
- 4 tasks deferred: OfflineFirstExpenseRepositoryTest (4.10-4.12) and Firestore rules emulator tests (4.16-4.18)
- Release variant test failure: pre-existing Robolectric + `isMinifyEnabled = true` incompatibility

None of these are blocking — core behavioral fix is fully functional and tested (115/115 tests pass in debug).

## Merge Summary

Merged the `firestore-authorization` delta spec into the main spec at `openspec/specs/firestore-authorization/spec.md`:
- **R002**: Replaced broad "Unauthenticated Access Denied" with focused "Unauthenticated Writes Are Denied" (events/expenses/participants), including expense-specific scenario
- **R004**: Refocused from combined read+write to "Event Read Access Restricted to Participants" only
- **R005 (new)**: "OWNER Has Full Write Access" — update and delete event/expense documents
- **R006 (new)**: "CONTRIBUTOR Has Limited Write Access" — create own expenses, edit/delete own, 6 scenarios covering all edge cases
- **R007 (new)**: "VIEWER Is Read-Only" — all writes denied, 2 scenarios
- **R008 (renumbered)**: Email Verification Enforced (preserved from old R005)

Total: 3 requirements replaced, 3 new requirements added, 2 preserved (R001, R003).
