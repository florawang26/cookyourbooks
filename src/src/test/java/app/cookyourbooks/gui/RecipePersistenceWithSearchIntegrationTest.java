package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/** End-to-end integration tests for recipe persistence effects on Search results. */
@ExtendWith(ApplicationExtension.class)
class RecipePersistenceWithSearchIntegrationTest {

  private static final String COLLECTION_TITLE = "Easy Recipes";
  private static final String RECIPE_TITLE = "SIMPLE SCRAMBLED EGGS";

  @SuppressWarnings("UnusedMethod")
  @Start
  private void start(Stage stage) {
    CookYourBooksGuiApp app = new CookYourBooksGuiApp();
    app.start(stage);
  }

  @Test
  void searchPath_renamingIngredient_updatesSearchResults(FxRobot robot) throws Exception {
    String oldIngredient = "eggs";
    String newIngredient = "fx-it-renamed-" + UUID.randomUUID().toString().substring(0, 8);

    try {
      openSearchView(robot);
      filterByIngredient(robot, oldIngredient);
      waitForSearchSettled(robot);
      assertThat(resultsContainTitle(robot, RECIPE_TITLE)).isTrue();

      openRecipeFromSearchResults(robot, RECIPE_TITLE);
      renameIngredient(robot, oldIngredient, newIngredient);
      saveAndWaitForSuccess(robot);

      openSearchView(robot);
      filterByIngredient(robot, oldIngredient);
      waitForSearchSettled(robot);
      assertThat(resultsContainTitle(robot, RECIPE_TITLE)).isFalse();

      filterByIngredient(robot, newIngredient);
      waitForSearchSettled(robot);
      assertThat(resultsContainTitle(robot, RECIPE_TITLE)).isTrue();
    } finally {
      // Restore baseline data so repeated local runs remain stable.
      attemptRestoreIngredientName(robot, newIngredient, oldIngredient);
    }
  }

  @Test
  void libraryPath_addingIngredient_createsNewSearchMatch(FxRobot robot) throws Exception {
    String addedIngredient = "fx-it-added-" + UUID.randomUUID().toString().substring(0, 8);

    openSearchView(robot);
    filterByIngredient(robot, addedIngredient);
    waitForSearchSettled(robot);
    assertThat(resultsContainTitle(robot, RECIPE_TITLE)).isFalse();

    openRecipeFromLibrary(robot, COLLECTION_TITLE, RECIPE_TITLE);
    addIngredient(robot, addedIngredient);
    saveAndWaitForSuccess(robot);

    openSearchView(robot);
    filterByIngredient(robot, addedIngredient);
    waitForSearchSettled(robot);
    assertThat(resultsContainTitle(robot, RECIPE_TITLE)).isTrue();
  }

  private void openSearchView(FxRobot robot) throws InterruptedException {
    Button searchButton = robot.lookup("#searchButton").queryAs(Button.class);
    robot.clickOn(searchButton);
    waitUntil(() -> robot.lookup("#ingredientFilterField").tryQuery().isPresent());
  }

  private void openRecipeFromSearchResults(FxRobot robot, String recipeTitle)
      throws InterruptedException {
    waitUntil(() -> resultsContainTitle(robot, recipeTitle));
    robot.doubleClickOn(recipeTitle);
    waitUntil(() -> robot.lookup("#recipe-title-label").tryQuery().isPresent());

    Label titleLabel = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(titleLabel.getText()).isEqualTo(recipeTitle);
  }

  private void openRecipeFromLibrary(FxRobot robot, String collectionTitle, String recipeTitle)
      throws InterruptedException {
    Button libraryButton = robot.lookup("#libraryButton").queryAs(Button.class);
    robot.clickOn(libraryButton);

    waitUntil(() -> robot.lookup(collectionTitle).tryQuery().isPresent());
    robot.clickOn(collectionTitle);

    waitUntil(() -> robot.lookup(recipeTitle).tryQuery().isPresent());
    robot.clickOn(recipeTitle);

    Button viewRecipeButton = robot.lookup("#viewRecipeButton").queryAs(Button.class);
    robot.clickOn(viewRecipeButton);

    waitUntil(() -> robot.lookup("#recipe-title-label").tryQuery().isPresent());
    Label titleLabel = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(titleLabel.getText()).isEqualTo(recipeTitle);
  }

  private void filterByIngredient(FxRobot robot, String ingredient) throws InterruptedException {
    Button clearButton = robot.lookup("#clearButton").queryAs(Button.class);
    robot.clickOn(clearButton);
    waitForSearchSettled(robot);

    TextField ingredientField = robot.lookup("#ingredientFilterField").queryAs(TextField.class);
    robot.clickOn(ingredientField);
    robot.eraseText(ingredientField.getText().length());
    robot.write(ingredient);

    Button addButton = robot.lookup("#addFilterButton").queryAs(Button.class);
    robot.clickOn(addButton);
  }

