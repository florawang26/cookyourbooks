package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.CookingService;
import app.cookyourbooks.services.CookingSession;

/** Implementation of {@link CookModeViewModel}. */
public final class CookModeViewModelImpl implements CookModeViewModel {

  private final CookingService cookingService;
  private final RecipeRepository recipeRepository;
  private final NavigationService navigationService;

  private final StringProperty recipeTitle = new SimpleStringProperty("");
  private final StringProperty currentStepText = new SimpleStringProperty("");
  private final StringProperty stepIndicator = new SimpleStringProperty("0 / 0");
  private final IntegerProperty currentStepNumber = new SimpleIntegerProperty(0);
  private final IntegerProperty totalSteps = new SimpleIntegerProperty(0);
  private final ObservableList<String> currentStepIngredients = FXCollections.observableArrayList();

  private final BooleanProperty canGoPrevious = new SimpleBooleanProperty(false);
  private final BooleanProperty canGoNext = new SimpleBooleanProperty(false);
  private final BooleanProperty finishAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty cookModeAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty usingIngredientFallback = new SimpleBooleanProperty(false);
  private final BooleanProperty noIngredientsForStep = new SimpleBooleanProperty(false);
  private final BooleanProperty finished = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  private @Nullable String recipeId;
  private @Nullable Recipe loadedRecipe;
  private @Nullable CookingSession session;

  /**
   * Creates a Cook Mode ViewModel.
   *
   * @param cookingService service used to create step-by-step sessions
   * @param recipeRepository repository used to load recipe details by ID
   * @param navigationService shared navigation service used to return to recipe editor
   */
  public CookModeViewModelImpl(
      CookingService cookingService,
      RecipeRepository recipeRepository,
      NavigationService navigationService) {
    this.cookingService = Objects.requireNonNull(cookingService);
    this.recipeRepository = Objects.requireNonNull(recipeRepository);
    this.navigationService = Objects.requireNonNull(navigationService);
  }

  @Override
  public StringProperty recipeTitleProperty() {
    return recipeTitle;
  }

  @Override
  public StringProperty currentStepTextProperty() {
    return currentStepText;
  }

  @Override
  public StringProperty stepIndicatorProperty() {
    return stepIndicator;
  }

  @Override
  public IntegerProperty currentStepNumberProperty() {
    return currentStepNumber;
  }

  @Override
  public IntegerProperty totalStepsProperty() {
    return totalSteps;
  }

  @Override
  public ObservableList<String> currentStepIngredientsProperty() {
    return currentStepIngredients;
  }

  @Override
  public BooleanProperty canGoPreviousProperty() {
    return canGoPrevious;
  }

  @Override
  public BooleanProperty canGoNextProperty() {
    return canGoNext;
  }

  @Override
  public BooleanProperty finishAvailableProperty() {
    return finishAvailable;
  }

  @Override
  public BooleanProperty cookModeAvailableProperty() {
    return cookModeAvailable;
  }

  @Override
  public BooleanProperty usingIngredientFallbackProperty() {
    return usingIngredientFallback;
  }

  @Override
  public BooleanProperty noIngredientsForStepProperty() {
    return noIngredientsForStep;
  }

