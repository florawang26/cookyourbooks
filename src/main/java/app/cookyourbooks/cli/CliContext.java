package app.cookyourbooks.cli;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.completion.CookModeHolder;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.PlannerService;
import app.cookyourbooks.services.TransformerService;

/**
 * Shared context for CLI commands: I/O and state.
 *
 * <p><b>IMPORTANT:</b> You may ADD additional fields to this record (e.g., your service
 * references), but you MUST NOT remove the existing fields. The test harness depends on {@link
 * #cookModeHolder()} being present.
 *
 * <p>Example of extending this record with your services:
 *
 * <pre>{@code
 * public record CliContext(
 *     Terminal terminal,
 *     LineReader lineReader,
 *     PrintWriter out,
 *     CookModeHolder cookModeHolder,
 *     // Add your services below:
 *     MyLibrarianService librarianService,
 *     MyPlannerService plannerService) { ... }
 * }</pre>
 */
public record CliContext(
    @NonNull Terminal terminal,
    @NonNull LineReader lineReader,
    @NonNull PrintWriter out,
    @NonNull CookModeHolder cookModeHolder,
    @NonNull LibrarianService librarianService,
    @NonNull PlannerService plannerService,
    @NonNull TransformerService transformerService,
    @NonNull RecipeRepository recipeRepository,
    @NonNull RecipeCollectionRepository collectionRepository) {

  /** Writes a line to the terminal output. */
  public void println(String s) {
    out.println(s);
    out.flush();
  }

  /** Writes text to the terminal output (no newline). */
  public void print(String s) {
    out.print(s);
    out.flush();
  }

  /** Reads a line from the user with the given prompt. */
  public String readLine(String prompt) {
    return lineReader.readLine(prompt);
  }
}
