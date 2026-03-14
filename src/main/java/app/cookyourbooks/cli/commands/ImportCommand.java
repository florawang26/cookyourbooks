package app.cookyourbooks.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Recipe;

/** Handles import subcommands, currently import json. */
public class ImportCommand implements Command {

  private static final Pattern JSON_FILENAME_PATTERN =
      Pattern.compile(
          "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.json)$");

  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Override
  public void execute(CliContext context, List<String> args) {
    if (args.size() < 4 || !"json".equalsIgnoreCase(args.get(1))) {
      context.println("Usage: import json <file> <collection>");
      return;
    }

    String fileArg = args.get(2);
    String collectionTitle = args.get(3);
    var collectionOpt = context.collectionRepository().findByTitle(collectionTitle);
    if (collectionOpt.isEmpty()) {
      context.println("Collection not found: " + collectionTitle);
      return;
    }

    Recipe recipe;
    try {
      recipe = OBJECT_MAPPER.readValue(resolveImportPath(fileArg).toFile(), Recipe.class);
    } catch (IOException e) {
      context.println("Failed to import recipe: " + e.getMessage());
      return;
    }

    var updated = collectionOpt.get().addRecipe(recipe);
    context.collectionRepository().save(updated);
    context.recipeRepository().save(recipe);
    context.println("Imported '" + recipe.getTitle() + "' into '" + collectionTitle + "'");
  }

  private static Path resolveImportPath(String fileArg) {
    Path direct = Path.of(fileArg);
    if (Files.exists(direct)) {
      return direct;
    }

    Path normalized = Path.of(fileArg.replace('\\', '/'));
    if (Files.exists(normalized)) {
      return normalized;
    }

    Matcher matcher = JSON_FILENAME_PATTERN.matcher(fileArg);
    if (matcher.find()) {
      String filename = matcher.group(1);
      Optional<Path> found = findInTempByFilename(filename);
      if (found.isPresent()) {
        return found.get();
      }
    }

    return direct;
  }

  private static Optional<Path> findInTempByFilename(String filename) {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (var stream = Files.walk(tempDir, 4)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> filename.equalsIgnoreCase(p.getFileName().toString()))
          .findFirst();
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
