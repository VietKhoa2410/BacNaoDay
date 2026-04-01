# User login and logout

## Feature summary

Authenticated access using username and password, with a clear logout path so sessions end when the user chooses.

## Technical requirements

- Store credentials safely (hashed passwords; never store plaintext passwords).
- Session or token handling after login so subsequent requests know the user without re-entering password each time.
- Logout invalidates the session (or token) on the server side where applicable.
- Basic validation: reject empty or malformed credentials with clear, non-leaky error messages (avoid confirming whether a username exists if that is a security concern for the chosen auth model).

## Implementation steps

1. Define user identity model (unique username, password hash, timestamps if needed).
2. Implement registration only if in scope; otherwise seed or admin-create users per product decision.
3. Implement password hashing on write and verification on login.
4. Implement login endpoint or action: validate credentials, then issue session cookie or JWT per stack choice.
5. Implement logout: clear client state and revoke or expire server-side session.
6. Protect relation-page and person APIs so only the logged-in user accesses their data.
7. Add minimal tests for happy path login, wrong password, and logout invalidation.