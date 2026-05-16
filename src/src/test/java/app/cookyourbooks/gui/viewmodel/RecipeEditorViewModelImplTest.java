package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.repository.RecipeRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway.Init")
class RecipeEditorViewModelImplTest extends ViewModelTestBase {

  @Mock private RecipeRepository recipeRepository;

  @Test
  void loadRecipe_populatesTitleAndIngredients() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk", "salt"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");

    assertThat(vm.getRecipeId()).isEqualTo("recipe-1");
    assertThat(vm.getTitle()).isEqualTo("Pancakes");
    assertThat(vm.getIngredientCount()).isEqualTo(3);
    assertThat(vm.getIngredientNames()).containsExactly("flour", "milk", "salt");
    assertThat(vm.getInstructionTexts()).containsExactly("Mix ingredients", "Cook on griddle");
    assertThat(vm.isDirty()).isFalse();
    assertThat(vm.isEditing()).isFalse();
  }

  @Test
  void toggleEditMode_flipsEditingState() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    assertThat(vm.isEditing()).isFalse();

    vm.toggleEditMode();
    assertThat(vm.isEditing()).isTrue();

    vm.toggleEditMode();
    assertThat(vm.isEditing()).isFalse();
  }

  @Test
  void toggleEditMode_exitingEditMode_clearsStatusMessage() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    vm.statusMessageProperty().set("Temporary status");
    vm.toggleEditMode();
    vm.toggleEditMode();

    assertThat(vm.isEditing()).isFalse();
    assertThat(vm.getStatusMessage()).isEmpty();
  }

  @Test
  void isValid_falseWhenTitleBlank_trueWhenNonBlank() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    assertThat(vm.isValid()).isFalse();

    vm.titleProperty().set("Chocolate Cake");
    assertThat(vm.isValid()).isTrue();

    vm.titleProperty().set("   ");
    assertThat(vm.isValid()).isFalse();
  }

  @Test
  void loadRecipe_nonexistentId_clearsStaleEditorStateAndSetsStatus() {
    when(recipeRepository.findById("missing")).thenReturn(Optional.empty());
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    vm.loadRecipe("missing");

    assertThat(vm.getRecipeId()).isNull();
    assertThat(vm.getTitle()).isEmpty();
    assertThat(vm.getIngredientCount()).isEqualTo(0);
    assertThat(vm.getStatusMessage()).contains("Recipe not found");
    assertThat(vm.isDirty()).isFalse();
    assertThat(vm.isEditing()).isFalse();
  }

  @Test
  void titleEdit_inEditMode_setsDirty() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();

    vm.titleProperty().set("Better Pancakes");

    assertThat(vm.isDirty()).isTrue();
  }

  @Test
  void ingredientFieldEdit_inEditMode_setsDirty() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();

    EditableIngredient first = vm.ingredientsProperty().get(0);
    first.setName("all-purpose flour");

    assertThat(vm.isDirty()).isTrue();
  }

  @Test
  void discardChanges_revertsToBaseline_clearsDirty_andStaysInEditMode() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();
    vm.titleProperty().set("Temporary Title");
    vm.addIngredient();
    vm.instructionsProperty().get(0).setText("Changed step");

    vm.discardChanges();

    assertThat(vm.getTitle()).isEqualTo("Pancakes");
    assertThat(vm.getIngredientNames()).containsExactly("flour", "milk");
    assertThat(vm.getInstructionTexts()).containsExactly("Mix ingredients", "Cook on griddle");
    assertThat(vm.isDirty()).isFalse();
    assertThat(vm.isEditing()).isTrue();
  }

  @Test
  void addAndRemoveIngredient_updatesList_andInvalidRemoveIsNoOp() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    vm.addIngredient();
    assertThat(vm.getIngredientCount()).isEqualTo(1);

    vm.removeIngredient(5);
    assertThat(vm.getIngredientCount()).isEqualTo(1);

    vm.removeIngredient(0);
    assertThat(vm.getIngredientCount()).isEqualTo(0);
  }

  @Test
  void addAndRemoveInstructionStep_updatesList_andInvalidRemoveIsNoOp() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);

    vm.addInstructionStep();
    assertThat(vm.getInstructionCount()).isEqualTo(1);

    vm.removeInstructionStep(3);
    assertThat(vm.getInstructionCount()).isEqualTo(1);

    vm.removeInstructionStep(0);
    assertThat(vm.getInstructionCount()).isEqualTo(0);
  }

  @Test
  void instructionEdit_inEditMode_setsDirty() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();

    vm.instructionsProperty().get(1).setText("Cook until golden and fluffy");

    assertThat(vm.isDirty()).isTrue();
  }

  @Test
  // E7: save() persists the edited recipe.
  void save_persistsEditedRecipe_andClearsDirtyAndEditModeOnSuccess() throws Exception {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();
    vm.titleProperty().set("Better Pancakes");
    vm.ingredientsProperty().get(0).setName("all-purpose flour");
    vm.instructionsProperty().get(0).setText("Whisk ingredients thoroughly");

    vm.save();
    waitForCondition(() -> !vm.isSaving());

    ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
    verify(recipeRepository).save(recipeCaptor.capture());
    Recipe savedRecipe = recipeCaptor.getValue();

    assertThat(savedRecipe.getId()).isEqualTo("recipe-1");
    assertThat(savedRecipe.getTitle()).isEqualTo("Better Pancakes");
    assertThat(savedRecipe.getIngredients())
        .extracting(Ingredient::getName)
        .containsExactly("all-purpose flour", "milk");
    assertThat(savedRecipe.getInstructions())
        .extracting(Instruction::getText)
        .containsExactly("Whisk ingredients thoroughly", "Cook on griddle");

    assertThat(vm.isDirty()).isFalse();
    assertThat(vm.isEditing()).isFalse();
    assertThat(vm.getStatusMessage()).isEqualTo("Saved successfully.");
  }

  @Test
  // E8: save() is async and isSaving is true while save is in progress.
  void save_setsIsSavingTrueWhileBackgroundSaveInProgress() throws Exception {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    CountDownLatch saveStarted = new CountDownLatch(1);
    CountDownLatch allowSaveToFinish = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              saveStarted.countDown();
              allowSaveToFinish.await();
              return null;
            })
        .when(recipeRepository)
        .save(any(Recipe.class));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();
    vm.titleProperty().set("Better Pancakes");

    vm.save();
    assertThat(saveStarted.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    waitForCondition(vm::isSaving);
    assertThat(vm.isSaving()).isTrue();

    allowSaveToFinish.countDown();
    waitForCondition(() -> !vm.isSaving());
    assertThat(vm.isSaving()).isFalse();
  }

  @Test
  // E9: save failure keeps edit mode + dirty state and reports an error message.
  void save_failure_keepsEditModeAndDirty_andSetsErrorStatus() throws Exception {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
    doThrow(new RuntimeException("disk unavailable"))
        .when(recipeRepository)
        .save(any(Recipe.class));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();
    vm.titleProperty().set("Better Pancakes");

    vm.save();
    waitForCondition(() -> !vm.isSaving());

    assertThat(vm.isEditing()).isTrue();
    assertThat(vm.isDirty()).isTrue();
    assertThat(vm.getStatusMessage()).contains("Save failed").contains("disk unavailable");
  }

  @Test
  // E10: save() is a no-op when not dirty.
  void save_whenNotDirty_isNoOp() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");

    vm.save();

    verify(recipeRepository, never()).save(any(Recipe.class));
  }

  @Test
  // E10: save() is a no-op when current edits are invalid.
  void save_whenInvalid_isNoOp() {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");
    vm.toggleEditMode();
    vm.titleProperty().set("   ");

    vm.save();

    verify(recipeRepository, never()).save(any(Recipe.class));
  }

  @Test
  void save_withoutLoadedRecipe_setsHelpfulStatusMessage() {
    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.toggleEditMode();
    vm.titleProperty().set("Preview Title");
    vm.addIngredient();

    vm.save();

    verify(recipeRepository, never()).save(any(Recipe.class));
    assertThat(vm.getStatusMessage()).isEqualTo("Open a real recipe from Library before saving.");
    assertThat(vm.isEditing()).isTrue();
  }

  @Test
  void exportToPdf_loadedRecipe_setsSuccessStatus(@TempDir Path tempDir) throws Exception {
    Recipe recipe = makeRecipe("recipe-1", "Pancakes", List.of("flour", "milk"));
    when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

    RecipeEditorViewModelImpl vm = new RecipeEditorViewModelImpl(recipeRepository);
    vm.loadRecipe("recipe-1");

    Path output = tempDir.resolve("pancakes.pdf");
    vm.exportToPdf(output);
    waitForCondition(() -> !vm.isExportingProperty().get());

    assertThat(vm.getStatusMessage()).contains("PDF exported successfully");
    assertThat(output).exists();
  }

  private static Recipe makeRecipe(String id, String title, List<String> ingredientNames) {
    List<Ingredient> ingredients =
        ingredientNames.stream()
            .map(name -> new VagueIngredient(name, null, null, null))
            .map(Ingredient.class::cast)
            .toList();

    return new Recipe(
        id,
        title,
        null,
        ingredients,
        List.of(
            new Instruction(1, "Mix ingredients", List.of()),
            new Instruction(2, "Cook on griddle", List.of())),
        List.<ConversionRule>of());
  }
}
