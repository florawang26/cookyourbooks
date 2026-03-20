package app.cookyourbooks.cli.commands;

import java.util.ArrayList;
import java.util.List;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.ErrorMessages;
import app.cookyourbooks.model.Recipe;

public class ShoppingListCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2) {
      context.println("Usage: shopping-list <recipe1> [recipe2] ...");
      return;
    }

    List<Recipe> recipes = new ArrayList<>();
    for (int i = 1; i < args.size(); i++) {
      String query = args.get(i).trim();
      if (query.isBlank()) {
        continue;
      }

      List<Recipe> matches = context.librarianService().findRecipes(query);
      if (matches.isEmpty()) {
        context.println(ErrorMessages.recipeNotFound(query));
        return;
      }
      if (matches.size() > 1) {
        CommandSupport.printAmbiguousRecipes(context, query, matches, "shopping-list");
        return;
      }
      recipes.add(matches.get(0));
    }

    if (recipes.isEmpty()) {
      context.println("Usage: shopping-list <recipe1> [recipe2] ...");
      return;
    }

    var shoppingList = context.plannerService().buildShoppingList(recipes);
    context.println("Shopping List (" + recipes.size() + " recipes):");
    context.println("Measured Items:");
    for (var item : shoppingList.getItems()) {
      context.println("  • " + item.getQuantity() + " " + item.getName());
    }

    if (!shoppingList.getUncountableItems().isEmpty()) {
      context.println("Also needed:");
      for (String item : shoppingList.getUncountableItems()) {
        context.println("  • " + item);
      }
    }

    context.println(
        "Total: "
            + shoppingList.getItems().size()
            + " measured items, "
            + shoppingList.getUncountableItems().size()
            + " vague items");
  }
}
