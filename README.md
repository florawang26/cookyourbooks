# CookYourBooks

A full-stack Java desktop application for digitizing, organizing, and using personal recipe collections. Features an interactive CLI, JavaFX GUI, Gemini API image import, intelligent unit conversion and scaling, and JSON persistence — built with hexagonal architecture across a 4-person agile team.

## Features

- **Recipe import from images** — upload a cookbook photo and the Gemini API extracts structured recipe data automatically
- **Intelligent scaling & unit conversion** — scale recipes to any serving size; convert between imperial, metric, and custom house units
- **Interactive CLI** — full command-line interface with tab completion, step-by-step cooking mode, and shopping list generation via JLine
- **JavaFX GUI** — four-feature desktop interface: library browser, recipe editor, image import, and search & filter
- **JSON persistence** — recipes and collections saved locally with full round-trip serialization using Jackson
- **Hexagonal architecture** — business logic fully separated from UI and persistence adapters

## Tech Stack

`Java` `JavaFX` `JLine` `Jackson` `JUnit` `Mockito` `Gemini API` `Gradle` `Git`

## Architecture

The app is structured around three actor-aligned service layers:

- **Librarian service** — manages collections, recipe import, and search
- **Cook service** — handles step-by-step cooking session state
- **Planner service** — shopping list generation, scaling, and export

Each service depends only on repository interfaces, enabling full testability with Mockito mocks and clean separation from persistence implementations.

## Built With

Northeastern University · CS3100: Program Design & Implementation · Spring 2026
