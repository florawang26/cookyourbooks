package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.conversion.ConversionRule;

public class ConversionsCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    List<ConversionRule> rules = context.librarianService().getConversions();
    if (rules.isEmpty()) {
      context.println("No house conversions defined. Use 'conversion add' to create one.");
      return;
    }

    context.println("House Conversions:");
    for (int i = 0; i < rules.size(); i++) {
      ConversionRule r = rules.get(i);
      double fromAmount = 1.0;
      double toAmount = r.factor();
      String ingredient = r.ingredientName() == null ? "any" : r.ingredientName();
      context.println(
          String.format(
              "  %d. %.4g %s %s = %.4g %s",
              i + 1,
              fromAmount,
              r.fromUnit().getAbbreviation(),
              ingredient,
              toAmount,
              r.toUnit().getAbbreviation()));
    }
  }
}
