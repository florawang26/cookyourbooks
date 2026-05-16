package app.cookyourbooks.services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.adapters.MarkdownExporter;
import app.cookyourbooks.adapters.PdfExportException;
import app.cookyourbooks.adapters.PdfExporter;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.ShoppingList;

/** Implementation of {@link PlannerService} for the Planner actor. */
public final class PlannerServiceImpl implements PlannerService {

  private final ShoppingListAggregator aggregator;
  private final MarkdownExporter markdownExporter;
  private final PdfExporter pdfExporter;

  /**
   * Constructs a new PlannerServiceImpl.
   *
   * @param aggregator shopping list aggregator
   * @param markdownExporter markdown exporter for recipes
   * @param pdfExporter pdf exporter for recipes
   */
  public PlannerServiceImpl(
      ShoppingListAggregator aggregator,
      MarkdownExporter markdownExporter,
      PdfExporter pdfExporter) {
    this.aggregator = aggregator;
    this.markdownExporter = markdownExporter;
    this.pdfExporter = pdfExporter;
  }

  @Override
  public @NonNull ShoppingList generateShoppingList(@NonNull List<Recipe> recipes) {
    List<Ingredient> allIngredients = new ArrayList<>();
    for (Recipe r : recipes) {
      allIngredients.addAll(r.getIngredients());
    }
    return aggregator.aggregate(allIngredients);
  }

  @Override
  public void exportToMarkdown(@NonNull Recipe recipe, @NonNull Path file) {
    markdownExporter.exportToFile(recipe, file);
  }

  @Override
  public void exportToPdf(@NonNull Recipe recipe, @NonNull Path file) throws PdfExportException {
    pdfExporter.exportToFile(recipe, file);
  }
}
