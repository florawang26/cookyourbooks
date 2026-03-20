package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.model.Recipe;

public class ScaleCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 3) {
      context.println("Usage: scale <recipe> <servings>");
      return;
    }

    String query = args.get(1).trim();
    int targetServings;
    try {
      targetServings = Integer.parseInt(args.get(2).trim());
    } catch (RuntimeException e) {
      context.println("Invalid servings. Please provide a positive number.");
      return;
    }

    List<Recipe> matches = context.librarianService().findRecipes(query);
    if (matches.isEmpty()) {
      context.println(ErrorMessages.recipeNotFound(query));
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "scale");
      return;
    }

    Recipe original = matches.get(0);
    app.cookyourbooks.services.TransformerService.ScaleResult scaled;
    try {
      scaled = context.transformerService().scaleToServings(original, targetServings);
    } catch (IllegalArgumentException e) {
      context.println(e.getMessage() == null ? "Invalid servings." : e.getMessage());
      return;
    }

    Recipe scaledRecipe = scaled.scaledRecipe();
    context.println(
        "Scaled '"
            + original.getTitle()
            + "' to "
            + targetServings
            + " servings ("
            + String.format("%.1fx", scaled.factor())
            + "):");
    context.println("Ingredient                Original        Scaled");
    for (int i = 0; i < original.getIngredients().size(); i++) {
      var before = original.getIngredients().get(i);
      var after = scaledRecipe.getIngredients().get(i);
      String ingredientName = before.getName();
      String originalText = CommandSupport.measuredQuantityText(before);
      String scaledText = CommandSupport.measuredQuantityText(after);
      context.println(String.format("%-24s %-14s -> %s", ingredientName, originalText, scaledText));
    }

    String save = context.readLine("Save scaled recipe? (y/n): ");
    if (save != null && save.trim().equalsIgnoreCase("y")) {
      String newTitle = original.getTitle() + " (scaled to " + targetServings + ")";
      Recipe persisted =
          new Recipe(
              null,
              newTitle,
              scaledRecipe.getServings(),
              scaledRecipe.getIngredients(),
              scaledRecipe.getInstructions(),
              scaledRecipe.getConversionRules());
      context.librarianService().saveDerivedRecipe(original, persisted);
      context.println("Saved scaled recipe '" + newTitle + "'.");
    } else {
      context.println("Scaling discarded.");
    }
  }
}
