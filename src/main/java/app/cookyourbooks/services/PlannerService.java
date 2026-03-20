package app.cookyourbooks.services;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import app.cookyourbooks.adapters.MarkdownExporter;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.ShoppingList;

/** Planner workflows: shopping-list generation and recipe export. */
public class PlannerService {

  private final ShoppingListAggregator shoppingListAggregator;
  private final MarkdownExporter markdownExporter;

  public PlannerService() {
    this(new ShoppingListAggregator(), new MarkdownExporter());
  }

  public PlannerService(
      ShoppingListAggregator shoppingListAggregator, MarkdownExporter markdownExporter) {
    this.shoppingListAggregator =
        Objects.requireNonNull(shoppingListAggregator, "shoppingListAggregator must not be null");
    this.markdownExporter =
        Objects.requireNonNull(markdownExporter, "markdownExporter must not be null");
  }

  /** Aggregates ingredients from the given recipes into a shopping list. */
  public ShoppingList buildShoppingList(List<Recipe> recipes) {
    List<Ingredient> allIngredients =
        recipes.stream().flatMap(r -> r.getIngredients().stream()).toList();
    return shoppingListAggregator.aggregate(allIngredients);
  }

  /** Exports a recipe to markdown at the provided path. */
  public void exportRecipe(Recipe recipe, Path outputPath) {
    markdownExporter.exportToFile(recipe, outputPath);
  }
}
