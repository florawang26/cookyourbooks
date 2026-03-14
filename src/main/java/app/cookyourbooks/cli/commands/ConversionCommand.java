package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import app.cookyourbooks.cli.CliContext;

/** Handles conversion subcommands (e.g., conversion add/remove). */
public class ConversionCommand implements Command {

  private final ConversionAddCommand addCommand = new ConversionAddCommand();
  private final ConversionRemoveCommand removeCommand = new ConversionRemoveCommand();

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 2) {
      context.println("Usage: conversion <add|remove>");
      return;
    }

    String sub = args.get(1).toLowerCase(Locale.ROOT);
    if ("add".equals(sub)) {
      addCommand.execute(context, args);
      return;
    }
    if ("remove".equals(sub)) {
      removeCommand.execute(context, args);
      return;
    }
    context.println("Unknown conversion subcommand. Usage: conversion <add|remove>");
  }
}
