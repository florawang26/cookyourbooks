Command architecture — How did you design your command dispatch system? What responsibilities does each component have? Which heuristics informed these assignments?
# ADR-003: [command architecture]

## Context
[The CLI needs to handle many different commands. Putting all command logic in one giant if/else or switch statement violates the rate of change heuristic — adding or modifying one command requires touching the entire dispatch class, risking breaking other commands.]

## Decision
[Create a Command interface with an execute() method. Each command gets its own class (e.g. CollectionsCommand, HelpCommand). A dispatcher uses a Map<String, Command> to route user input to the right command class.]

## Consequences
[Positive:

Adding a new command only requires a new class, existing commands untouched (Open/Closed Principle)
Each command can be tested independently
Rate of change heuristic satisfied — commands that change independently are separated

Negative:

More files to manage
Need to remember to register each new command in the dispatcher map]