package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.adapters.MarkdownExporter;
import app.cookyourbooks.adapters.PdfExporter;
import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.PlannerService;
import app.cookyourbooks.services.PlannerServiceImpl;
import app.cookyourbooks.services.ShoppingListAggregator;

/**
 * ViewModel implementation for the Recipe Editor feature.
 *
 * <p>This class maintains mutable UI state while the underlying {@link Recipe} domain object
 * remains immutable. In this first implementation chunk, the ViewModel supports loading recipes,
 * toggling edit mode, title validation, and read accessors for tests.
 */
public final class RecipeEditorViewModelImpl implements RecipeEditorViewModel {

  private final RecipeRepository recipeRepository;
  private final PlannerService plannerService;
  private final BooleanProperty isExporting = new SimpleBooleanProperty(false);

  private final StringProperty title = new SimpleStringProperty("");
  private final ObservableList<EditableIngredient> ingredients =
      FXCollections.observableArrayList();
  private final ObservableList<EditableInstruction> instructions =
      FXCollections.observableArrayList();
  private final BooleanProperty editing = new SimpleBooleanProperty(false);
  private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
  private final BooleanProperty isValid = new SimpleBooleanProperty(false);
  private final BooleanProperty isSaving = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  private @Nullable String recipeId;
  private @Nullable Recipe loadedRecipe;
  private @Nullable String editSessionTitle;
  private List<EditableIngredient> editSessionIngredients = List.of();
  private List<EditableInstruction> editSessionInstructions = List.of();
  private boolean suppressDirtyTracking;

  /**
   * Creates a Recipe Editor ViewModel backed by the given repository.
   *
   * @param recipeRepository repository used to load existing recipes
   */
  public RecipeEditorViewModelImpl(RecipeRepository recipeRepository) {
    this(
        recipeRepository,
        new PlannerServiceImpl(
            new ShoppingListAggregator(), new MarkdownExporter(), new PdfExporter()));
  }

  /**
   * Creates a Recipe Editor ViewModel backed by the given repository and planner service.
   *
   * @param recipeRepository repository used to load existing recipes
   * @param plannerService planner service used for export operations
   */
  public RecipeEditorViewModelImpl(
      RecipeRepository recipeRepository, PlannerService plannerService) {
    this.plannerService = Objects.requireNonNull(plannerService);
    this.recipeRepository = Objects.requireNonNull(recipeRepository);
    title.addListener(
        (obs, oldValue, newValue) -> {
          updateValidity();
          if (shouldTrackDirty()) {
            isDirty.set(true);
          }
        });

    ingredients.addListener(
        (ListChangeListener<EditableIngredient>)
            change -> {
              while (change.next()) {
                if (change.wasAdded()) {
                  for (EditableIngredient added : change.getAddedSubList()) {
                    attachIngredientDirtyTracking(added);
                  }
                }
              }

              if (shouldTrackDirty()) {
                isDirty.set(true);
              }
            });

    instructions.addListener(
        (ListChangeListener<EditableInstruction>)
            change -> {
              while (change.next()) {
                if (change.wasAdded()) {
                  for (EditableInstruction added : change.getAddedSubList()) {
                    attachInstructionDirtyTracking(added);
                  }
                }
              }

              if (shouldTrackDirty()) {
                isDirty.set(true);
              }
            });

    updateValidity();
  }

  @Override
  public StringProperty titleProperty() {
    return title;
  }

  @Override
  public ObservableList<EditableIngredient> ingredientsProperty() {
    return ingredients;
  }

  @Override
  public ObservableList<EditableInstruction> instructionsProperty() {
    return instructions;
  }

  @Override
  public BooleanProperty editingProperty() {
    return editing;
  }

  @Override
  public BooleanProperty isDirtyProperty() {
    return isDirty;
  }

  @Override
  public BooleanProperty isValidProperty() {
    return isValid;
  }

