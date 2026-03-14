package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;

public interface Command {
  /**
   * Executes this command.
   *
   * @param context the CLI context (terminal, services, etc.)
   * @param args the parsed arguments (args.get(0) is the command name)
   */
  void execute(CliContext context, List<String> args);
}
