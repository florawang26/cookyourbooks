package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
public class LibraryToRecipeViewIntegrationTest {

  @Start
  @SuppressWarnings("unused")
  private void start(Stage stage) {
    CookYourBooksGuiApp app = new CookYourBooksGuiApp();
    app.start(stage);
  }

  @Test
  void clickingRecipeInLibraryShowsRecipeView(FxRobot robot) {
    // Click on the library button in the sidebar to show the library view
    Button libraryBtn = robot.lookup("#libraryButton").queryAs(Button.class);
    robot.clickOn(libraryBtn);

    // Click on the first collection in the library view (should be populated by test data)
    robot.clickOn("Easy Recipes");

    // Click on the first recipe in that collection (should also be populated by test data)
    robot.clickOn("SIMPLE SCRAMBLED EGGS");

    // Open the selected recipe in the editor view
    robot.clickOn("#viewRecipeButton");

    // Verify that the recipe view is displayed with the correct recipe details
    Label recipeTitle = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(recipeTitle).isNotNull();
    assertThat(recipeTitle.getText()).isEqualTo("SIMPLE SCRAMBLED EGGS");
  }

  @Test
  void clickingRecipeInLibraryShowsRecipeViewAndThenReturnsToLibrary(FxRobot robot) {
    // Click on the library button in the sidebar to show the library view
    Button libraryBtn = robot.lookup("#libraryButton").queryAs(Button.class);
    robot.clickOn(libraryBtn);

    // Click on the first collection in the library view (should be populated by test data)
    robot.clickOn("Easy Recipes");

    // Click on the first recipe in that collection (should also be populated by test data)
    robot.clickOn("SIMPLE SCRAMBLED EGGS");

    // Open the selected recipe in the editor view
    robot.clickOn("#viewRecipeButton");

    // Verify that the recipe view is displayed with the correct recipe details
    Label recipeTitle = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(recipeTitle).isNotNull();
    assertThat(recipeTitle.getText()).isEqualTo("SIMPLE SCRAMBLED EGGS");

    // Click the back button to return to the library view
    Button backButton = robot.lookup("#recipe-back-button").queryAs(Button.class);
    robot.clickOn(backButton);

    // Verify that we are back in the library view by checking for a known element
    Button addCollectionBtn = robot.lookup("#newCollectionButton").queryAs(Button.class);
    assertThat(addCollectionBtn).isNotNull();
  }

  @Test
  void doubleClickingRecipeInLibraryShowsRecipeView(FxRobot robot) {
    // Click on the library button in the sidebar to show the library view
    Button libraryBtn = robot.lookup("#libraryButton").queryAs(Button.class);
    robot.clickOn(libraryBtn);

    // Click on the first collection in the library view (should be populated by test data)
    robot.clickOn("Easy Recipes");

    // Double-click on the first recipe in that collection (should also be populated by test data)
    robot.doubleClickOn("SIMPLE SCRAMBLED EGGS");

    // Verify that the recipe view is displayed with the correct recipe details
    Label recipeTitle = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(recipeTitle).isNotNull();
    assertThat(recipeTitle.getText()).isEqualTo("SIMPLE SCRAMBLED EGGS");
  }
}
