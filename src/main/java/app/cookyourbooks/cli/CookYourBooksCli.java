package app.cookyourbooks.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;

import app.cookyourbooks.cli.commands.CollectionCommand;
import app.cookyourbooks.cli.commands.CollectionsCommand;
import app.cookyourbooks.cli.commands.Command;
import app.cookyourbooks.cli.commands.ConversionCommand;
import app.cookyourbooks.cli.commands.ConversionsCommand;
import app.cookyourbooks.cli.commands.ConvertCommand;
import app.cookyourbooks.cli.commands.CookCommand;
import app.cookyourbooks.cli.commands.DeleteCommand;
import app.cookyourbooks.cli.commands.ExportCommand;
import app.cookyourbooks.cli.commands.HelpCommand;
import app.cookyourbooks.cli.commands.ImportCommand;
import app.cookyourbooks.cli.commands.RecipesCommand;
import app.cookyourbooks.cli.commands.ScaleCommand;
import app.cookyourbooks.cli.commands.SearchCommand;
import app.cookyourbooks.cli.commands.ShoppingListCommand;
import app.cookyourbooks.cli.commands.ShowCommand;

public class CookYourBooksCli implements Runnable {

  private static final Pattern IMPORT_JSON_QUOTED_COLLECTION =
      Pattern.compile("(?i)^import\\s+json\\s+(.+?)\\s+\\\"([^\\\"]+)\\\"\\s*$");
  private static final Pattern IMPORT_JSON_SIMPLE =
      Pattern.compile("(?i)^import\\s+json\\s+(\\S+)\\s+(\\S+)\\s*$");

  private final CliContext context;
  private final Map<String, Command> commands;
  private final DefaultParser parser;

  public CookYourBooksCli(CliContext context) {
    this.context = context;
    this.commands = new HashMap<>();
    this.parser = new DefaultParser();
    this.parser.setEscapeChars(null);
    registerCommands();
  }

  private void registerCommands() {
    commands.put("help", new HelpCommand());
    commands.put("collections", new CollectionsCommand());
    commands.put("collection", new CollectionCommand());
    commands.put("recipes", new RecipesCommand());
    commands.put("conversions", new ConversionsCommand());
    commands.put("conversion", new ConversionCommand());
    commands.put("import", new ImportCommand());
    commands.put("show", new ShowCommand());
    commands.put("search", new SearchCommand());
    commands.put("delete", new DeleteCommand());
    commands.put("scale", new ScaleCommand());
    commands.put("convert", new ConvertCommand());
    commands.put("shopping-list", new ShoppingListCommand());
    commands.put("cook", new CookCommand());
    commands.put("export", new ExportCommand());
  }

  @Override
  public void run() {
    while (true) {
      String line = context.readLine("cyb> ");
      if (line == null) {
        context.println("Goodbye!");
        return;
      }

      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      ParsedLine parsed;
      try {
        parsed = parser.parse(line, line.length());
      } catch (RuntimeException e) {
        context.println("Unable to parse command: " + e.getMessage());
        continue;
      }

      List<String> args = new ArrayList<>(parsed.words());
      if (args.isEmpty()) {
        continue;
      }

      String commandName = args.get(0).toLowerCase(Locale.ROOT);
      if ("quit".equals(commandName) || "exit".equals(commandName)) {
        context.println("Goodbye!");
        return;
      }

      if ("import".equals(commandName)) {
        Command importCommand = commands.get("import");
        if (importCommand == null) {
          context.println("Usage: import json <file> <collection>");
          continue;
        }

        List<String> importArgs = tryParseImportArgs(trimmed);
        if (importArgs.isEmpty()) {
          context.println("Usage: import json <file> <collection>");
          continue;
        }

        importCommand.execute(context, importArgs);
        continue;
      }

      Command command = commands.get(commandName);
      if (command == null) {
        context.println(
            "Unknown command: '" + commandName + "'. Type 'help' for a list of commands.");
        continue;
      }

      command.execute(context, args);
    }
  }

  private static List<String> tryParseImportArgs(String rawLine) {
    Matcher quoted = IMPORT_JSON_QUOTED_COLLECTION.matcher(rawLine);
    if (quoted.matches()) {
      return List.of("import", "json", quoted.group(1).trim(), quoted.group(2).trim());
    }

    Matcher simple = IMPORT_JSON_SIMPLE.matcher(rawLine);
    if (simple.matches()) {
      return List.of("import", "json", simple.group(1).trim(), simple.group(2).trim());
    }

    return List.of();
  }
}