  private void renameIngredient(FxRobot robot, String oldName, String newName)
      throws InterruptedException {
    Button editButton = robot.lookup("#recipe-edit-button").queryAs(Button.class);
    robot.clickOn(editButton);

    TextField ingredientField = findIngredientFieldByText(oldName);
    robot.clickOn(ingredientField);
    robot.push(KeyCode.SHORTCUT, KeyCode.A);
    robot.eraseText(oldName.length() + 10);
    robot.write(newName);
  }

  private void addIngredient(FxRobot robot, String ingredientName) throws InterruptedException {
    Button editButton = robot.lookup("#recipe-edit-button").queryAs(Button.class);
    robot.clickOn(editButton);

    Button addIngredientButton = robot.lookup("#recipe-add-ingredient").queryAs(Button.class);
    robot.clickOn(addIngredientButton);

    waitUntil(() -> robot.lookup("Ingredient").tryQuery().isPresent());
    robot.clickOn("Ingredient");
    robot.write(ingredientName);
  }

  private void saveAndWaitForSuccess(FxRobot robot) throws InterruptedException {
    Button saveButton = robot.lookup("#recipe-save-button").queryAs(Button.class);
    robot.clickOn(saveButton);

    waitUntil(
        () -> {
          Label status = robot.lookup("#recipe-status-label").queryAs(Label.class);
          return "Saved successfully.".equals(status.getText());
        });
  }

  private void attemptRestoreIngredientName(FxRobot robot, String currentName, String targetName) {
    try {
      openSearchView(robot);
      filterByIngredient(robot, currentName);
      waitForSearchSettled(robot);

      if (!resultsContainTitle(robot, RECIPE_TITLE)) {
        return;
      }

      openRecipeFromSearchResults(robot, RECIPE_TITLE);
      renameIngredient(robot, currentName, targetName);
      saveAndWaitForSuccess(robot);
    } catch (Exception ignored) {
      // Best-effort cleanup only.
    }
  }

  private boolean resultsContainTitle(FxRobot robot, String recipeTitle) {
    ListView<?> resultsList = robot.lookup("#resultsListView").queryAs(ListView.class);
    for (Object item : fxSnapshotItems(resultsList)) {
      if (item == null) {
        continue;
      }

      String title = tryExtractTitle(item);
      if (recipeTitle.equals(title)) {
        return true;
      }
    }
    return false;
  }

  private static java.util.List<?> fxSnapshotItems(ListView<?> listView) {
    AtomicReference<java.util.List<?>> ref = new AtomicReference<>(java.util.List.of());
    runOnFxThread(
        () -> {
          ref.set(java.util.List.copyOf(listView.getItems()));
        });
    return Objects.requireNonNull(ref.get());
  }

  private TextField findIngredientFieldByText(String expectedText) throws InterruptedException {
    AtomicReference<TextField> ref = new AtomicReference<>();

    runOnFxThread(
        () -> {
          Set<Node> nodes =
              javafx.stage.Window.getWindows().stream()
                  .filter(window -> window instanceof Stage)
                  .map(window -> ((Stage) window).getScene())
                  .filter(scene -> scene != null)
                  .flatMap(scene -> scene.getRoot().lookupAll(".recipe-ingredient-name").stream())
                  .collect(java.util.stream.Collectors.toSet());

          for (Node node : nodes) {
            if (node instanceof TextField field && expectedText.equals(field.getText())) {
              ref.set(field);
              break;
            }
          }
        });

    waitUntil(() -> ref.get() != null);
    return Objects.requireNonNull(ref.get());
  }

  private static String tryExtractTitle(Object item) {
    try {
      Method titleMethod = item.getClass().getMethod("title");
      Object value = titleMethod.invoke(item);
      return value == null ? "" : value.toString();
    } catch (Exception ignored) {
      return item.toString();
    }
  }

  private void waitForSearchSettled(FxRobot robot) throws InterruptedException {
    waitUntil(
        () -> {
          ProgressIndicator indicator =
              robot.lookup("#searchingIndicator").queryAs(ProgressIndicator.class);
          return !indicator.isVisible();
        });
  }

  private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > deadline) {
        throw new AssertionError("Timed out waiting for condition");
      }
      Thread.sleep(50);
    }
  }

  private static void runOnFxThread(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      runnable.run();
      return;
    }

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            runnable.run();
          } finally {
            latch.countDown();
          }
        });

    try {
      boolean completed = latch.await(3, TimeUnit.SECONDS);
      if (!completed) {
        throw new AssertionError("Timed out waiting for FX thread task");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for FX thread task", e);
    }
  }
}
