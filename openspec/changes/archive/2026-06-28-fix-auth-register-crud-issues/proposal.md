# Proposal: fix-auth-register-crud-issues

## Intent

Fix three coupled regressions in the auth and event CRUD flows: generic/unrecoverable login errors, a register button that falls off-screen, and broken edit/delete permissions for expenses and participants.

## Scope

### In Scope
- Login error mapping, timeout, and retry for transient Firebase failures.
- Responsive, no-scroll login/register layout that keeps the register CTA visible with the keyboard open.
- Expense ownership fix: populate `createdByProfileId` and use it consistently in UI, `PermissionEngine`, and Firestore rules.
- Participant permission fix: restrict add/edit/remove to OWNER, hide/disable controls for CONTRIBUTOR/VIEWER, and add role badges in the participant list.
- Surface ViewModel errors to the UI instead of silently logging them.

### Out of Scope
- Email verification flow changes.
- Cloud Functions or invitation-flow redesign.
- Offline-first retry queue overhaul.
- Receipt feature functional changes (only visibility permissions).

## Capabilities

### New Capabilities
- `auth/login`: Map Firebase Auth exceptions to user-facing messages; add timeout/retry.
- `auth/register-screens`: No-scroll responsive layout with keyboard-aware, always-visible register CTA.
- `event/expense-management`: Set `createdByProfileId`; align edit/delete checks across UI, engine, and rules.
- `event/participant-management`: Owner-only participant mutations, per-role UI enablement, role badges.

### Modified Capabilities
- `firestore-authorization`: Update event/expense write rules to match the OWNER / CONTRIBUTOR / VIEWER matrix.

## Approach

1. **Auth errors**: Wrap `signInWithEmailAndPassword` in a suspend mapper with `withTimeoutOrNull` and exponential-backoff retry; keep Firebase calls in `MainActivity` but extract mapping for tests.
2. **Register layout**: Replace the fixed top spacer with a `Scaffold` that pins the register action above the keyboard using `WindowInsets.ime`/`navigationBarsPadding`; no scrolling; minimum 48 dp touch target.
3. **Permissions**: Define OWNER / CONTRIBUTOR / VIEWER in `PermissionEngine`; update Firestore rules; set `createdByProfileId` on expense creation; use it as the ownership key everywhere; restrict participant mutations to OWNER.
4. **UX**: Add role chips/badges in the participant list; emit user-facing errors from ViewModels via `SharedFlow`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/com/cuentamorosos/MainActivity.kt` | Modified | Login timeout, retry, error mapping. |
| `shared/.../ui/auth/SplashAuthScreen.kt` | Modified | No-scroll responsive layout, inset handling. |
| `shared/.../model/PermissionEngine.kt` | Modified | OWNER/CONTRIBUTOR/VIEWER matrix. |
| `shared/.../ui/EventDetailScreen.kt` | Modified | Expense creation, edit/delete enable flags, role badges. |
| `shared/.../ui/EventDetailViewModel.kt` | Modified | Error emission, expense save/delete wrappers. |
| `shared/.../ui/EventsViewModel.kt` | Modified | Error emission, member removal wrappers. |
| `shared/.../data/repository/FirestoreExpenseRepository.kt` | Modified | Serialize ownership field consistently. |
| `firestore.rules` | Modified | Role-based write rules for events/expenses. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Client and rules drift out of sync again | Med | Update `PermissionEngine`, UI flags, and rules in the same PR; add unit tests. |
| Silent failures persist | Med | Convert `runCatching` logs to `SharedFlow` errors and verify in specs. |
| Offline queue retries invalid operations forever | Low | Reject local ops that fail permission rules and remove from queue. |
| Small-screen/layout regression | Med | Verify on small emulator with keyboard open. |

## Rollback Plan

Revert the single feature commit (or restore `firestore.rules` from the previous deployed revision). Re-deploy rules. Existing expenses created with empty `createdByProfileId` will remain un-editable until a data migration or manual repair; the spec phase will decide whether a one-time backfill is required.

## Dependencies

- Firebase Auth / Play Services behavior on target devices.
- Firestore rules deployment access.
- Existing `PermissionEngine` and repository unit tests.

## Success Criteria

- [ ] Login shows friendly, actionable errors; never hangs on spinner; retries transient failures.
- [ ] Register button is visible and tappable without scrolling on small screens and with keyboard open.
- [ ] OWNER can manage participants, expenses, calculations, event, and receipt.
- [ ] CONTRIBUTOR can add/assign expenses, run calculations, view receipt; cannot add/edit/remove participants.
- [ ] VIEWER is read-only for everything including receipt and participant list.
- [ ] Participant list shows OWNER / CONTRIBUTOR / VIEWER role labels or icons.
- [ ] Expense edit/delete uses `createdByProfileId` and works for the creator when rules allow.
- [ ] ViewModel errors surface in the UI instead of being silently logged.
- [ ] New and existing unit tests pass.
