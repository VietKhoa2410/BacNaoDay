---
name: analyze
description: >-
  Analyzes business and product documentation to derive discrete features and optional
  technical implementation notes, then writes markdown outputs when the user wants
  persistence. Use when the user invokes /analyze, asks to analyze docs for business
  or features, or to break documentation into implementable features.
---

# Analyze (documentation → business & features)

## When to apply

- User says `/analyze`, “analyze the docs”, “break down business into features”, or points at documentation to analyze.
- User wants findings **saved** to files (explicitly or implied by project conventions below).

## Inputs

1. **Documentation**: Paths or pasted content the user provides. If none, default to project sources such as `docs/features/Business.md` and related `docs/` files the user cares about.
2. **Scope**: Confirm whether output is **chat-only** or **saved** (see Saving).

Read the referenced files fully before analyzing.

## Analysis workflow

1. **Business summary**  
   Extract goals, actors, constraints, and scope from the docs. Stay aligned with what the documentation actually states; flag gaps or contradictions.

2. **Feature breakdown**  
   Split the business into **small, independent features** where possible so each can be implemented without entangling others.
   - Short, plain-language description per feature (what users get / what problem it solves).
   - **Do not** put implementation technology in this feature list (no frameworks, DBs, APIs unless the source doc is explicitly technical).

3. **Technical follow-up (optional)**  
   If the user or project rules ask for per-feature tech planning: for each feature, outline technical requirements, suggested implementation steps, and dependencies. Keep this **separate** from the high-level feature list.

## Saving

**If the user asks to save**, or project `docs/features/Business.md` (or equivalent) specifies output paths, write files accordingly.

**This repository’s convention** (from `docs/features/Business.md` when present):

| Output | Purpose |
|--------|---------|
| `docs/features/analyze.md` | Aggregated business analysis + feature list (non-technical descriptions). |
| `docs/features/v{number}-{feature-name}.md` | Per-feature technical requirements and implementation steps (e.g. `docs/features/v1-simple-login.md`). |

Use lowercase hyphenated `feature-name` in filenames. Number features sequentially (`v1`, `v2`, …).

If the user wants a different location or naming, follow their instruction.

**If saving is not requested** and no project rule mandates it, respond in chat only.

## Output shape (markdown)

Use clear headings so files stay scannable:

```markdown
# Analysis: [product or doc title]

## Business overview
[Concise summary from documentation]

## Features
### 1. [Feature title]
[Short description — user-facing, no tech stack]

### 2. ...
```

For per-feature tech files:

```markdown
# [Feature title]

## Feature summary
[One paragraph]

## Technical requirements
- ...

## Implementation steps
1. ...
```

## Quality checks

- Features are **atomic** and **testable** in spirit (each describable without requiring another feature’s full delivery).
- Terminology matches the source docs; note assumptions explicitly.
- After writing files, paths are correct for the OS (use `docs/features/...` in repo).
