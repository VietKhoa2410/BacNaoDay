# Relation pages (create and name)

## Feature summary

Each relation page is a named workspace for one family tree or relation scenario. Users create pages and assign names so they can open the right context later.

## Technical requirements

- Relation pages belong to a single user (owner).
- Persist page name and metadata (e.g., created date) as needed for listing and sorting.
- Enforce ownership: users can only read/write their own pages.
- Name constraints: reasonable length, optional uniqueness per user to avoid confusion (product choice).

## Implementation steps

1. Define data model: relation page with `id`, `ownerUserId`, `name`, and audit fields if required.
2. Implement create: authenticated user submits a name; server stores page linked to user.
3. Implement list: return the current user’s pages for navigation/home.
4. Implement optional update/rename if product expands; otherwise omit until requested.
5. Wire UI: create form, list or dashboard, navigation into a selected page.
6. Add tests for create, list, and cross-user access denial.
