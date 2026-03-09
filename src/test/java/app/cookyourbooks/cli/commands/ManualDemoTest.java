package app.cookyourbooks.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.TestRecipeBuilder;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

/**
 * Manual demo tests that exercise formatting paths not fully verified by automated tests.
 *
 * <p>These tests generate output files in {@code build/manual-demo-output/} for human review. Run
 * with: {@code ./gradlew test --tests ManualDemoTest}
 *
 * <p>Grading: 6 points total (2 per test). See cyb5.md for detailed grading criteria.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ManualDemoTest extends CliTestBase {

  private static final Path OUTPUT_DIR = Path.of("build/manual-demo-output");

  @BeforeEach
  void setupOutputDir() throws Exception {
    Files.createDirectories(OUTPUT_DIR);
  }

  private void saveOutput(String filename) throws IOException {
    Path outFile = OUTPUT_DIR.resolve(filename);
    Files.writeString(outFile, getOutput());
    System.out.println("Output saved to: " + outFile.toAbsolutePath());
  }

  /**
   * Exercises RecipeFormatter.formatFull() and ComparisonFormatter paths.
   *
   * <p>Grading checklist (2 points):
   *
   * <ul>
   *   <li>Recipe box uses decorative borders (═══)
   *   <li>Ingredients use bullet points (•)
   *   <li>Scale comparison shows "Ingredient", "Original", "Scaled" column headers
   *   <li>Scale comparison shows arrows (→) between original and scaled values
   *   <li>Convert comparison shows unit name in uppercase (e.g., "GRAM")
   *   <li>Vague ingredients display "to taste" in both original and scaled columns
   * </ul>
   */
  @Test
  void demoRecipeDisplayAndTransform() throws Exception {
    Recipe recipe =
        TestRecipeBuilder.recipe("Grandmother's Apple Pie")
            .serves(8)
            .withRangeQuantityIngredient("flour", 2, 3, Unit.CUP)
            .withIngredient("butter", 1, Unit.CUP, "softened")
            .withIngredient("sugar", 0.5, Unit.CUP)
            .withIngredient("apples", 6, Unit.WHOLE, "peeled and sliced")
            .withVagueIngredient("cinnamon", "to taste")
            .withVagueIngredient("salt", "a pinch")
            .withStep("Preheat oven to 375°F")
            .withStep("Mix flour and salt in a large bowl")
            .withStep("Cut butter into flour until crumbly")
            .withStep("Arrange apples in pie crust")
            .withStep("Sprinkle with sugar and cinnamon")
            .withStep("Bake for 45 minutes until golden")
            .build();

    setupRecipeInCollection(recipe, "Family Recipes");

    sendCommands(
        "show \"Grandmother's Apple Pie\"",
        "scale \"Grandmother's Apple Pie\" 16",
        "n",
        "convert \"Grandmother's Apple Pie\" gram",
        "n",
        "quit");
    runCli();

    saveOutput("recipe-transform-demo.txt");
  }

  /**
   * Exercises CookModeController and cook-mode formatting paths.
   *
   * <p>Grading checklist (2 points):
   *
   * <ul>
   *   <li>Cook header shows "COOKING: Recipe Name" with decorative border (═══)
   *   <li>Ingredients display in compact two-column layout
   *   <li>Each step shows separator line (───────)
   *   <li>Step counter shows "Step N of M" format
   *   <li>Steps with ingredients show "Uses: quantity ingredient, ..." format
   *   <li>Steps without ingredients show "(no ingredients used in this step)"
   *   <li>Hints bar shows "[next] [prev] [ingredients] [quit]"
   *   <li>Finish message shows "Finished cooking Recipe Name! Enjoy!"
   * </ul>
   */
  @Test
  void demoCookModeWalkthrough() throws Exception {
    var flour = new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null);
    var butter = new MeasuredIngredient("butter", new ExactQuantity(0.5, Unit.CUP), "melted", null);
    var eggs = new MeasuredIngredient("eggs", new ExactQuantity(2, Unit.WHOLE), "beaten", null);
    var milk = new MeasuredIngredient("milk", new ExactQuantity(1, Unit.CUP), null, null);
    var sugar = new MeasuredIngredient("sugar", new ExactQuantity(0.25, Unit.CUP), null, null);

    var flourRef = new IngredientRef(flour, new ExactQuantity(2, Unit.CUP));
    var butterRef = new IngredientRef(butter, new ExactQuantity(0.5, Unit.CUP));
    var eggsRef = new IngredientRef(eggs, new ExactQuantity(2, Unit.WHOLE));
    var milkRef = new IngredientRef(milk, new ExactQuantity(1, Unit.CUP));
    var sugarRef = new IngredientRef(sugar, new ExactQuantity(0.25, Unit.CUP));

    Recipe recipe =
        TestRecipeBuilder.recipe("Classic Pancakes")
            .serves(4)
            .withIngredient("flour", 2, Unit.CUP)
            .withIngredient("butter", 0.5, Unit.CUP, "melted")
            .withIngredient("eggs", 2, Unit.WHOLE, "beaten")
            .withIngredient("milk", 1, Unit.CUP)
            .withIngredient("sugar", 0.25, Unit.CUP)
            .withVagueIngredient("salt", "a pinch")
            .withVagueIngredient("baking powder", "1 tsp")
            .withStepWithRefs(
                "Whisk flour, sugar, salt, and baking powder in a large bowl",
                List.of(flourRef, sugarRef))
            .withStepWithRefs(
                "In another bowl, beat eggs and mix with milk", List.of(eggsRef, milkRef))
            .withStep("Pour wet ingredients into dry and stir until just combined")
            .withStepWithRefs("Melt butter and fold into batter", List.of(butterRef))
            .withStep(
                "Heat griddle over medium heat and cook pancakes until bubbles form, then flip")
            .build();

    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands(
        "cook \"Classic Pancakes\"",
        "next",
        "next",
        "prev",
        "ingredients",
        "next",
        "next",
        "next",
        "next",
        "quit");
    runCli();

    saveOutput("cook-mode-demo.txt");
  }

  /**
   * Exercises CollectionFormatter, ShoppingListFormatter, and AmbiguousMatchFormatter.
   *
   * <p>Grading checklist (2 points):
   *
   * <ul>
   *   <li>Collections list shows numbered items (1., 2., ...)
   *   <li>Collections show type badges [Personal]
   *   <li>Collections show recipe counts ("N recipes")
   *   <li>Recipe listing shows "Serves N" for recipes with servings
   *   <li>Search results show collection name in parentheses for each match
   *   <li>Ambiguous match shows short IDs in brackets [xxxxxxxx]
   *   <li>Ambiguous match hint is context-appropriate (same-title vs different-title)
   *   <li>House conversions show "1 unit ingredient = amount unit" format
   *   <li>Shopping list shows "Measured Items:" section header
   *   <li>Shopping list shows "Also needed:" section for vague items
   *   <li>Shopping list shows "Total: N measured items, M vague items"
   * </ul>
   */
  @Test
  void demoLibraryAndShoppingList() throws Exception {
    Recipe pancakes =
        TestRecipeBuilder.recipe("Classic Pancakes")
            .serves(4)
            .withIngredient("flour", 2, Unit.CUP)
            .withIngredient("milk", 1, Unit.CUP)
            .withIngredient("eggs", 2, Unit.WHOLE)
            .withVagueIngredient("salt", "to taste")
            .withStep("Mix and cook")
            .build();

    Recipe waffles =
        TestRecipeBuilder.recipe("Belgian Waffles")
            .serves(6)
            .withIngredient("flour", 1.5, Unit.CUP)
            .withIngredient("milk", 1.25, Unit.CUP)
            .withIngredient("eggs", 2, Unit.WHOLE)
            .withIngredient("butter", 0.5, Unit.CUP, "melted")
            .withVagueIngredient("vanilla extract", "1 tsp")
            .withVagueIngredient("salt", "a pinch")
            .withStep("Mix and cook in waffle iron")
            .build();

    Recipe chocolateCake1 =
        TestRecipeBuilder.recipe("Chocolate Cake")
            .serves(12)
            .withIngredient("flour", 2, Unit.CUP)
            .withIngredient("cocoa powder", 0.75, Unit.CUP)
            .withIngredient("sugar", 2, Unit.CUP)
            .withVagueIngredient("salt", "to taste")
            .withStep("Mix and bake")
            .build();

    Recipe chocolateCake2 =
        TestRecipeBuilder.recipe("Chocolate Cake")
            .serves(8)
            .withIngredient("flour", 1.5, Unit.CUP)
            .withIngredient("cocoa powder", 0.5, Unit.CUP)
            .withIngredient("sugar", 1.5, Unit.CUP)
            .withStep("Mix and bake")
            .build();

    setupRecipeInCollection(pancakes, "Breakfast Favorites");
    setupRecipeInCollection(waffles, "Breakfast Favorites");
    setupRecipeInCollection(chocolateCake1, "Desserts");
    setupRecipeInCollection(chocolateCake2, "Holiday Treats");

    sendCommands(
        "collections",
        "recipes \"Breakfast Favorites\"",
        "recipes Desserts",
        "search flour",
        "show \"Chocolate Cake\"",
        "conversion add",
        "1",
        "cup",
        "flour",
        "120",
        "g",
        "conversions",
        "shopping-list \"Classic Pancakes\" \"Belgian Waffles\"",
        "quit");
    runCli();

    saveOutput("library-lists-demo.txt");
  }
}
