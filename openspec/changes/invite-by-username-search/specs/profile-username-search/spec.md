# Delta for profile-username-search

## ADDED Requirements

### R001: Remote username prefix search via Firestore

The repository SHALL support searching profiles by username prefix via Firestore. Search MUST query the `profiles` collection using `whereGreaterThanOrEqualTo("username", prefix)` + `whereLessThanOrEqualTo("username", prefix + "\uf8ff")` for prefix matching across ALL profiles. Ghost profiles (username=null) SHALL NOT appear in results. The user's own profile SHALL NOT appear in results.

The search requires a Firestore composite index on `profiles` collection, field `username` ASCENDING.

**Acceptance**: `searchByUsername(prefix)` returns `List<ProfileItem>` from Firestore filtered by prefix, excluding ghost and own profile.

#### Scenario: Prefix matches multiple profiles globally

- GIVEN Firestore profiles collection has usernames "naigel", "nico", "nadia", "miguel"
- WHEN search is invoked with prefix "n"
- THEN results contain "naigel", "nico", "nadia"
- AND "miguel" is excluded

#### Scenario: Mid-string prefix matches

- GIVEN Firestore profiles with usernames "naigel", "aigan", "miguel"
- WHEN search is invoked with prefix "aig"
- THEN results contain "naigel", "aigan"
- AND "miguel" is excluded

#### Scenario: No match

- GIVEN any profiles in Firestore
- WHEN search is invoked with prefix "xyz"
- THEN results are empty

#### Scenario: Ghost profile excluded

- GIVEN a ghost profile (username=null) and a profile with username="nadia"
- WHEN search is invoked with prefix "n"
- THEN results contain only "nadia"

#### Scenario: Own profile excluded

- GIVEN currentUserUid="abc", own profile (id="abc", username="naigel"), and another profile (username="nico")
- WHEN search is invoked with prefix "n"
- THEN results contain only "nico"

#### Scenario: Network unavailable

- GIVEN the device has no network connectivity
- WHEN search is invoked
- THEN the dialog SHALL display "Sin conexión" message

### R002: Debounced search trigger

The invite dialog SHALL debounce search input by 500ms. Search SHALL trigger only when input length is 2 or more characters. Network requests SHALL be cancelled when input changes before debounce completes.

**Acceptance**: Typing 2+ chars fires one Firestore query after 500ms idle; 1 char or empty input triggers no query.

#### Scenario: Debounce with rapid input

- GIVEN user types "n" then "a" within 500ms
- WHEN debounce timer fires
- THEN a single Firestore query executes for prefix "na"

#### Scenario: Single character ignored

- GIVEN user types only "n"
- WHEN input stabilizes after debounce
- THEN no search is triggered

#### Scenario: Empty input clears results

- GIVEN search results are visible from a previous query
- WHEN input is cleared to empty
- THEN results are cleared and prompt message shown

### R003: Profile selection resolves linkedEmail for invitation

Selecting a profile from search results SHALL resolve the profile's `linkedEmail` and send an invitation through the existing `sendInvitation()` flow. Profiles without `linkedEmail` SHALL NOT appear in results.

**Acceptance**: Tapping a profile with linkedEmail sends invitation; profiles without linkedEmail are absent from results.

#### Scenario: Profile with linked email selected

- GIVEN search results include profile "naigel" with linkedEmail="naigel@gmail.com"
- WHEN user selects "naigel"
- THEN invitation is sent to "naigel@gmail.com" via `sendInvitation()`

#### Scenario: Profile without linkedEmail excluded

- GIVEN a Firestore profile has linkedEmail=null and username="test"
- WHEN search results are computed
- THEN that profile SHALL NOT appear in results

### R004: Graceful empty, no-match, loading, and offline states

The dialog SHALL handle loading, empty cache, no-match, and offline states with clear messaging.

**Acceptance**: While loading, show indicator. Offline shows "Sin conexión". No matches shows "Sin resultados para '{query}'".

#### Scenario: Search in progress

- GIVEN user types "na" and Firestore query is pending
- WHEN debounce fires
- THEN dialog shows loading indicator

#### Scenario: No matches for query

- GIVEN user types "xyz" with zero matching profiles
- WHEN Firestore query completes
- THEN message "Sin resultados para 'xyz'" is displayed

#### Scenario: Device offline

- GIVEN device has no network connectivity
- WHEN user types 2+ characters
- THEN message "Sin conexión" is displayed and no Firestore query is attempted
