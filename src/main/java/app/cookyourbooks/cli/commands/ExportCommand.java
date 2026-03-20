package app.cookyourbooks.cli.commands;

import java.nio.file.Path;
import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.model.Recipe;

public class ExportCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 3) {
      context.println("Usage: export <recipe> <file>");
      return;
    }

    String query = args.get(1).trim();
    List<Recipe> matches = context.librarianService().findRecipes(query);
    if (matches.isEmpty()) {
      context.println(ErrorMessages.recipeNotFound(query));
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "export");
      return;
    }

    Recipe recipe = matches.get(0);
    Path output = Path.of(args.get(2));
    try {
      context.plannerService().exportRecipe(recipe, output);
      context.println("Exported '" + recipe.getTitle() + "' to " + output);
    } catch (RuntimeException e) {
      context.println(e.getMessage() == null ? "Export failed." : e.getMessage());
    }
  }
}
