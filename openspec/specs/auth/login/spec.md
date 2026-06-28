# Domain: auth/login

## Purpose
Define login behavior including Firebase Auth error mapping, request timeout, and transient-failure retry so users receive clear, actionable feedback.

## Requirements

### Requirement: Firebase Auth errors are mapped to user-facing messages
The system MUST translate every Firebase Auth exception into a concise, actionable message displayed in the login UI.

#### Scenario: Invalid email format
- GIVEN the user enters a malformed email address
- WHEN they tap the login button
- THEN the system MUST display an "Invalid email" message and MUST NOT call Firebase Auth

#### Scenario: Wrong password
- GIVEN the user enters a registered email and an incorrect password
- WHEN they submit the login form
- THEN the system MUST display "Incorrect email or password"

#### Scenario: Account disabled
- GIVEN the Firebase account has been disabled
- WHEN the user attempts login
- THEN the system MUST display "Account disabled. Contact support."

### Requirement: Login request has a bounded timeout
The system MUST abandon a login attempt that exceeds a defined timeout and surface a user-facing timeout message.

#### Scenario: Firebase does not respond
- GIVEN the network is reachable but Firebase Auth does not respond
- WHEN the login timeout elapses
- THEN the system MUST stop the loading indicator and display "Login is taking too long. Try again."

### Requirement: Transient failures are retried
The system SHOULD retry transient network failures with exponential backoff and MUST surface the failure if all retries exhaust.

#### Scenario: Retry succeeds
- GIVEN a transient network failure occurs on the first login attempt
- WHEN the system retries and the second attempt succeeds
- THEN the user MUST be logged in without manual re-entry

#### Scenario: All retries fail
- GIVEN transient network failures persist through all retry attempts
- WHEN retries exhaust
- THEN the system MUST display "Network error. Please check your connection and try again."

### Requirement: Login shows a loading indicator
The system MUST display a loading indicator while a login attempt is in progress and MUST disable the login button during that time.

#### Scenario: User taps login
- GIVEN the user enters valid credentials
- WHEN they tap the login button
- THEN the system MUST show a loading indicator and MUST disable the login button until the attempt completes

#### Scenario: Login attempt finishes
- GIVEN a login attempt is in progress
- WHEN it completes (success or failure)
- THEN the system MUST hide the loading indicator and re-enable the login button

### Requirement: Login errors are surfaced in the UI
The system MUST emit login errors to a UI-observable channel and MUST NOT log them silently.

#### Scenario: Silent failure becomes visible
- GIVEN a login error occurs
- WHEN the ViewModel processes the error
- THEN the UI MUST display the mapped message and the loading state MUST clear
