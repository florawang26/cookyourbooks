package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;

/** Handles interactive cook mode: step navigation and ingredient views. */
public class CookCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || args.get(1).isBlank()) {
      context.println("Usage: cook <recipe>");
      return;
    }

    String query = args.get(1).trim();
    List<Recipe> matches = context.librarianService().findRecipes(query);

    if (matches.isEmpty()) {
      context.println(
          "Recipe not found: '" + query + "'. Use 'search' to find recipes by ingredient.");
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "cook");
      return;
    }

    Recipe recipe = matches.get(0);
    if (recipe.getInstructions().isEmpty()) {
      context.println("Error: recipe has no instructions");
      return;
    }

    runCookMode(context, recipe);
  }

  private static void runCookMode(CliContext context, Recipe recipe) {
    int index = 0;
    context.cookModeHolder().setInCookMode(true);
    try {
      printCookHeader(context, recipe);
      displayStep(context, recipe, index);

      while (true) {
        String line = context.readLine("cook> ");
        if (line == null) {
          return;
        }
        String input = line.trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
          continue;
        }

        if ("quit".equals(input) || "q".equals(input)) {
          return;
        }
        if ("ingredients".equals(input) || "i".equals(input)) {
          printIngredients(context, recipe);
          continue;
        }
        if ("prev".equals(input) || "p".equals(input)) {
          if (index == 0) {
            context.println("Already at the beginning.");
          } else {
            index--;
            displayStep(context, recipe, index);
          }
          continue;
        }
        if ("next".equals(input) || "n".equals(input)) {
          if (index >= recipe.getInstructions().size() - 1) {
            context.println("Finished cooking " + recipe.getTitle() + "! Enjoy!");
            return;
          }
          index++;
          displayStep(context, recipe, index);
          continue;
        }

        // Unknown cook-mode command: keep user oriented by re-showing current step and hints.
        displayStep(context, recipe, index);
      }
    } finally {
      context.cookModeHolder().setInCookMode(false);
    }
  }

  private static void printCookHeader(CliContext context, Recipe recipe) {
    context.println("COOKING: " + recipe.getTitle());
    Servings servings = recipe.getServings();
    if (servings != null) {
      String description = servings.getDescription();
      if (description == null || description.isBlank()) {
        context.println("Serves " + servings.getAmount());
      } else {
        context.println("Serves " + servings.getAmount() + " " + description);
      }
    } else {
      context.println("No Servings");
    }
    printIngredients(context, recipe);
  }

  private static void printIngredients(CliContext context, Recipe recipe) {
    context.println("Ingredients:");
    for (var ingredient : recipe.getIngredients()) {
      context.println("  - " + ingredient);
    }
  }

  private static void displayStep(CliContext context, Recipe recipe, int index) {
    Instruction step = recipe.getInstructions().get(index);
    context.println("──");
    context.println("Step " + (index + 1) + " of " + recipe.getInstructions().size());
    context.println(step.getText());

    List<app.cookyourbooks.model.IngredientRef> refs = step.getIngredientRefs();
    if (refs.isEmpty()) {
      context.println("(no ingredients used in this step)");
    } else {
      context.println("Uses:");
      String joined =
          refs.stream()
              .map(ref -> CommandSupport.ingredientRefDisplay(ref.ingredient(), ref.quantity()))
              .collect(java.util.stream.Collectors.joining(", "));
      context.println("  " + joined);
    }

    context.println("[next] [prev] [ingredients] [quit]");
  }
}
