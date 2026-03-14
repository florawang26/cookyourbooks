package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.model.Unit;

public class ConversionAddCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    double fromAmount;
    double toAmount;
    try {
      fromAmount = Double.parseDouble(context.readLine("From amount: ").trim());
    } catch (RuntimeException e) {
      context.println("Invalid amount. Please enter a number.");
      return;
    }

    String fromUnitRaw = context.readLine("From unit: ").trim();
    String ingredientRaw = context.readLine("Ingredient (or any): ").trim();
    try {
      toAmount = Double.parseDouble(context.readLine("To amount: ").trim());
    } catch (RuntimeException e) {
      context.println("Invalid amount. Please enter a number.");
      return;
    }
    String toUnitRaw = context.readLine("To unit: ").trim();

    Unit fromUnit;
    Unit toUnit;
    try {
      fromUnit = Unit.parse(fromUnitRaw);
      toUnit = Unit.parse(toUnitRaw);
    } catch (IllegalArgumentException e) {
      context.println("Invalid unit: " + e.getMessage());
      return;
    }

    if (fromAmount <= 0 || toAmount <= 0) {
      context.println("Invalid amount. Please enter a number greater than zero.");
      return;
    }

    String ingredient = ingredientRaw.isBlank() ? "any" : ingredientRaw;
    String normalizedIngredient = ingredient.toLowerCase(Locale.ROOT);
    String identifier =
        fromUnit.getAbbreviation().toLowerCase(Locale.ROOT) + " " + normalizedIngredient;

    boolean duplicate =
        context.librarianService().getConversions().stream()
            .anyMatch(
                r -> {
                  String existingIngredient =
                      r.ingredientName() == null
                          ? "any"
                          : r.ingredientName().toLowerCase(Locale.ROOT);
                  String existing =
                      r.fromUnit().getAbbreviation().toLowerCase(Locale.ROOT)
                          + " "
                          + existingIngredient;
                  return existing.equals(identifier);
                });
    if (duplicate) {
      context.println("A conversion for '" + identifier + "' already exists.");
      return;
    }

    double factor = toAmount / fromAmount;
    String ingredientName = "any".equalsIgnoreCase(ingredient) ? null : ingredient;
    ConversionRule rule = new ConversionRule(fromUnit, toUnit, factor, ingredientName);
    context.librarianService().addConversion(rule);

    context.println(
        "Added: "
            + fromAmount
            + " "
            + fromUnit.getAbbreviation()
            + " "
            + ingredient
            + " = "
            + toAmount
            + " "
            + toUnit.getAbbreviation());
  }
}
