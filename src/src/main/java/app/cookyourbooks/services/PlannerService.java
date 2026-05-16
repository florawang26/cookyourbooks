package app.cookyourbooks.services;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.adapters.PdfExportException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.ShoppingList;

/** Service for the Planner actor: shopping lists and export. */
public interface PlannerService {

  /**
   * Generates a shopping list by aggregating ingredients from the given recipes.
   *
   * @param recipes the recipes to aggregate (must not be null)
   * @return the aggregated shopping list
   */
  @NonNull ShoppingList generateShoppingList(@NonNull List<Recipe> recipes);

  /**
   * Exports a recipe to a Markdown file.
   *
   * @param recipe the recipe to export
   * @param file the output file path
   */
  void exportToMarkdown(@NonNull Recipe recipe, @NonNull Path file);

  /**
   * Exports a recipe to a PDF file.
   *
   * @param recipe the recipe to export
   * @param file the output file path
   * @throws PdfExportException if the PDF cannot be written
   */
  void exportToPdf(@NonNull Recipe recipe, @NonNull Path file) throws PdfExportException;
}
