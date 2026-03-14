# ADR-001: [Service Boundary Decomposition]

## Context
[In A4, RecipeService handled too many responsibilities in one class. As the application grows to support a CLI with multiple user types, we need a cleaner way to organize the service layer so that each part of the system can change independently.]

## Decision
[I decided to create separate services aligned with the three actors:

LibrarianService handles collection management, conversions, import, and search — because these are all Librarian workflows that change together
CookService handles step-by-step cooking and recipe lookup — because the Cook's needs are independent from the Librarian's
PlannerService handles shopping lists, export — because the Planner's meal planning workflows change independently
TransformerService handles scaling and unit conversion as a shared capability — because both the Planner and potentially other actors need transformation without it being tied to any one actor]

## Consequences
[Positive:

Low coupling means changes to Librarian workflows (e.g. how recipes are imported) won't affect Cook or Planner
Each service can be tested independently
In the group project, teammates can work on different actors without stepping on each other's code (Conway's Law)

Negative:

More files to manage
Some capabilities like recipe lookup are needed by multiple actors, which requires careful coordination between services]
