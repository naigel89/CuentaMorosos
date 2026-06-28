## Exploration: fix-auth-register-crud-issues

### Current State

The auth flow lives in `MainActivity.kt` and the shared `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/` package. `SplashAuthScreen` is the splash+login screen; `RegisterScreen` and `ForgotPasswordScreen` are separate screens toggled via `AuthFlow`. Firebase Auth is used directly inside `MainActivity` with callbacks passed into the composables. Event CRUD flows through `EventDetailScreen` / `EventDetailViewModel` / `EventsViewModel` and the offline-first repositories backed by Firebase Firestore.

### Issue 1 — Login blocked by "PIN Verification failed" / "An internal error occurred"

**Findings**

- The strings "PIN Verification failed" and "An internal error occurred" do **not** exist in this codebase. They come from the Firebase Auth SDK / Play Services layer, not from app strings.
- Login is implemented in `MainActivity.onLogin` (`AuthFlow`, lines 450–459) using `auth.signInWithEmailAndPassword(...).addOnSuccessListener { ... }.addOnFailureListener { e -> onResult(e.localizedMessage ?: "Error al iniciar sesión") }`.
- Error handling is generic: any `FirebaseAuthInvalidCredentialsException`, `FirebaseAuthInvalidUserException`, `FirebaseNetworkException`, `FirebaseTooManyRequestsException`, or backend internal error is reduced to `e.localizedMessage`. On devices where the SDK emits "An internal error occurred", that raw text is shown in the snackbar.
- There is **no timeout / no retry**. If the Firebase task hangs (network blocked, Play Services issue), `isLoading` stays `true` forever and the user is stuck on the spinner.
- There is no dispatcher injection; the Firebase callbacks run on the internal thread pool, but the UI state mutation happens on the success/failure listener.
- No phone/2FA/PIN verification step is implemented in the app; the logcat message is almost certainly the Firebase Auth SDK reporting a backend/internal failure.

**Root cause hypothesis**

The generic error mapping swallows actionable failure types and the lack of timeout/retry makes transient Firebase/Play Services failures feel like a hard blocker.

**Affected files**
- `app/src/main/java/com/cuentamorosos/MainActivity.kt` — auth callbacks, error mapping, no timeout.
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/SplashAuthScreen.kt` — displays loading/error state; does not know about timeout.

**Recommended approach**
1. Map specific `FirebaseAuthException` subclasses to user-facing messages (invalid credentials, user not found, network, too-many-requests, generic).
2. Wrap the Firebase sign-in task in a `suspend` function with `withTimeoutOrNull` (e.g. 10–15 s) and a small exponential-backoff retry for transient network errors.
3. Reset `isLoading` and show a specific "network/auth service unreachable" message on timeout.
4. Keep the call in `MainActivity` (Firebase Auth is Android-only) but move the mapping logic into a small, testable object/function so the ViewModel/stateless screen can be unit-tested.

---

### Issue 2 — Register button hidden / unclickable on some devices

**Findings**

- `SplashAuthScreen` lays out the login form inside a top-aligned `Column` (`Arrangement.Top`) with a fixed `Spacer(Modifier.height(logoStartFromTopDp))` of **300 dp** before the logo, and the logo animation ends **40 dp** from the top.
- The email, password, login button, forgot-password link, and **register Row are at the very bottom** of the inner `Column`. There is **no `verticalScroll` modifier** on either the outer or inner `Column`.
- The inner `Column` is offset by the logo slide amount; during and after animation it sits below the logo. On small-screen devices or when the keyboard is open, the register `Row` can be pushed below the visible viewport and is unreachable.
- There is no `imePadding()`, `navigationBarsPadding()`, or `WindowInsets` handling; the button is not anchored to a `Scaffold` bottom bar, so it is not guaranteed to stay on screen.
- The register button is a `TextButton` inside a `Row` with no minimum touch target enforcement; it can be visually and physically small.

**Root cause hypothesis**

A fixed, non-scrollable layout with a 300 dp top spacer pushes the register call-to-action below the fold on small/keyboard-open screens. The button exists and is interactive but is outside the visible/clickable area.

**Affected files**
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/SplashAuthScreen.kt` — layout, scroll, insets, button placement.

**Recommended approach**
1. Wrap the form content in a scrollable container (`verticalScroll(rememberScrollState())`) or use a `Scaffold` with a pinned bottom bar for the register action.
2. Replace the bottom `Row` + `TextButton` with a clearly separated, minimum-height (`48 dp` touch target) register button that is always visible, e.g. a `TextButton` inside a `Surface`/`Box` with `imePadding()` and `navigationBarsPadding()`.
3. Consider adding `WindowInsets.ime` handling so the form scrolls when the keyboard opens.
4. Keep the splash animation but ensure the final layout is responsive and reachable.

---

### Issue 3 — Cannot edit or delete event participants and expenses

**Findings**

