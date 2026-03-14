package app.cookyourbooks.cli.commands;

import java.util.List;

import app.cookyourbooks.cli.CliContext;

public class ConversionRemoveCommand implements Command {

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 3) {
      context.println("Usage: conversion remove <rule>");
      return;
    }

    String ruleId = args.get(2).trim();
    if (ruleId.isBlank()) {
      context.println("Usage: conversion remove <rule>");
      return;
    }

    boolean removed = context.librarianService().removeConversion(ruleId);
    if (!removed) {
      context.println("No conversion found for '" + ruleId + "'.");
      return;
    }
    context.println("Removed conversion: " + ruleId);
  }
}
