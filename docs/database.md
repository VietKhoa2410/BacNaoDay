# Database design: person and kinship relations

This document defines a relational model where each person can have many relationships to other person. Relationships are stored **once** as a directed edge; the **other person’s role from the viewer’s perspective** is derived using an inverse map (so if *A is B’s father*, from *B* you resolve *B is A’s son* or *daughter* using B’s gender where needed).

---

## Entity relationship overview

```text
person ──┬──< person_relations >──┬── person

```

---

## 1. `person`

Core identity for a person in the system. Add columns your product already needs (auth, display name, etc.).

| Column           | Type        | Notes |
|------------------|------------|--------|
| `id`             | UUID / BIGSERIAL PK | Stable identifier |
| `display_name`   | TEXT       | Shown in UI |
| `gender`         | ENUM or TEXT | `M`, `F`, `UNKNOWN`, … — used when choosing *son* vs *daughter* inverse |
| `created_at`     | TIMESTAMPTZ | |
| `updated_at`     | TIMESTAMPTZ | |
| `deleted_at`     | TIMESTAMPTZ NULL | Optional soft delete |

**Indexes:** PK on `id`; optional index on `deleted_at` for active-person queries.

---

## 2. `person_relations`
| Column          | Type                | Notes                     |
|-----------------|---------------------|---------------------------|
| `id`            | UUID / BIGSERIAL PK | Stable identifier         |
| `from_person_id`  | FK → `person.id`      |                           |
| `to_person_id`    | FK → `person.id`      |                           |
| `relation_type` | TEXT NOT NULL       | FATHER_OF, MOTHER_OF, etc |


**Rules:**
- Create enum for relation_type with forward and backward type.
- When new relation created, add two person_relations record for forward and backward.
Example:
A is B's father, then create 2 person_relations record:
- From A to B with relation_type is FATHER_OF
- From B to A with relation_type is SON_OF, DAUGHTER_OF or CHILD_OF base on B gender.
