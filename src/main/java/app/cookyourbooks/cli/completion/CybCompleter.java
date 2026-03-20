package app.cookyourbooks.cli.completion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.LibrarianService;

/** JLine completer for command names and context-aware command arguments. */
public class CybCompleter implements Completer {

  private static final List<String> COMMAND_CANDIDATES =
      List.of(
          "help",
          "collections",
          "collection create",
          "recipes",
          "conversions",
          "conversion add",
          "conversion remove",
          "show",
          "search",
          "import json",
          "delete",
          "scale",
          "convert",
          "shopping-list",
          "cook",
          "export",
          "quit",
          "exit");

  private static final List<String> COOK_MODE_COMMANDS =
      List.of("next", "prev", "ingredients", "quit");

  private final CookModeHolder cookModeHolder;
  private final RecipeRepository recipeRepository;
  private final RecipeCollectionRepository collectionRepository;
  private final LibrarianService librarianService;

  public CybCompleter(
      CookModeHolder cookModeHolder,
      RecipeRepository recipeRepository,
      RecipeCollectionRepository collectionRepository,
      LibrarianService librarianService) {
    this.cookModeHolder = cookModeHolder;
    this.recipeRepository = recipeRepository;
    this.collectionRepository = collectionRepository;
    this.librarianService = librarianService;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (cookModeHolder.isInCookMode()) {
      addByPrefix(candidates, COOK_MODE_COMMANDS, safeWord(line));
      return;
    }

    List<String> words = line.words();
    int wordIndex = line.wordIndex();

    if (words.isEmpty() || wordIndex == 0) {
      addByPrefix(candidates, COMMAND_CANDIDATES, safeWord(line));
      return;
    }

    String command = words.get(0).toLowerCase(Locale.ROOT);

    if (isRecipeCommand(command) && wordIndex == 1) {
      addRecipeCandidates(candidates, safeWord(line));
      return;
    }

    if ("shopping-list".equals(command) && wordIndex >= 1) {
      addRecipeCandidates(candidates, safeWord(line));
      return;
    }

    if ("recipes".equals(command) && wordIndex == 1) {
      addCollectionCandidates(candidates, safeWord(line));
      return;
    }

    if ("import".equals(command)
        && words.size() >= 2
        && "json".equalsIgnoreCase(words.get(1))
        && wordIndex == 3) {
      addCollectionCandidates(candidates, safeWord(line));
      return;
    }

    if ("convert".equals(command) && wordIndex == 2) {
      List<String> units = List.of(Unit.getParseableNames());
      addByPrefix(candidates, units, safeWord(line));
      return;
    }

    if ("conversion".equals(command)
        && words.size() >= 2
        && "remove".equalsIgnoreCase(words.get(1))
        && wordIndex == 2) {
      addByPrefix(candidates, librarianService.getConversionIdentifiers(), safeWord(line));
    }
  }

  private static boolean isRecipeCommand(String command) {
    return "show".equals(command)
        || "delete".equals(command)
        || "scale".equals(command)
        || "convert".equals(command)
        || "cook".equals(command)
        || "export".equals(command);
  }

  private static String safeWord(ParsedLine line) {
    String word = line.word();
    return word == null ? "" : word;
  }

  private void addRecipeCandidates(List<Candidate> out, String prefix) {
    Set<String> values = new LinkedHashSet<>();

    for (Recipe recipe : recipeRepository.findAll()) {
      values.add(quoteIfNeeded(recipe.getTitle()));
      String id = recipe.getId();
      if (id != null && !id.isBlank()) {
        values.add(id.length() <= 8 ? id : id.substring(0, 8));
      }
    }

    addByPrefix(out, new ArrayList<>(values), prefix);
  }

  private void addCollectionCandidates(List<Candidate> out, String prefix) {
    List<String> titles =
        collectionRepository.findAll().stream().map(c -> quoteIfNeeded(c.getTitle())).toList();
    addByPrefix(out, titles, prefix);
  }

  private static String quoteIfNeeded(String value) {
    if (value == null) {
      return "";
    }
    return value.contains(" ") ? "\"" + value + "\"" : value;
  }

  private static void addByPrefix(List<Candidate> out, List<String> options, String prefix) {
    String normalizedPrefix = normalizeForMatch(prefix);
    for (String option : options) {
      if (normalizeForMatch(option).startsWith(normalizedPrefix)) {
        out.add(new Candidate(option));
      }
    }
  }

  private static String normalizeForMatch(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("\"")) {
      normalized = normalized.substring(1);
    }
    return normalized;
  }
}
