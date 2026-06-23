# Tasks: invite-by-username-search

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~206 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | pending |
| Decision needed before apply | No |

## Phases

### Phase 1: Repository Layer (Firestore Query)
- [x] 1.1 Add `suspend fun searchByUsername(prefix: String): List<ProfileItem>` to `shared/.../data/repository/ProfileRepository.kt`
- [x] 1.2 Implement in `shared/.../data/repository/FirestoreProfileRepository.kt`: Firestore prefix query with `whereGreaterThanOrEqualTo` + `whereLessThanOrEqualTo`, `limit(20)`, lowercase, filter ghost+no-linkedEmail
- [x] 1.3 Add stub in `shared/.../data/repository/OfflineFirstProfileRepository.kt` throwing `UnsupportedOperationException`

### Phase 2: Repository Tests (strict TDD)
- [x] 2.1 Write tests in `shared/src/commonTest/.../FirestoreProfileRepositoryTest.kt`: prefix match, mid-string match, no-match, ghost exclusion, own-profile exclusion, empty prefix, limit=20 (covers spec R001)
- [x] 2.2 Run `./gradlew test` — confirm Phase 1 implementation passes all Phase 2 tests

### Phase 3: UI — InviteMemberDialog Redesign
- [x] 3.1 Define `SearchState` sealed class (Idle, Loading, Results, Empty, Offline) in `shared/.../ui/EventDetailScreen.kt`
- [x] 3.2 Redesign `InviteMemberDialog` signature: add `profileRepository: ProfileRepository`, `currentUserUid: String?` params
- [x] 3.3 Implement `LaunchedEffect` debounce: 500ms, filter input >= 2 chars, cancel on input change
- [x] 3.4 Build dropdown UI: `OutlinedTextField` with `@` prefix + `LazyColumn` with `ProfileAvatar` + name + @username rows
- [x] 3.5 Implement all `SearchState` render branches: Idle prompt, Loading spinner, Offline message, Empty message, Results list (spec R004)
- [x] 3.6 Wire profile selection → `linkedEmail` → `onInvite(email)` (spec R003)
- [x] 3.7 Update dialog invocation at `EventDetailScreen.kt` + pass `profileRepository` down from `CuentaMorososApp.kt`

### Phase 4: UI Tests + Verification
- [x] 4.1 Create `shared/src/commonTest/.../InviteMemberDialogTest.kt`: debounce behavior, SearchState transitions, self-exclusion, no-linkedEmail exclusion, empty/no-match/offline messages
- [x] 4.2 Run `./gradlew test` and verify all scenarios from spec pass (R001-R004)
- [x] 4.3 Create Firestore composite index on `profiles.username ASC` (Firebase Console or `firestore.indexes.json`)
- [ ] 4.4 Manual smoke test: open event → invite → type @username → select → verify invitation flow

## Definition of Done
- All [ ] checked
- `./gradlew test` passes all new tests
- All spec scenarios verified (R001 ×6, R002 ×3, R003 ×2, R004 ×3)
- Self and ghost profiles excluded from results
- Dialog shows graceful loading/empty/no-match/offline messages
- Firestore composite index documented for deployment
