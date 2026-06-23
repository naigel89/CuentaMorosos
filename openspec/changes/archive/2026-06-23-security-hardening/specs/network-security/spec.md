# Network Security Specification

## Purpose

All network communication with Firebase services MUST be protected against man-in-the-middle (MITM) attacks via certificate pinning. Cleartext traffic policy MUST be explicitly declared.

## Requirements

### R001: Certificate Pinning for Firebase Domains

The app MUST declare a `network_security_config.xml` with `<pin-set>` entries for `*.googleapis.com` and `*.firebaseio.com`. The manifest MUST reference this config via `android:networkSecurityConfig`.

**Acceptance Criteria**: `app/src/main/res/xml/network_security_config.xml` exists with valid pin digests. `AndroidManifest.xml` includes `android:networkSecurityConfig="@xml/network_security_config"`.

#### Scenario: App connects to Firebase with pinned certificate

- **GIVEN** `network_security_config.xml` has valid SPKI pin digests for `*.googleapis.com`
- **WHEN** the app makes a Firestore or Firebase Auth request
- **THEN** the TLS handshake succeeds
- **AND** the connection proceeds normally

#### Scenario: MITM proxy with rogue certificate is rejected

- **GIVEN** a MITM proxy (e.g., mitmproxy, Charles) intercepts traffic with its own CA certificate
- **WHEN** the app attempts to connect to `firestore.googleapis.com`
- **THEN** the TLS handshake FAILS
- **AND** the connection is terminated before any data is exchanged

### R002: Backup Pins for Certificate Rotation

The pin set MUST include at least one backup pin from a different CA root to allow seamless certificate rotation.

**Acceptance Criteria**: `<pin-set>` contains at least 2 pin digests per domain. Changing certs in Firebase Console does not break connectivity.

#### Scenario: Primary certificate is rotated

- **GIVEN** Google rotates the `*.googleapis.com` TLS certificate to a new CA
- **WHEN** the app connects with the backup pin matching the new certificate
- **THEN** the connection succeeds
- **AND** users experience no downtime

### R003: Explicit Cleartext Traffic Policy

`network_security_config.xml` MUST explicitly declare `<base-config cleartextTrafficPermitted="false">`. The manifest MUST NOT contain `android:usesCleartextTraffic="true"`.

**Acceptance Criteria**: HTTP (non-HTTPS) requests from the app are blocked at the platform level.

#### Scenario: HTTP request is blocked

- **GIVEN** the app or a dependency attempts an HTTP (cleartext) request
- **WHEN** the platform processes the network call
- **THEN** the request is blocked with a `Cleartext HTTP traffic not permitted` error
