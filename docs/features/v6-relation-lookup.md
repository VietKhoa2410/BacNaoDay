# Relation and relative-name lookup

## Feature summary

Given the user’s chosen “self” and another person on the same relation page, the product shows the relationship in understandable terms and the **Vietnamese kinship term** (how to call or refer to that relative).

## Technical requirements

- Create a api to lookup relative-name by personId as a param.
- If no marked person yet, return fail.
- Else, from marked person, calculate the level between marked node and input node and return the result.

## Implementation steps

1. Create api.
2. Get relationPage by current user and personId, if no result return error.
3. Check if relationPage have markedPersonId yet, if not then return error.
4. Calculate the level between marked node and input node by rule:
    From marked node, every level lead to input node that:
    - higher than marked node, add -1 to counter.
    - lower than marked node, add 1 to counter.
5. Return counter result.