# Mark “your” person

## Feature summary

On each relation page, the user designates one person node as themselves. This anchor is required before “my relation to X” lookups make sense.

## Technical requirements

- In table RelationPage, add a nullable column markedPersonId.
- Create new API to mark a person in a relation page.

## Implementation steps

1. Add markedPersonId to RelationPage
2. Create api
3. Validate input, check if personId belong to that relationPage.
4. Update markedPersonId for that ralationPage.
