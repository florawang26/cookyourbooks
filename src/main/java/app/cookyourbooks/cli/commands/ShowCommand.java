package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.model.Recipe;

public class ShowCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || args.get(1).isBlank()) {
      context.println("Usage: show <recipe>");
      return;
    }

    String query = args.get(1).trim();
    List<Recipe> matches = context.librarianService().findRecipes(query);
    if (matches.isEmpty()) {
      context.println(ErrorMessages.recipeNotFound(query));
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "show");
      return;
    }

    Recipe recipe = matches.get(0);
    context.println(recipe.getTitle());
    if (recipe.getServings() != null) {
      String description = recipe.getServings().getDescription();
      if (description == null || description.isBlank()) {
        context.println("Serves " + recipe.getServings().getAmount());
      } else {
        context.println("Serves " + recipe.getServings().getAmount() + " " + description);
      }
    }

    context.println("Ingredients:");
    for (var ingredient : recipe.getIngredients()) {
      context.println("  • " + ingredient);
    }

    context.println("Instructions:");
    for (var instruction : recipe.getInstructions()) {
      context.println("  " + instruction);
    }
  }
}
