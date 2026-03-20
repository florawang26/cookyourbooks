package app.cookyourbooks.services;

import java.util.Objects;
import java.util.function.Supplier;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

/** Shared transformation capability used by planner/cook/librarian workflows. */
public class TransformerService {

  private final Supplier<ConversionRegistry> conversionRegistrySupplier;

  public TransformerService(Supplier<ConversionRegistry> conversionRegistrySupplier) {
    this.conversionRegistrySupplier =
        Objects.requireNonNull(
            conversionRegistrySupplier, "conversionRegistrySupplier must not be null");
  }

  /** Scales recipe quantities to target servings and returns a transformed copy. */
  public ScaleResult scaleToServings(Recipe recipe, int targetServings) {
    if (targetServings <= 0) {
      throw new IllegalArgumentException("Invalid servings. Please provide a positive number.");
    }
    if (recipe.getServings() == null) {
      throw new IllegalArgumentException(
          "Cannot scale '" + recipe.getTitle() + "': no serving information available.");
    }

    int originalServings = recipe.getServings().getAmount();
    double factor = ((double) targetServings) / originalServings;
    Recipe scaled = recipe.scale(factor);
    return new ScaleResult(scaled, factor, targetServings);
  }

  /** Converts recipe measured quantities to the provided target unit. */
  public Recipe convert(Recipe recipe, Unit targetUnit) throws UnsupportedConversionException {
    return recipe.convert(targetUnit, conversionRegistrySupplier.get());
  }

  public record ScaleResult(Recipe scaledRecipe, double factor, int targetServings) {}
}
