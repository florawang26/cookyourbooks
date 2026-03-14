package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;

public class HelpCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() >= 2) {
      printDetailedHelp(context, args.get(1));
      return;
    }

    context.println("CookYourBooks Commands");
    context.println("");
    context.println("Library");
    context.println("  collections                       List all recipe collections");
    context.println("  collection create <name>         Create a new personal collection");
    context.println("  recipes <collection>             List recipes in a collection");
    context.println("  conversions                      List all house conversion rules");
    context.println("  conversion add                   Add a house conversion rule (interactive)");
    context.println("  conversion remove <rule>         Remove a house conversion rule");
    context.println("");
    context.println("Recipe");
    context.println("  show <recipe>                    Display a recipe's details");
    context.println("  search <ingredient>              Find recipes containing an ingredient");
    context.println("  import json <file> <coll>        Import recipe from JSON file");
    context.println("  delete <recipe>                  Delete a recipe");
    context.println("");
    context.println("Tools");
    context.println("  scale <recipe> <servings>        Scale recipe to target servings");
    context.println("  convert <recipe> <unit>          Convert recipe to different units");
    context.println("  shopping-list <r1> [r2] ...      Generate shopping list from recipes");
    context.println("  cook <recipe>                    Step-by-step cooking mode");
    context.println("  export <recipe> <file>           Export recipe to Markdown");
    context.println("");
    context.println("General");
    context.println(
        "  help [command]                   Show help (or help for a specific command)");
    context.println("  quit / exit                      Exit the application");
  }

  private static void printDetailedHelp(CliContext context, String commandNameRaw) {
    String commandName = commandNameRaw.toLowerCase(Locale.ROOT);
    switch (commandName) {
      case "show" -> {
        context.println("show <recipe>");
        context.println("Display full recipe details");
      }
      case "scale" -> {
        context.println("scale <recipe> <servings>");
        context.println("Scale a recipe to a target number of servings");
      }
      case "convert" -> {
        context.println("convert <recipe> <unit>");
        context.println("Convert ingredient units in a recipe");
      }
      default ->
          context.println(
              "Unknown command: '" + commandNameRaw + "'. Type 'help' for a list of commands.");
    }
  }
}
