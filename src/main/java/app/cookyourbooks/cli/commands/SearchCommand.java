package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;

public class SearchCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || args.get(1).isBlank()) {
      context.println("Usage: search <ingredient>");
      return;
    }

    String query = args.get(1).trim();
    var results = context.librarianService().searchByIngredient(query);
    if (results.isEmpty()) {
      context.println("No recipes found containing '" + query + "'.");
      return;
    }

    context.println("Recipes containing '" + query + "':");
    for (int i = 0; i < results.size(); i++) {
      var hit = results.get(i);
      context.println(
          "  " + (i + 1) + ". " + hit.recipe().getTitle() + " (" + hit.collectionTitle() + ")");
    }
    context.println("Found " + results.size() + " recipes.");
  }
}
