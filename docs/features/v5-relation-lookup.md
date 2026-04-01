# Relation and relative-name lookup

## Feature summary

Given the user’s chosen “self” and another person on the same relation page, the product shows the relationship in understandable terms and the **Vietnamese kinship term** (how to call or refer to that relative).

## Technical requirements

- Inputs: `relationPageId`, self person, target person (both on same page).
- Kinship logic: either a rules engine over a family graph (edges between persons) or a simplified model—must be specified during design. Vietnamese kinship often depends on gender, side of family, relative age, and generation; capture required attributes on persons or edges.
- Output: human-readable relationship description + canonical Vietnamese term(s) where ambiguity exists (document how ties are broken or shown).
- Performance: lookups should be fast for typical family sizes; optional caching per (self, target) if graph is stable.

## Implementation steps

1. Finalize graph model: what edges exist (parent-child, marriage, etc.) and mandatory person attributes for Vietnamese rules.
2. Research or specify term tables / algorithm (could start with a limited subset of relations).
3. Implement path or rule resolution from self to target on the page’s graph.
4. Map resolved relation to display string + Vietnamese relative name.
5. API: accept page id + target person id; resolve self from stored page setting; return structured result for UI.
6. UI: select target person (after self is set); show result clearly.
7. Tests: known small trees with expected terms; error when self unset or persons disconnected if graph incomplete.
