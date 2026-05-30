## Verification Report

**Change**: profile-account-settings
**Version**: 1.0
**Mode**: Standard (no TDD)

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 24 |
| Tasks complete | 24 |
| Tasks incomplete | 0 |

### Build & Tests Execution
**Build**: ❌ Failed
```text
Could not find io.coil-kt.coil3:coil-network:3.0.4
```
The artifact `io.coil-kt.coil3:coil-network` does not exist in version 3.0.4. In Coil 3.x, network dependencies were renamed:
- Use `io.coil-kt.coil3:coil-network-okhttp:3.0.4` (Android/JVM)
- Use `io.coil-kt.coil3:coil-network-ktor3:3.0.4` (KMP)

This is a build configuration defect introduced in task 1.6.

**Tests**: ❌ Cannot execute — build fails before test phase.

### Spec Compliance Matrix
| Requirement | Scenario | Test | Result |
|------------|----------|------|--------|
| R-001 | ProfileItem 4 new fields with null defaults | `ProfileItemTest > backward compatibility` | ✅ COMPLIANT |
| R-001 | All 11 fields populate correctly | `CuentaMorososLocalStoreProfileTest > profile JSON roundtrip preserves all fields` | ✅ COMPLIANT |
| R-002 | displayNameFor: customName > displayName > name | `ProfileItemTest > displayNameFor resolution (4 tests)` | ✅ COMPLIANT |
| R-003 | updateDisplayName(profileId, displayName): Result<Unit> | Source inspection | ✅ COMPLIANT |
| R-004 | uploadPhoto/deletePhoto | Source inspection | ⚠️ PARTIAL (photoUrl not actually uploaded — TODO in ViewModel.updatePhoto mocked) |
| R-005 | checkUsernameAvailability + setUsername | Source inspection | ✅ COMPLIANT |
| R-006 | setCustomName subcollection + changePassword | Source inspection | ✅ COMPLIANT |
| R-007 | ProfileAvatar photoUrl -> AsyncImage | Source inspection | ⚠️ PARTIAL (shows "📷" placeholder, not actual AsyncImage) |
| R-008 | ProfileCard: customName > displayName > name + @username | Source inspection | ✅ COMPLIANT |
| R-009 | Compress 256x256, upload Storage, persist URL | Source inspection | ❌ UNTESTED (photo upload not implemented, ViewModel.updatePhoto is stub) |
| R-010 | Photo deletion removes Storage file | Source inspection | ❌ UNTESTED (not implemented) |
| R-011 | Username 500ms debounced Firestore query | `AccountViewModelTest` | ✅ COMPLIANT |
| R-012 | @username in ProfileCard | Source inspection | ✅ COMPLIANT |
| R-013 | CustomNames subcollection, resolution chain | Source inspection | ✅ COMPLIANT |
| R-014 | Sync via Firestore snapshot listeners | Source inspection | ⚠️ PARTIAL (sync via OfflineFirstProfileRepository's observeProfiles, not explicit customNames listener) |
| R-015 | "Mi perfil" entry in SettingsScreen | Source inspection | ✅ COMPLIANT |
| R-016 | 3 sub-screens via AnimatedContent | Source inspection | ✅ COMPLIANT |
| R-017 | AccountViewModel with StateFlow per sub-screen | Source inspection | ✅ COMPLIANT |
| R-018 | Password change with EmailAuthProvider reauth | Source inspection | ✅ COMPLIANT |
| R-019 | requiresRecentLogin handling | `AccountViewModelPasswordTest > password change with reauth error shows error state` | ⚠️ PARTIAL (shows Error state but does NOT show specific "requiresRecentLogin" prompt/dialog per R-019 spec) |

**Compliance summary**: 12/19 fully compliant, 4 partial, 2 untested, 1 build-blocked

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| ProfileItem model | ✅ Implemented | 4 new fields with correct defaults, displayNameFor() resolution works |
| SQLDelight migration | ✅ Implemented | 2.sqm with 4 ALTER TABLE, CachedProfile.sq with 12-param upsert |
| CuentaMorososLocalStore | ✅ Implemented | loadProfiles/saveProfiles handle all 4 new fields + customNames JSON parsing |
| FirebaseUserSyncManager | ✅ Implemented | ensureOwnProfile writes new fields with merge |
| ProfileRepository interface | ✅ Implemented | 6 new methods with correct signatures |
| FirestoreProfileRepository | ✅ Implemented | toMap/toProfileItem, ghost enforcement, customNames subcollection |
| OfflineFirstProfileRepository | ✅ Implemented | 12-param upsert, ghost-aware mapping, 6 new method implementations |
| ProfileAvatar | ⚠️ Partial | Accepts photoUrl param but shows "📷" placeholder instead of loading image |
| ProfileCard | ✅ Implemented | displayNameFor() + @username display |
| SettingsScreen | ✅ Implemented | ProfileSettingsSection with tap → showAccountScreen |
| AccountViewModel | ✅ Implemented | 3 sub-screens, debounced username, password change |
| AccountScreen | ✅ Implemented | MainMenu + NamePhotoScreen + UsernameScreen + SecurityScreen via AnimatedContent |
| CuentaMorososApp | ✅ Implemented | showAccountScreen overlay wired |
| AppViewModelFactory | ✅ Implemented | AccountViewModel registered |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| customNames type Map<String, String> = emptyMap() | ✅ Yes | Design said nullable, impl uses non-null emptyMap() — matches apply-progress deviation |
| 12-param SQLDelight upsert | ✅ Yes | Matches design |
| Ghost profiles force null photoUrl/username | ✅ Yes | Enforced in both Firestore and offline repos |
| ProfileCard uses displayNameFor(currentUid) | ✅ Yes | |
| AccountViewModel own sub-screen state | ✅ Yes | |
| AnimatedContent overlay pattern | ✅ Yes | Same as EventDetailScreen |
| Username debounce 500ms via Flow.debounce | ✅ Yes | |
| Photo upload with 256x256 compression | ❌ No | Not implemented — ViewModel.updatePhoto is a stub |
| Coil AsyncImage for photoUrl | ⚠️ Partial | Not implemented in shared ProfileAvatar (uses "📷" placeholder) |
| expect/actual for Coil | ❌ No | No platform-specific implementation found |

### Issues Found

**CRITICAL**:
1. **Build fails — missing dependency**: `io.coil-kt.coil3:coil-network:3.0.4` does not exist. Coil 3.x renamed network artifacts. Must use `io.coil-kt.coil3:coil-network-okhttp:3.0.4` or `io.coil-kt.coil3:coil-network-ktor3:3.0.4`. Blocks all test execution.
2. **R-009 Photo upload not implemented**: `AccountViewModel.updatePhoto()` is a stub (`_state.value = AccountUiState.Success("Foto actualizada (simulado)")`). No bitmap compression, no Firebase Storage upload, no URL persistence.
3. **R-010 Photo deletion not implemented**: No implementation found for deleting photos from Storage.
4. **Coil AsyncImage not implemented**: `ProfileAvatar` shows a "📷" placeholder text when `photoUrl` is non-null. The spec requires `AsyncImage` via Coil. No `expect`/`actual` pattern exists for platform-specific image loading.

**WARNING**:
1. **R-019 requiresRecentLogin handling**: The spec says "prompt sign-out and re-sign-in with clear message." The current implementation returns `PasswordState.Error("requiresRecentLogin")` as a generic error — no specific dialog or sign-out prompt.
2. **ProfileAvatar placeholder**: The current "📷" emoji visual is acceptable as a first iteration but does not meet the spec requirement for `AsyncImage`/Coil.
3. **FirebaseUserSyncManager uses Firebase Android SDK**: `ensureOwnProfile` uses Android-specific `com.google.firebase.firestore.SetOptions` but the project is multi-platform. The gitlive KMP SDK is used everywhere else.
4. **customNames serialization uses `=` delimiter**: The pipe-delimited format `key=value|key2=value2` breaks if custom names contain `=` or `|` characters. A JSON-based encoding would be safer.

**SUGGESTION**:
1. Replace `coil-network` with `coil-network-okhttp` for Android and add `coil-network-ktor3` for KMP in a platform-specific source set.
2. Implement an `expect fun AsyncImage(...)` composable and provide the Coil `actual` in AndroidMain.
3. Add actual image loading in `ProfileAvatar` instead of the placeholder emoji.
4. Change customNames serialization to use JSON format in SQLDelight (serialize as JSON string instead of pipe-delimited) to handle special characters.
5. Add Firestore security rules for `avatar` paths to enforce own-UID access at the storage level.
6. The `FirebaseUserSyncManager.ensureOwnProfile` writes `"photoUrl" to ""` and `"username" to ""` but these should be nullable fields — writing empty strings means the document has non-null empty values, conflicting with the merge-safe pattern.

### Verdict
**FAIL**

Build does not compile due to a non-existent dependency (`coil-network:3.0.4`). Additionally, 2 spec requirements (R-009 photo upload, R-010 photo deletion) are unimplemented, and 1 core design decision (Coil AsyncImage for ProfileAvatar) is not met.
