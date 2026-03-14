Tab completion architecture — How did you design your completer? What concerns did you identify, and how did you assign responsibility for each? Which heuristics drove those assignments?
# ADR-004: [Title of Decision]

## Context
[Tab completion involves multiple distinct concerns that change at different rates. Putting all completion logic in one class means adding a new command's completion would risk breaking existing completions, violating the rate of change heuristic.]

## Decision
[Separate tab completion into 3 concerns:

What arguments a command needs — each command knows which argument positions expect which types (recipe, collection, unit)
What values are available — values come from the repository (recipe titles, collection names) or fixed lists (unit names from Unit enum)
Formatting — names with spaces are quoted automatically]

## Consequences
[Positive:

Adding a new command's completion only requires adding its argument types, not touching existing completers
Values from repository are always up to date
Rate of change heuristic satisfied — each concern changes independently

Negative:

More classes to coordinate
Need to keep argument type declarations in sync with actual command implementations]