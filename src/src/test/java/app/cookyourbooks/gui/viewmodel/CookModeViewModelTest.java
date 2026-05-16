package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.CookingService;
import app.cookyourbooks.services.CookingSession;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway.Init")
class CookModeViewModelTest {

  @Mock private CookingService cookingService;
  @Mock private RecipeRepository recipeRepository;
  @Mock private NavigationService navigationService;

  private CookModeViewModelImpl viewModel;

  // ─── Shared test data ────────────────────────────────────────────────

  private static final MeasuredIngredient FLOUR =
      new MeasuredIngredient("Flour", new ExactQuantity(2, Unit.CUP), null, null);
  private static final MeasuredIngredient SUGAR =
      new MeasuredIngredient("Sugar", new ExactQuantity(1, Unit.CUP), null, null);
  private static final VagueIngredient SALT = new VagueIngredient("Salt", "to taste", null, null);

  private static final IngredientRef FLOUR_REF =
      new IngredientRef(FLOUR, new ExactQuantity(1, Unit.CUP));
  private static final IngredientRef SUGAR_REF =
      new IngredientRef(SUGAR, new ExactQuantity(0.5, Unit.CUP));

  private static final Instruction STEP_1 =
      new Instruction(1, "Mix flour and sugar", List.of(FLOUR_REF, SUGAR_REF));
  private static final Instruction STEP_2 = new Instruction(2, "Add salt to taste", List.of());
  private static final Instruction STEP_3 =
      new Instruction(3, "Bake at 350F for 30 minutes", List.of());

  private static final Recipe THREE_STEP_RECIPE =
      new Recipe(
          "recipe-1",
          "Test Cake",
          null,
          List.of(FLOUR, SUGAR, SALT),
          List.of(STEP_1, STEP_2, STEP_3),
          List.of());

  private static final Recipe NO_INSTRUCTIONS_RECIPE =
      new Recipe("recipe-empty", "Empty Recipe", null, List.of(FLOUR), List.of(), List.of());

  @BeforeEach
  void setUp() {
    viewModel = new CookModeViewModelImpl(cookingService, recipeRepository, navigationService);
  }

  // ─── Navigation boundary tests ───────────────────────────────────────

