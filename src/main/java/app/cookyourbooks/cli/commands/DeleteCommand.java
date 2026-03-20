package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.model.Recipe;

public class DeleteCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || args.get(1).isBlank()) {
      context.println("Usage: delete <recipe>");
      return;
    }

    String query = args.get(1).trim();
    List<Recipe> matches = context.librarianService().findRecipes(query);
    if (matches.isEmpty()) {
      context.println(ErrorMessages.recipeNotFound(query));
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "delete");
      return;
    }

    Recipe recipe = matches.get(0);
    String response = context.readLine("Delete recipe '" + recipe.getTitle() + "'? (y/n): ");
    if (response != null && response.trim().equalsIgnoreCase("y")) {
      context.librarianService().deleteRecipe(recipe);
      context.println("Deleted recipe '" + recipe.getTitle() + "'.");
    }
  }
}