  @Override
  public BooleanProperty finishedProperty() {
    return finished;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public void loadRecipe(String requestedRecipeId) {
    recipeRepository
        .findById(requestedRecipeId)
        .ifPresentOrElse(this::initializeSession, () -> clearForMissingRecipe(requestedRecipeId));
  }

  @Override
  public void nextStep() {
    if (session == null || !cookModeAvailable.get()) {
      return;
    }
    if (finishAvailable.get()) {
      return;
    }
    if (session.hasNext()) {
      session.next();
      refreshFromSession();
    }
  }

  @Override
  public void previousStep() {
    if (session == null || !cookModeAvailable.get()) {
      return;
    }
    if (session.hasPrevious()) {
      session.previous();
      refreshFromSession();
    }
  }

  @Override
  public void finishCooking() {
    if (!finishAvailable.get()) {
      return;
    }
    finished.set(true);
    statusMessage.set("Recipe complete.");
    exitCookMode();
  }

  @Override
  public void exitCookMode() {
    if (recipeId != null) {
      navigationService.navigateToRecipe(recipeId);
      return;
    }
    navigationService.navigateTo(NavigationService.View.RECIPE_EDITOR);
  }

  @Override
  public void reset() {
    clearSessionReferences();
    recipeTitle.set("");
    clearStepDisplayState();
    clearNavigationState();
    cookModeAvailable.set(false);
    setIngredientFlags(false, false);
    finished.set(false);
    statusMessage.set("");
  }

  @Override
  public @Nullable String getRecipeId() {
    return recipeId;
  }

  @Override
  public String getRecipeTitle() {
    return recipeTitle.get();
  }

  @Override
  public String getCurrentStepText() {
    return currentStepText.get();
  }

  @Override
  public int getCurrentStepNumber() {
    return currentStepNumber.get();
  }

  @Override
  public int getTotalSteps() {
    return totalSteps.get();
  }

  @Override
  public List<String> getCurrentStepIngredients() {
    return List.copyOf(currentStepIngredients);
  }

  @Override
  public boolean canGoPrevious() {
    return canGoPrevious.get();
  }

  @Override
  public boolean canGoNext() {
    return canGoNext.get();
  }

  @Override
  public boolean isFinishAvailable() {
    return finishAvailable.get();
  }

  @Override
  public boolean isCookModeAvailable() {
    return cookModeAvailable.get();
  }

  @Override
  public boolean isUsingIngredientFallback() {
    return usingIngredientFallback.get();
  }

  @Override
  public boolean isNoIngredientsForStep() {
    return noIngredientsForStep.get();
  }

  @Override
  public boolean isFinished() {
    return finished.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  private void initializeSession(Recipe recipe) {
    recipeId = recipe.getId();
    loadedRecipe = recipe;
    recipeTitle.set(recipe.getTitle());
    finished.set(false);

    if (recipe.getInstructions().isEmpty()) {
      applyNoInstructionState();
      return;
    }

    cookModeAvailable.set(true);
    session = cookingService.startSession(recipe);
    statusMessage.set("Cook mode ready.");
    refreshFromSession();
  }

  private void clearForMissingRecipe(String missingRecipeId) {
    reset();
    statusMessage.set("Recipe not found: " + missingRecipeId);
  }

  private void clearSessionReferences() {
    recipeId = null;
    loadedRecipe = null;
    session = null;
  }

  private void clearStepDisplayState() {
    currentStepText.set("");
    stepIndicator.set("0 / 0");
    currentStepNumber.set(0);
    totalSteps.set(0);
    currentStepIngredients.clear();
  }

  private void clearNavigationState() {
    canGoPrevious.set(false);
    canGoNext.set(false);
    finishAvailable.set(false);
  }

  private void setIngredientFlags(boolean fallback, boolean noIngredients) {
    usingIngredientFallback.set(fallback);
    noIngredientsForStep.set(noIngredients);
  }

  private void applyNoInstructionState() {
    session = null;
    cookModeAvailable.set(false);
    clearStepDisplayState();
    clearNavigationState();
    setIngredientFlags(false, true);
    statusMessage.set("Cook mode unavailable: recipe has no instructions.");
  }

  private void refreshFromSession() {
    if (session == null || loadedRecipe == null) {
      return;
    }

    Instruction instruction = session.getCurrentInstruction();
    int current = session.getCurrentStep();
    int total = session.getTotalSteps();

    currentStepText.set(instruction.getText());
    currentStepNumber.set(current);
    totalSteps.set(total);
    stepIndicator.set(current + " / " + total);

    canGoPrevious.set(session.hasPrevious());
    canGoNext.set(session.hasNext());
    finishAvailable.set(!session.hasNext());

    updateCurrentStepIngredients(loadedRecipe, instruction);
  }

  private void updateCurrentStepIngredients(Recipe recipe, Instruction instruction) {
    List<IngredientRef> refs = instruction.getIngredientRefs();
    if (refs.isEmpty()) {
      // No structured refs for this instruction: show all recipe ingredients as fallback.
      List<String> fallback = recipe.getIngredients().stream().map(Ingredient::toString).toList();
      currentStepIngredients.setAll(fallback);
      usingIngredientFallback.set(true);
      noIngredientsForStep.set(fallback.isEmpty());
      return;
    }

    Set<String> usedNamesLower =
        refs.stream()
            .map(ref -> ref.ingredient().getName().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());

    List<String> orderedUsed =
        recipe.getIngredients().stream()
            .filter(
                ingredient ->
                    usedNamesLower.contains(ingredient.getName().toLowerCase(Locale.ROOT)))
            .map(ingredient -> displayForIngredient(ingredient, refs))
            .toList();

    currentStepIngredients.setAll(orderedUsed);
    usingIngredientFallback.set(false);
    noIngredientsForStep.set(orderedUsed.isEmpty());
  }

  private String displayForIngredient(Ingredient ingredient, List<IngredientRef> refs) {
    String targetName = ingredient.getName();
    for (IngredientRef ref : refs) {
      if (ref.ingredient().getName().equalsIgnoreCase(targetName)) {
        if (ref.quantity() != null) {
          return ref.quantity().toString() + " " + ref.ingredient().getName();
        }
        return ref.ingredient().toString();
      }
    }
    return ingredient.toString();
  }
}