  @Test
  void firstStepHasNoPreviousStep() {
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.canGoPrevious()).isFalse();
    assertThat(viewModel.canGoNext()).isTrue();
  }

  @Test
  void lastStepHasNoNextStep() {
    CookingSession session = stubSessionAt(3, 3, STEP_3, true, false);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.canGoNext()).isFalse();
    assertThat(viewModel.canGoPrevious()).isTrue();
  }

  @Test
  void lastStepShowsFinishText() {
    CookingSession session = stubSessionAt(3, 3, STEP_3, true, false);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.isFinishAvailable()).isTrue();
  }

  // ─── Cook mode availability ──────────────────────────────────────────

  @Test
  void cookModeUnavailableWhenNoRecipeLoaded() {
    assertThat(viewModel.isCookModeAvailable()).isFalse();
  }

  @Test
  void cookModeUnavailableWhenLoadedRecipeHasNoInstructions() {
    when(recipeRepository.findById("recipe-empty")).thenReturn(Optional.of(NO_INSTRUCTIONS_RECIPE));

    viewModel.loadRecipe("recipe-empty");

    assertThat(viewModel.isCookModeAvailable()).isFalse();
    assertThat(viewModel.getStatusMessage()).contains("no instructions");
  }

  // ─── Ingredient display ──────────────────────────────────────────────

  @Test
  void cookModeIngredientsShowFallbackWhenNoStepSpecificIngredients() {
    // STEP_2 has no ingredient refs → should fall back to full recipe ingredients
    CookingSession session = stubSessionAt(2, 3, STEP_2, true, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.isUsingIngredientFallback()).isTrue();
    assertThat(viewModel.getCurrentStepIngredients()).hasSize(3);
  }

  @Test
  void cookModeStepsShowRelevantIngredients() {
    // STEP_1 has refs to Flour and Sugar → should show only those
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.isUsingIngredientFallback()).isFalse();
    assertThat(viewModel.getCurrentStepIngredients()).hasSize(2);
  }

  // ─── Step information ────────────────────────────────────────────────

  @Test
  void cookModeStepsShowCorrectRecipeStepInformation() {
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.getCurrentStepText()).isEqualTo("Mix flour and sugar");
    assertThat(viewModel.getCurrentStepNumber()).isEqualTo(1);
    assertThat(viewModel.getTotalSteps()).isEqualTo(3);
    assertThat(viewModel.stepIndicatorProperty().get()).isEqualTo("1 / 3");
    assertThat(viewModel.getRecipeTitle()).isEqualTo("Test Cake");
  }

  // ─── Loading ─────────────────────────────────────────────────────────

  @Test
  void loadRecipeWithValidIdInitializesSession() {
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.isCookModeAvailable()).isTrue();
    assertThat(viewModel.getRecipeId()).isEqualTo("recipe-1");
    assertThat(viewModel.getRecipeTitle()).isEqualTo("Test Cake");
    assertThat(viewModel.getStatusMessage()).isEqualTo("Cook mode ready.");
  }

  @Test
  void loadRecipeWithInvalidIdResetsCookMode() {
    when(recipeRepository.findById("bad-id")).thenReturn(Optional.empty());

    viewModel.loadRecipe("bad-id");

    assertThat(viewModel.isCookModeAvailable()).isFalse();
    assertThat(viewModel.getRecipeId()).isNull();
    assertThat(viewModel.getStatusMessage()).contains("not found");
  }

  // ─── Step navigation ─────────────────────────────────────────────────

  @Test
  void nextStepAdvancesInstruction() {
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    // Simulate session advancing to step 2
    when(session.getCurrentStep()).thenReturn(2);
    when(session.getCurrentInstruction()).thenReturn(STEP_2);
    when(session.hasPrevious()).thenReturn(true);
    when(session.hasNext()).thenReturn(true);

    viewModel.nextStep();

    verify(session).next();
    assertThat(viewModel.getCurrentStepNumber()).isEqualTo(2);
    assertThat(viewModel.getCurrentStepText()).isEqualTo("Add salt to taste");
  }

  @Test
  void previousStepGoesBackInstruction() {
    // Start at step 2
    CookingSession session = stubSessionAt(2, 3, STEP_2, true, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    // Simulate session going back to step 1 after previous() is called.
    // hasPrevious() must return true on the guard check, then false on refresh.
    when(session.hasPrevious()).thenReturn(true, false);
    when(session.getCurrentStep()).thenReturn(1);
    when(session.getCurrentInstruction()).thenReturn(STEP_1);
    when(session.hasNext()).thenReturn(true);

    viewModel.previousStep();

    verify(session).previous();
    assertThat(viewModel.getCurrentStepNumber()).isEqualTo(1);
    assertThat(viewModel.getCurrentStepText()).isEqualTo("Mix flour and sugar");
  }

  // ─── Exit and finish ─────────────────────────────────────────────────

  @Test
  void exitCookModeButtonReturnsToRecipeView() {
    CookingSession session = stubSessionAt(1, 3, STEP_1, false, true);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    viewModel.exitCookMode();

    verify(navigationService).navigateToRecipe("recipe-1");
  }

  @Test
  void finishRecipeButtonMarksRecipeAsFinishedAndReturnsToRecipeView() {
    // Navigate to the last step
    CookingSession session = stubSessionAt(3, 3, STEP_3, true, false);
    loadRecipeWithSession(THREE_STEP_RECIPE, session);

    assertThat(viewModel.isFinishAvailable()).isTrue();

    viewModel.finishCooking();

    assertThat(viewModel.isFinished()).isTrue();
    assertThat(viewModel.getStatusMessage()).isEqualTo("Recipe complete.");
    verify(navigationService).navigateToRecipe("recipe-1");
  }

  // ─── Test helpers ────────────────────────────────────────────────────

  private CookingSession stubSessionAt(
      int currentStep,
      int totalSteps,
      Instruction instruction,
      boolean hasPrevious,
      boolean hasNext) {
    CookingSession session = mock(CookingSession.class);
    when(session.getCurrentStep()).thenReturn(currentStep);
    when(session.getTotalSteps()).thenReturn(totalSteps);
    when(session.getCurrentInstruction()).thenReturn(instruction);
    when(session.hasPrevious()).thenReturn(hasPrevious);
    when(session.hasNext()).thenReturn(hasNext);
    return session;
  }

  private void loadRecipeWithSession(Recipe recipe, CookingSession session) {
    when(recipeRepository.findById(recipe.getId())).thenReturn(Optional.of(recipe));
    when(cookingService.startSession(recipe)).thenReturn(session);
    viewModel.loadRecipe(recipe.getId());
  }
}
