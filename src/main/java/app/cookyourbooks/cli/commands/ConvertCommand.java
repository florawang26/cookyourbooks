package app.cookyourbooks.cli.commands;

import java.util.Arrays;
import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

public class ConvertCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 3) {
      context.println("Usage: convert <recipe> <unit>");
      return;
    }

    String query = args.get(1).trim();
    String unitRaw = args.get(2).trim();

    Unit targetUnit;
    try {
      targetUnit = Unit.parse(unitRaw);
    } catch (IllegalArgumentException e) {
      context.println(
          "Unknown unit: '"
              + unitRaw
              + "'. Valid units include: "
              + String.join(", ", Arrays.asList(Unit.getParseableNames()))
              + ".");
      return;
    }

    List<Recipe> matches = context.librarianService().findRecipes(query);
    if (matches.isEmpty()) {
      context.println(ErrorMessages.recipeNotFound(query));
      return;
    }
    if (matches.size() > 1) {
      CommandSupport.printAmbiguousRecipes(context, query, matches, "convert");
      return;
    }

    Recipe original = matches.get(0);
    Recipe converted;
    try {
      converted = context.transformerService().convert(original, targetUnit);
    } catch (UnsupportedConversionException e) {
      context.println(e.getMessage() == null ? "Cannot convert." : e.getMessage());
      return;
    }

    context.println("Converted '" + original.getTitle() + "' to " + targetUnit.name() + ":");
    context.println("Ingredient                Original        Converted");
    for (int i = 0; i < original.getIngredients().size(); i++) {
      var before = original.getIngredients().get(i);
      var after = converted.getIngredients().get(i);
      context.println(
          String.format(
              "%-24s %-14s -> %s",
              before.getName(),
              CommandSupport.measuredQuantityText(before),
              CommandSupport.measuredQuantityText(after)));
    }

    String save = context.readLine("Save converted recipe? (y/n): ");
    if (save != null && save.trim().equalsIgnoreCase("y")) {
      String newTitle = original.getTitle() + " (converted to " + targetUnit.name() + ")";
      Recipe persisted =
          new Recipe(
              null,
              newTitle,
              converted.getServings(),
              converted.getIngredients(),
              converted.getInstructions(),
              converted.getConversionRules());
      context.librarianService().saveDerivedRecipe(original, persisted);
      context.println("Saved converted recipe '" + newTitle + "'.");
    } else {
      context.println("Conversion discarded.");
    }
  }
}