**Expenses**
- New expenses are created in `EventDetailScreen` (lines 202–207 and 270–275) with `EventExpenseItem(eventId, name, amountEuros, paidByProfileId = currentUid)`. **`createdByProfileId` is never set**, so it defaults to `""`.
- `FirestoreExpenseRepository.toMap()` writes `createdByProfileId` to Firestore (line 191). Because it is empty on creation, the document is created with `createdByProfileId = ""`.
- Firestore rules (`firestore.rules`, lines 74–75) require `resource.data.createdByProfileId == request.auth.uid` for `update`/`delete`. Since the stored value is empty, **no user** (not even the owner or creator) can edit or delete the expense.
- `PermissionEngine` allows `CONTRIBUTOR` to edit only when `expenseOwnerId == profileId` (line 64), but the UI passes `expense.paidByProfileId` as the owner ID (`EventDetailScreen` lines 630–631), not `createdByProfileId`. This is a second mismatch: the UI, the engine, and the rules are checking three different things.
- `PermissionEngine` allows only `OWNER` to delete expenses (line 65), while Firestore rules allow the creator. The UI therefore disables delete for contributors even when the rule would permit it (if `createdByProfileId` were set).

**Participants**
- `PermissionEngine.ManageParticipants` allows `OWNER` or `CONTRIBUTOR` (line 46), and the UI shows add/remove participant controls for both roles.
- Adding a participant calls `eventsViewModel.saveEvent(currentEvent.copy(participants = ...))` (`CuentaMorososApp.kt` lines 440–442), which triggers `FirestoreEventRepository.saveEvent()` — a full document `set()`.
- Firestore rules (`firestore.rules`, lines 56–57) allow `update` only if `request.auth.uid == resource.data.ownerId`. Contributors are rejected by the server.
- Removing a member calls `eventsViewModel.removeMember(...)` → `FirestoreEventRepository.removeMember(...)`, which also performs an `update()` on the event document; again, only the owner is allowed by the rules.
- Error handling in the ViewModels swallows failures (`runCatching { ... }.onFailure { LogSanitizer.log(...) }`), so the user sees no error message; the action just silently does nothing.

**Root cause hypothesis**

- Expense edit/delete is broken because `createdByProfileId` is not populated at creation, and the UI/engine/rules disagree on what identifies the "owner" of an expense.
- Participant edit/delete is broken because the client permission model grants the action to contributors while Firestore rules restrict event-document updates to owners, and failures are silently swallowed.

**Affected files**
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventDetailScreen.kt` — expense creation, edit/delete enabled flags.
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventDetailViewModel.kt` — expense save/delete wrappers.
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventsViewModel.kt` — `removeMember`, `saveEvent` wrappers.
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt` — `onAddProfileToEvent` wiring.
- `shared/src/commonMain/kotlin/com/cuentamorosos/model/PermissionEngine.kt` — role/action matrix for expenses and participants.
- `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt` — `EventExpenseItem.createdByProfileId` default.
- `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreExpenseRepository.kt` — expense serialization.
- `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreEventRepository.kt` — event update/remove member.
- `/home/naigel89/Documentos/Pruebas VS Code/CuentaMorosos/firestore.rules` — authorization rules.

**Recommended approach**

1. **Unify the expense ownership concept**: set `createdByProfileId = currentProfileId` when an expense is created in `EventDetailScreen`, and use `createdByProfileId` everywhere the UI/engine/rules check ownership.
2. **Align permission models**: decide whether contributors may edit/delete their own expenses and update both `PermissionEngine` and Firestore rules consistently. Current rules allow creator edit/delete; `PermissionEngine` blocks contributor delete. Pick one model and apply it to both layers.
3. **Participants**: either
   - Restrict participant management to owners in the UI (`PermissionEngine.ManageParticipants` → owner only) and keep the Firestore rule, or
   - Keep contributor management and implement participant mutations through a Cloud Function / subcollection so contributors do not need event-document `update` permission.
   The simpler, lower-risk fix is option A (UI + rules both owner-only).
4. **Surface errors**: change `runCatching` + silent log in ViewModels to emit user-facing errors (e.g. a `SharedFlow<String>` of error messages) so permission/server failures are visible.
5. **Validation**: add unit tests for `PermissionEngine` and for the `EventExpenseItem` creation path to ensure `createdByProfileId` is non-blank before remote save.

---

### Cross-cutting risks

- **Silent failures**: both auth and CRUD paths swallow exceptions or show raw SDK messages, making debugging hard.
- **Rule/client mismatch**: fixing only the client or only the rules will leave the other side out of sync.
- **Offline-first side effects**: `OfflineFirstExpenseRepository` enqueues pending operations on remote failure; if rules permanently reject an edit/delete, the queue will retry forever until the app realizes the operation is invalid.
- **Small-screen regressions**: any auth layout change must be verified on small devices and with the keyboard open.

### Ready for Proposal

**Yes.** The next phase should produce a proposal that scopes the work into three independent fixes: (1) auth error mapping + timeout, (2) login/register responsive layout, and (3) expense/participant ownership and permission alignment. Each fix should include Firestore rule updates and client-side changes together; they must not be split across proposals because the client and rules are tightly coupled for both issues 1 and 3.
