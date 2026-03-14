Transformation vs. persistence separation — How does your design handle "preview before save" workflows? How is this different from A4's RecipeService?
# ADR-002: [transformation and persistence separation]

## Context
[In A4, RecipeService bundled transformation and persistence together — scaling or converting a recipe would automatically save it. This means users cannot preview the result before deciding whether to keep it.]

## Decision
[Separate transformation from persistence. TransformerService only calculates and returns a new Recipe object without saving. The CLI receives the result, displays it to the user, and only calls the repository to save if the user confirms with "y".]

## Consequences
[Positive:

Users can preview scaled or converted recipes before deciding to save
TransformerService is easier to test since it has no side effects — no file I/O needed
Transformation logic is reusable without always persisting

Negative:

User must explicitly confirm to save, so they could discard changes accidentally
CLI needs extra logic to handle the save confirmation step]