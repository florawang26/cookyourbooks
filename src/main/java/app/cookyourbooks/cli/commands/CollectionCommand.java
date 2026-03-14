package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;

/** Handles collection subcommands (e.g., collection create). */
public class CollectionCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || !"create".equals(args.get(1).toLowerCase(Locale.ROOT))) {
      context.println("Unknown collection subcommand. Usage: collection create <name>");
      return;
    }

    if (args.size() < 3) {
      context.println("Usage: collection create <name>");
      return;
    }

    String name = args.get(2).trim();
    if (name.isBlank()) {
      context.println("Please provide a collection name.");
      return;
    }

    context.librarianService().createCollection(name);
    context.println("Created personal collection '" + name + "'.");
  }
}
