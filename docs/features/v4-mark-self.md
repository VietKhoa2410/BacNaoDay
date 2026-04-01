# Mark “your” person

## Feature summary

On each relation page, the user designates one person node as themselves. This anchor is required before “my relation to X” lookups make sense.

## Technical requirements

- At most one “self” person per relation page per user (or globally per page if pages are single-user).
- Persist the choice (e.g., `selfPersonId` on the page or a flag on the person row).
- Changing “self” should be allowed; re-run or invalidate any cached lookup results tied to the old anchor if caching exists.
- Validate that the chosen person belongs to the same relation page.

## Implementation steps

1. Add storage for self reference on relation page (or equivalent).
2. Implement API to set/clear self: check page ownership and that `personId` is on that page.
3. Enforce single self: unset previous when setting a new one.
4. UI: explicit control (e.g., “This is me”) on a person node; show current self clearly.
5. Block or guide relation lookup (v5) until self is set, if desired for UX.
6. Tests: set self, change self, reject person from another page, reject unauthenticated or wrong owner.
