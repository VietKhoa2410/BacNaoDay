# Person nodes on a relation page

## Feature summary

Within a relation page, users add person nodes representing individuals. These nodes are the building blocks for marking “you” and for relationship lookups.

## Technical requirements

- Each person node is tied to exactly one relation page and inherits access from that page’s owner.
- Minimum fields: stable identifier, display label (e.g., name or nickname), and order or position if the UI uses a canvas or list.
- If relationships between nodes (parent/child, spouse, etc.) are required for kinship calculation, define that in the kinship feature; this feature covers **creating and listing** nodes on the page.

## Implementation steps

1. Define person model: `id`, `relationPageId`, display name, optional fields (gender, birth order) only if kinship rules need them—derive from kinship design.
2. Implement create person under a page; validate page ownership.
3. Implement list persons for a page for rendering and selection flows.
4. If the product needs graph edges (e.g., parent-child), add a separate small model or extend in coordination with v5 kinship logic.
5. UI: add-person control on the relation page; show nodes in the chosen layout.
6. Tests: create on owned page, reject on others’ pages, list correctness.
