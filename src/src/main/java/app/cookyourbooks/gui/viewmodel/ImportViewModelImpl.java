package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.RecipeOcrService;

public class ImportViewModelImpl implements ImportViewModel {

  private final RecipeOcrService ocrService;
  private final LibrarianService librarianService;
  @Nullable private Recipe importedRecipe = null;

  private final StringProperty statusMessage = new SimpleStringProperty("Ready");
  private final StringProperty errorMessage = new SimpleStringProperty("");

  // current state (one of: "idle", "processing", "review", "error")
  private final StringProperty state = new SimpleStringProperty("idle");
  // imported recipe title
  private final StringProperty importedTitle = new SimpleStringProperty("");
  // selected collection ID
  private final StringProperty selectedCollectionId = new SimpleStringProperty("");

  // exposed so the View can bind to the state machine directly if needed
  public StringProperty stateProperty() {
    return state;
  }

  // list of available collections
  private final ObservableList<CollectionSummary> availableCollections =
      FXCollections.observableArrayList();
  // list of imported ingredients
  private final ObservableList<String> importedIngredients = FXCollections.observableArrayList();

  public ImportViewModelImpl(RecipeOcrService ocrService, LibrarianService librarianService) {
    this.ocrService = ocrService;
    this.librarianService = librarianService;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public StringProperty errorMessageProperty() {
    return errorMessage;
  }

  @Override
  public StringProperty importedTitleProperty() {
    return importedTitle;
  }

  @Override
  public ObservableList<String> importedIngredientsProperty() {
    return importedIngredients;
  }

  @Override
  public ObservableList<ImportViewModel.CollectionSummary> availableCollectionsProperty() {
    return availableCollections;
  }

  @Override
  public String getState() {
    return state.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public String getErrorMessage() {
    return errorMessage.get();
  }

  @Override
  public String getImportedRecipeTitle() {
    return importedTitle.get();
  }

  @Override
  public String getSelectedCollectionId() {
    return selectedCollectionId.get();
  }

  @Override
  public List<String> getImportedIngredientNames() {
    return new ArrayList<>(importedIngredients);
  }

  @Override
  public List<String> getAvailableCollectionIds() {
    return availableCollections.stream().map(CollectionSummary::id).toList();
  }

  @Override
  public void startImport(Path imagePath) {
    state.set("processing");
    statusMessage.set("Extracting recipe...");

    BackgroundTaskRunner.run(
        () -> ocrService.extractRecipe(imagePath),
        recipe -> {
          // fill in success: state, importedTitle, statusMessage
          importedRecipe = recipe;
          state.set("review");
          importedTitle.set(recipe.getTitle());
          statusMessage.set("Recipe extracted successfully!");
          importedIngredients.clear();
          for (Ingredient ingredient : recipe.getIngredients()) {
            importedIngredients.add(ingredient.getName());
          }
        },
        error -> {
          state.set("error");
          errorMessage.set(error.getMessage());
        });
  }

  @Override
  public void cancelImport() {
    state.set("idle");
    statusMessage.set("Ready");
    importedRecipe = null;
  }

  @Override
  public void rejectImport() {
    state.set("idle");
    importedTitle.set("");
    importedIngredients.clear();
    statusMessage.set("Ready");
    importedRecipe = null;
  }

  @Override
  public void selectTargetCollection(String collectionId) {
    selectedCollectionId.set(collectionId);
  }

  @Override
  public void loadCollections() {
    availableCollections.setAll(
        librarianService.listCollections().stream()
            .map(c -> new CollectionSummary(c.getId(), c.getTitle()))
            .toList());
  }

  @Override
  public void acceptImport() {
    if (importedRecipe == null || selectedCollectionId.get().isEmpty()) {
      return; // no-op
    }

    librarianService.saveRecipe(importedRecipe, selectedCollectionId.get());
    state.set("idle");
    statusMessage.set("Recipe saved!");
    importedTitle.set("");
    importedIngredients.clear();
    importedRecipe = null;
  }
}
