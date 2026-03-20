package app.cookyourbooks;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.CookYourBooksCli;
import app.cookyourbooks.cli.completion.CookModeHolder;
import app.cookyourbooks.cli.completion.CybCompleter;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.PlannerService;
import app.cookyourbooks.services.TransformerService;

/** Main entry point for CookYourBooks CLI. */
public final class CookYourBooksApp {

  private CookYourBooksApp() {}

  /**
   * Creates a test harness for CLI E2E testing.
   *
   * <p><b>IMPORTANT:</b> You MUST implement this method. The test suite calls this to create a
   * fully-wired CLI. Your implementation must:
   *
   * <ul>
   *   <li>Load the library from {@code libraryPath}
   *   <li>Create your services and wire them together
   *   <li>Create your CLI (must be a {@link Runnable} with a {@code run()} method)
   *   <li>Create your completer (must implement JLine's {@link Completer} interface)
   *   <li>Use the provided {@link CookModeHolder} for cook mode state
   *   <li>Return a {@link TestCliHarness} with all components wired
   * </ul>
   *
   * @param libraryPath path to the library JSON file (e.g. cyb-library.json)
   * @param terminal JLine terminal (dumb terminal with piped streams for tests)
   * @return a harness with run(), getCompletionValues(), and setCookMode()
   */
  @SuppressWarnings("UnusedVariable") // Variables are hints for student implementation
  public static TestCliHarness createTestHarness(Path libraryPath, Terminal terminal) {
    CybLibrary library = CybLibrary.load(libraryPath);
    var recipeRepo = library.getRecipeRepository();
    var collRepo = library.getCollectionRepository();
    var conversionRegistry = library.getConversionRegistry();

    LibrarianService librarianService =
        new LibrarianService(collRepo, recipeRepo, conversionRegistry);
    PlannerService plannerService = new PlannerService();
    TransformerService transformerService =
        new TransformerService(librarianService::getConversionRegistry);

    // REQUIRED: Use this CookModeHolder for cook mode state
    CookModeHolder cookModeHolder = new CookModeHolder();

    Completer completer = new CybCompleter(cookModeHolder, recipeRepo, collRepo, librarianService);

    DefaultParser parser = new DefaultParser();
    parser.setEscapeChars(null);

    LineReader lineReader =
        LineReaderBuilder.builder().terminal(terminal).parser(parser).completer(completer).build();
    PrintWriter out = new PrintWriter(terminal.writer(), true);

    CliContext context =
        new CliContext(
            terminal,
            lineReader,
            out,
            cookModeHolder,
            librarianService,
            plannerService,
            transformerService,
            recipeRepo,
            collRepo);

    Runnable cliRunner = new CookYourBooksCli(context);

    return new TestCliHarness(cliRunner, completer, lineReader, cookModeHolder);
  }

  /** Test harness for CLI E2E tests. Opaque wrapper around wired CLI components. */
  public static final class TestCliHarness {

    private final Runnable cliRunner;
    private final Completer completer;
    private final LineReader lineReader;
    private final CookModeHolder cookModeHolder;

    /**
     * Creates a test harness.
     *
     * @param cliRunner the CLI main loop (Runnable)
     * @param completer JLine completer for tab completion
     * @param lineReader LineReader used by the CLI (must have the completer attached)
     * @param cookModeHolder holder for cook mode state
     */
    public TestCliHarness(
        Runnable cliRunner,
        Completer completer,
        LineReader lineReader,
        CookModeHolder cookModeHolder) {
      this.cliRunner = cliRunner;
      this.completer = completer;
      this.lineReader = lineReader;
      this.cookModeHolder = cookModeHolder;
    }

    /** Runs the CLI main loop. */
    public void run() {
      cliRunner.run();
    }

    /**
     * Gets completion candidates for a partial command. Cursor is at end of the string.
     *
     * @param partialCommand the command line as typed so far
     * @return list of completion values
     */
    public List<String> getCompletionValues(String partialCommand) {
      var parser = lineReader.getParser();
      int cursor = partialCommand.length();
      ParsedLine parsed = parser.parse(partialCommand, cursor);
      List<Candidate> candidates = new ArrayList<>();
      completer.complete(lineReader, parsed, candidates);
      return candidates.stream().map(Candidate::value).toList();
    }

    /**
     * Sets cook mode state for completion testing. Cook-mode-specific completions (next, prev,
     * ingredients, quit) only appear when in cook mode.
     *
     * @param inCookMode true if simulating cook mode for completion tests
     */
    public void setCookMode(boolean inCookMode) {
      cookModeHolder.setInCookMode(inCookMode);
    }
  }

  @SuppressWarnings("UnusedVariable") // Variables are hints for student implementation
  public static void main(String[] args) {
    Path libraryPath = Path.of("cyb-library.json");

    Terminal terminal;
    try {
      terminal = TerminalBuilder.builder().system(true).build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create terminal", e);
    }

    createTestHarness(libraryPath, terminal).run();
  }
}
