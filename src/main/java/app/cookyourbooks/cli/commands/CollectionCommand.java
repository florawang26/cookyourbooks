package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;

/** Handles collection subcommands (e.g., collection create). */
public class CollectionCommand implements Command {

  private final CollectionCreateCommand createCommand = new CollectionCreateCommand();

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2 || !"create".equals(args.get(1).toLowerCase(Locale.ROOT))) {
      context.println("Unknown collection subcommand. Usage: collection create <name>");
      return;
    }
    createCommand.execute(context, args);
  }
}