  @Override
  public BooleanProperty isSavingProperty() {
    return isSaving;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public void loadRecipe(String requestedRecipeId) {
    recipeRepository
        .findById(requestedRecipeId)
        .ifPresentOrElse(
            this::loadRecipeIntoEditor, () -> clearForMissingRecipe(requestedRecipeId));
  }

  @Override
  public void toggleEditMode() {
    if (!editing.get()) {
      captureEditSessionBaseline();
      editing.set(true);
      return;
    }
    editing.set(false);
    statusMessage.set("");
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void save() {
    if (isSaving.get()) {
      return;
    }

    if (!isDirty.get()) {
      statusMessage.set("No changes to save.");
      return;
    }

    if (!isValid.get()) {
      statusMessage.set("Cannot save: title is required.");
      return;
    }

    if (recipeId == null || loadedRecipe == null) {
      statusMessage.set("Open a real recipe from Library before saving.");
      return;
    }

    Recipe baseRecipe = loadedRecipe;

    isSaving.set(true);
    statusMessage.set("Saving...");

    BackgroundTaskRunner.run(
        () -> {
          Recipe updated = mergeEditedStateIntoLoadedRecipe(baseRecipe);
          recipeRepository.save(updated);
          return updated;
        },
        savedRecipe -> {
          loadedRecipe = savedRecipe;
          isSaving.set(false);
          isDirty.set(false);
          editing.set(false);
          statusMessage.set("Saved successfully.");
        },
        error -> {
          isSaving.set(false);
          editing.set(true);
          isDirty.set(true);
          String message =
              error.getMessage() == null ? "Save failed." : "Save failed: " + error.getMessage();
          statusMessage.set(message);
        });
  }

  @Override
  public void discardChanges() {
    if (editSessionTitle == null) {
      return;
    }

    suppressDirtyTracking = true;
    try {
      title.set(editSessionTitle);
      ingredients.setAll(cloneIngredientRows(editSessionIngredients));
      instructions.setAll(cloneInstructionRows(editSessionInstructions));
      editing.set(true);
      isDirty.set(false);
      statusMessage.set("Changes discarded.");
      updateValidity();
    } finally {
      suppressDirtyTracking = false;
    }
  }

  @Override
  public void addIngredient() {
    ingredients.add(new EditableIngredient("", ""));
  }

  @Override
  public void removeIngredient(int index) {
    if (index < 0 || index >= ingredients.size()) {
      return;
    }
    ingredients.remove(index);
  }

  @Override
  public void addInstructionStep() {
    instructions.add(new EditableInstruction(""));
  }

  @Override
  public void removeInstructionStep(int index) {
    if (index < 0 || index >= instructions.size()) {
      return;
    }
    instructions.remove(index);
  }

  @Override
  public @Nullable String getRecipeId() {
    return recipeId;
  }

  @Override
  public String getTitle() {
    return title.get();
  }

  @Override
  public int getIngredientCount() {
    return ingredients.size();
  }

  @Override
  public List<String> getIngredientNames() {
    return ingredients.stream().map(EditableIngredient::getName).toList();
  }

  @Override
  public int getInstructionCount() {
    return instructions.size();
  }

  @Override
  public List<String> getInstructionTexts() {
    return instructions.stream().map(EditableInstruction::getText).toList();
  }

  @Override
  public boolean isEditing() {
    return editing.get();
  }

  @Override
  public boolean isDirty() {
    return isDirty.get();
  }

  @Override
  public boolean isValid() {
    return isValid.get();
  }

  @Override
  public boolean isSaving() {
    return isSaving.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  /** Populates editor state from a loaded recipe without triggering dirty tracking. */
  private void loadRecipeIntoEditor(Recipe recipe) {
    suppressDirtyTracking = true;
    try {
      loadedRecipe = recipe;
      applyRecipeToEditor(recipe);
      editing.set(false);
      isDirty.set(false);
      statusMessage.set("");
      updateValidity();
    } finally {
      suppressDirtyTracking = false;
    }
  }

  /** Clears editor state when a requested recipe does not exist. */
  private void clearForMissingRecipe(String missingRecipeId) {
    suppressDirtyTracking = true;
    try {
      recipeId = null;
      loadedRecipe = null;
      title.set("");
      ingredients.clear();
      instructions.clear();
      editing.set(false);
      isDirty.set(false);
      statusMessage.set("Recipe not found: " + missingRecipeId);
      updateValidity();
    } finally {
      suppressDirtyTracking = false;
    }
  }

  /** Returns true when a property/list mutation should mark the editor as dirty. */
  private boolean shouldTrackDirty() {
    return !suppressDirtyTracking && editing.get();
  }

  private void captureEditSessionBaseline() {
    editSessionTitle = title.get();
    editSessionIngredients = cloneIngredientRows(ingredients);
    editSessionInstructions = cloneInstructionRows(instructions);
  }

  private List<EditableIngredient> cloneIngredientRows(List<EditableIngredient> source) {
    return source.stream()
        .map(row -> new EditableIngredient(row.getName(), row.getDescription()))
        .toList();
  }

  private List<EditableInstruction> cloneInstructionRows(List<EditableInstruction> source) {
    return source.stream().map(row -> new EditableInstruction(row.getText())).toList();
  }

  private void applyRecipeToEditor(Recipe recipe) {
    recipeId = recipe.getId();
    title.set(recipe.getTitle());
    ingredients.setAll(
        recipe.getIngredients().stream().map(EditableIngredient::fromIngredient).toList());
    instructions.setAll(
        recipe.getInstructions().stream().map(EditableInstruction::fromInstruction).toList());
  }

  private void attachIngredientDirtyTracking(EditableIngredient ingredient) {
    ingredient
        .nameProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (shouldTrackDirty()) {
                isDirty.set(true);
              }
            });

    ingredient
        .descriptionProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (shouldTrackDirty()) {
                isDirty.set(true);
              }
            });
  }

  private void attachInstructionDirtyTracking(EditableInstruction instruction) {
    instruction
        .textProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (shouldTrackDirty()) {
                isDirty.set(true);
              }
            });
  }

  /** Recomputes validation from current editable field values. */
  private void updateValidity() {
    isValid.set(!title.get().trim().isEmpty());
  }

  private Recipe mergeEditedStateIntoLoadedRecipe(Recipe baseRecipe) {
    List<Ingredient> editedIngredients =
        ingredients.stream().map(EditableIngredient::toIngredient).toList();

    List<Instruction> baseInstructions = baseRecipe.getInstructions();
    List<Instruction> editedInstructions =
        java.util.stream.IntStream.range(0, instructions.size())
            .mapToObj(
                index -> {
                  String text = instructions.get(index).getText();
                  List<IngredientRef> refs =
                      index < baseInstructions.size()
                          ? baseInstructions.get(index).getIngredientRefs()
                          : List.of();
                  return new Instruction(index + 1, text, refs);
                })
            .toList();

    return new Recipe(
        baseRecipe.getId(),
        title.get().trim(),
        baseRecipe.getServings(),
        editedIngredients,
        editedInstructions,
        baseRecipe.getConversionRules());
  }

  @Override
  public BooleanProperty isExportingProperty() {
    return isExporting;
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void exportToPdf(java.nio.file.Path outputPath) {
    if (loadedRecipe == null) {
      return;
    }
    Recipe recipe = loadedRecipe;
    isExporting.set(true);
    BackgroundTaskRunner.run(
        () -> {
          plannerService.exportToPdf(recipe, outputPath);
          return null;
        },
        result -> {
          isExporting.set(false);
          statusMessage.set("PDF exported successfully.");
        },
        error -> {
          isExporting.set(false);
          statusMessage.set("Export failed: " + error.getMessage());
        });
  }
}
