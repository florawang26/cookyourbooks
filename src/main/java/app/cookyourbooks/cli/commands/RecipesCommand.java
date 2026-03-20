package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.services.CollectionNotFoundException;

public class RecipesCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2) {
      context.println("Usage: recipes <collection>");
      return;
    }

    String title = args.get(1).trim();
    if (title.isBlank()) {
      context.println("Usage: recipes <collection>");
      return;
    }

    List<Recipe> recipes;
    try {
      recipes = context.librarianService().getRecipesInCollection(title);
    } catch (CollectionNotFoundException e) {
      context.println(
          "Collection not found: '" + title + "'. Use 'collections' to see available collections.");
      return;
    }

    context.println(title + " (" + recipes.size() + " recipes):");
    for (int i = 0; i < recipes.size(); i++) {
      Recipe recipe = recipes.get(i);
      String servings =
          recipe.getServings() != null ? servingsText(recipe.getServings()) : "No servings";
      context.println("  " + (i + 1) + ". " + recipe.getTitle() + "          " + servings);
    }
  }

  private static String servingsText(Servings servings) {
    if (servings == null) {
      return "No servings";
    }
    String description = servings.getDescription();
    if (description == null || description.isBlank()) {
      return "Serves " + servings.getAmount();
    }
    return "Serves " + servings.getAmount() + " " + description;
  }
}
