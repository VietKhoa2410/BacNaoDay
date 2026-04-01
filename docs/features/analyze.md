# Analysis: BacNaoDay

## Business overview

BacNaoDay is a simple website focused on **Vietnamese family kinship**: it helps users see how people in a family are related and **what to call each relative** in Vietnamese.

**Actors:** Users with accounts (username and password).

**Core scope (from documentation):**

- Sign in and sign out.
- **Relation pages:** Each page represents one family-relation context; the user gives it a name.
- **People on a page:** The user adds person nodes inside a relation page.
- **Self and lookup:** The user marks one person as “you,” then selects another person to learn **what the relationship is** and **the relative’s name/title** in Vietnamese.

**Gaps and assumptions (not specified in source docs):**

- No detail on editing or deleting relation pages or people, password recovery, multi-user collaboration on the same page, or how Vietnamese kinship rules are sourced (fixed rules engine vs. data).
- “Relative name” is interpreted as the culturally appropriate way to address or refer to that relative (e.g., kinship term), consistent with the product goal.

## Features

### 1. User login and logout

Users sign in with a username and password and can sign out. This bounds access to their own relation pages and data.

### 2. Relation pages (create and name)

Users can create separate pages, each representing one family-relation scenario, and give each page a clear name so they can tell them apart.

### 3. Person nodes on a relation page

On a given relation page, users can add person nodes (individuals) that will be used to build the family picture for lookups.

### 4. Mark “your” person

On a relation page, the user picks exactly one person node that represents themselves, so the system knows the reference point for “my relation to…”.

### 5. Relation and relative-name lookup

After “you” is set, the user selects another person on the same page; the site shows **what the relationship is** and **the appropriate Vietnamese relative name/title** for that person from the user’s perspective.
