package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import app.cookyourbooks.CybLibrary;
import app.cookyourbooks.gui.view.ImportViewController;
import app.cookyourbooks.gui.view.LibraryViewController;
import app.cookyourbooks.gui.view.MainViewController;
import app.cookyourbooks.gui.view.RecipeEditorViewController;
import app.cookyourbooks.gui.view.SearchViewController;
import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModelImpl;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl;
import app.cookyourbooks.services.LibrarianServiceImpl;
import app.cookyourbooks.services.ocr.FakeRecipeOcrService;

@SuppressWarnings("NullAway.Init")
@ExtendWith(ApplicationExtension.class)
public class ImportToLibraryIntegrationTest {

  private ImportViewModelImpl importVm;

  @Start
  @SuppressWarnings("unused")
  private void start(Stage stage) {
    CybLibrary library = CybLibrary.load(Path.of("cyb-library.json"));
    var librarianService =
        new LibrarianServiceImpl(
            library.getRecipeRepository(), library.getCollectionRepository(), library);
    var navigationService = new NavigationService();
    var mainController = new MainViewController(navigationService);

    // Wire Import with FakeRecipeOcrService instead of real Gemini adapter
    var ocrService = new FakeRecipeOcrService(50);
    importVm = new ImportViewModelImpl(ocrService, librarianService);

    try {
      FXMLLoader importLoader = new FXMLLoader(getClass().getResource("/fxml/ImportView.fxml"));
      importLoader.setControllerFactory(clazz -> new ImportViewController(importVm));
      Parent importView = importLoader.load();
      mainController.setViewNode(NavigationService.View.IMPORT, importView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load ImportView.fxml", e);
    }

    // Wire Library
    try {
      var libraryVm = new LibraryViewModelImpl(librarianService, navigationService);
      FXMLLoader libraryLoader = new FXMLLoader(getClass().getResource("/fxml/LibraryView.fxml"));
      libraryLoader.setControllerFactory(
          clazz -> new LibraryViewController(libraryVm, navigationService));
      Parent libraryView = libraryLoader.load();
      mainController.setViewNode(NavigationService.View.LIBRARY, libraryView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load LibraryView.fxml", e);
    }

    // Wire Recipe Editor
    try {
      var recipeEditorVm = new RecipeEditorViewModelImpl(library.getRecipeRepository());
      FXMLLoader recipeEditorLoader =
          new FXMLLoader(getClass().getResource("/fxml/RecipeEditorView.fxml"));
      recipeEditorLoader.setControllerFactory(
          clazz -> new RecipeEditorViewController(recipeEditorVm, navigationService));
      Parent recipeEditorView = recipeEditorLoader.load();
      mainController.setViewNode(NavigationService.View.RECIPE_EDITOR, recipeEditorView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load RecipeEditorView.fxml", e);
    }

    // Wire Search
    try {
      var searchVm = new SearchViewModelImpl(librarianService, navigationService);
      FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/fxml/SearchView.fxml"));
      searchLoader.setControllerFactory(clazz -> new SearchViewController(searchVm));
      Parent searchView = searchLoader.load();
      mainController.setViewNode(NavigationService.View.SEARCH, searchView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load SearchView.fxml", e);
    }

    // Load main layout
    try {
      FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
      mainLoader.setControllerFactory(clazz -> mainController);
      Parent root = mainLoader.load();
      Scene scene = new Scene(root, 960, 640);
      stage.setTitle("CookYourBooks");
      stage.setScene(scene);
      stage.show();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load MainView.fxml", e);
    }
  }

  @Test
  void importedRecipeAppearsInLibraryAndRecipeView(FxRobot robot) {
    // Step 1: Navigate to Import view
    robot.clickOn("#importButton");

    // Step 2: Trigger import via ViewModel (bypassing FileChooser)
    Platform.runLater(
        () -> {
          importVm.loadCollections();
          importVm.startImport(Path.of("test.jpg"));
        });

    // Step 3: Wait for OCR to finish and review pane to appear
    WaitForAsyncUtils.waitForFxEvents();
    robot.sleep(200);
    WaitForAsyncUtils.waitForFxEvents();

    // Step 4: Select "Easy Recipes" from the collections dropdown and accept
    robot.clickOn("#collectionsDropdown");
    robot.clickOn("Easy Recipes");
    robot.clickOn("#acceptButton");

    // Step 5: Navigate to Library view
    robot.clickOn("#libraryButton");
    robot.sleep(200);
    WaitForAsyncUtils.waitForFxEvents();

    // Step 6: Select the "Easy Recipes" collection
    robot.clickOn("Easy Recipes");

    // Step 7: Verify the imported recipe appears in the recipe list
    robot.clickOn("Imported: test.jpg");

    // Step 8: Open the recipe and verify the title
    robot.clickOn("#viewRecipeButton");
    Label recipeTitle = robot.lookup("#recipe-title-label").queryAs(Label.class);
    assertThat(recipeTitle).isNotNull();
    assertThat(recipeTitle.getText()).isEqualTo("Imported: test.jpg");
  }
}
