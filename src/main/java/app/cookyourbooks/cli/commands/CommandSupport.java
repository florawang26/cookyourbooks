package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.VagueIngredient;

final class CommandSupport {

  private CommandSupport() {}

  static String shortId(Recipe recipe) {
    String id = recipe.getId();
    if (id == null || id.isBlank()) {
      return "";
    }
    return id.length() <= 8 ? id : id.substring(0, 8);
  }

  static void printAmbiguousRecipes(
      CliContext context, String query, List<Recipe> matches, String command) {
    context.println("Multiple recipes match '" + query + "':");
    for (int i = 0; i < matches.size(); i++) {
      Recipe recipe = matches.get(i);
      String collection =
          context
              .librarianService()
              .findCollectionTitleForRecipe(recipe.getId())
              .orElse("Unknown Collection");
      context.println(
          "  "
              + (i + 1)
              + ". "
              + recipe.getTitle()
              + "  ["
              + shortId(recipe)
              + "]  ("
              + collection
              + ")");
    }

    Set<String> distinctTitles = matches.stream().map(Recipe::getTitle).collect(Collectors.toSet());
    String sampleId = shortId(matches.get(0));
    if (distinctTitles.size() == 1) {
      context.println("Please specify a short ID (e.g. '" + command + " " + sampleId + "').");
    } else {
      context.println(
          "Please specify the full recipe name, or use a short ID (e.g. '"
              + command
              + " "
              + sampleId
              + "').");
    }
  }

  static String measuredQuantityText(Ingredient ingredient) {
    if (ingredient instanceof MeasuredIngredient measured) {
      return measured.getQuantity().toString();
    }
    if (ingredient instanceof VagueIngredient vague) {
      String description = vague.getDescription();
      if (description == null || description.isBlank()) {
        return "to taste";
      }
      return description;
    }
    return "";
  }

  static String ingredientRefDisplay(Ingredient ingredient, @Nullable Quantity quantity) {
    if (ingredient instanceof MeasuredIngredient measured && quantity != null) {
      StringBuilder builder = new StringBuilder();
      builder.append(quantity).append(" ").append(measured.getName());
      if (measured.getPreparation() != null && !measured.getPreparation().isBlank()) {
        builder.append(", ").append(measured.getPreparation());
      }
      return builder.toString();
    }

    if (ingredient instanceof VagueIngredient vague) {
      String description = vague.getDescription();
      String suffix = (description == null || description.isBlank()) ? "to taste" : description;
      StringBuilder builder = new StringBuilder();
      builder.append(vague.getName()).append(" ").append(suffix);
      if (vague.getPreparation() != null && !vague.getPreparation().isBlank()) {
        builder.append(", ").append(vague.getPreparation());
      }
      return builder.toString();
    }

    return ingredient.toString();
  }
}
