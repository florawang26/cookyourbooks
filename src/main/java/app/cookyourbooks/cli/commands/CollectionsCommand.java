package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;

public class CollectionsCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    List<RecipeCollection> collections = context.librarianService().getCollections();
    if (collections.isEmpty()) {
      context.println("No collections. Use 'collection create <name>' to create one.");
      return;
    }

    context.println("Collections:");
    for (int i = 0; i < collections.size(); i++) {
      RecipeCollection collection = collections.get(i);
      String type = typeLabel(collection.getSourceType());
      int count = collection.getRecipes().size();
      String suffix = count == 1 ? "recipe" : "recipes";
      context.println(
          String.format(
              "  %d. %-24s [%-8s] %4d %s", i + 1, collection.getTitle(), type, count, suffix));
    }
  }

  private static String typeLabel(SourceType sourceType) {
    return switch (sourceType) {
      case PERSONAL -> "Personal";
      case PUBLISHED_BOOK -> "Cookbook";
      case WEBSITE -> "Web";
    };
  }
}
