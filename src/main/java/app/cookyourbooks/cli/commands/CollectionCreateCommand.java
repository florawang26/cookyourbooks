package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;

public class CollectionCreateCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
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
