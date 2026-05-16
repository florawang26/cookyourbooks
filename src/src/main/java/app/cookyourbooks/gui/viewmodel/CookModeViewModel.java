package app.cookyourbooks.gui.viewmodel;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

/**
 * ViewModel interface for Cook Mode.
 *
 * <p>Cook Mode shows one instruction step at a time with large next/previous controls and a list of
 * ingredients relevant to the current step.
 */
public interface CookModeViewModel {

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (for JavaFX binding in the View)
  // ──────────────────────────────────────────────────────────────────────────

  /** The title of the recipe currently being cooked. */
  StringProperty recipeTitleProperty();

  /** The text of the currently visible instruction step. */
  StringProperty currentStepTextProperty();

  /** Step indicator in display form, e.g. "2 / 7". */
  StringProperty stepIndicatorProperty();

  /** Current step number (1-based). */
  IntegerProperty currentStepNumberProperty();

  /** Total number of steps in the loaded recipe. */
  IntegerProperty totalStepsProperty();

  /** Ingredients shown for the current step in display-ready string form. */
  ObservableList<String> currentStepIngredientsProperty();

  /** Whether there is a previous step available. */
  BooleanProperty canGoPreviousProperty();

  /** Whether there is a next step available. */
  BooleanProperty canGoNextProperty();

  /**
   * Whether the UI should show "Finish Recipe" instead of "Next".
   *
   * <p>This is true when the user is on the last step.
   */
  BooleanProperty finishAvailableProperty();

  /** Whether cook mode can be entered for the currently loaded recipe. */
  BooleanProperty cookModeAvailableProperty();

  /** Whether the current step is showing the full recipe ingredient fallback. */
  BooleanProperty usingIngredientFallbackProperty();

  /** Whether the current step has no ingredients to show at all. */
  BooleanProperty noIngredientsForStepProperty();

  /** Whether this cooking run has been finished. */
  BooleanProperty finishedProperty();

  /** User-facing status message (errors, hints, and finish status). */
  StringProperty statusMessageProperty();

  // ──────────────────────────────────────────────────────────────────────────
  // Commands (user actions)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Loads a recipe by ID and initializes cook mode state.
   *
   * <p>If the recipe has no instructions, cook mode is marked unavailable.
   */
  void loadRecipe(String recipeId);

  /** Moves to the next step when available. */
  void nextStep();

  /** Moves to the previous step when available. */
  void previousStep();

  /** Marks cooking as finished and exits back to recipe editor view. */
  void finishCooking();

  /** Exits cook mode back to recipe editor view. */
  void exitCookMode();

  /** Clears all state in this ViewModel. */
  void reset();

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /** Returns the currently loaded recipe ID, or null if none. */
  @Nullable String getRecipeId();

  /** Returns the recipe title. */
  String getRecipeTitle();

  /** Returns the current step text. */
  String getCurrentStepText();

  /** Returns the current step number (1-based). */
  int getCurrentStepNumber();

  /** Returns total number of steps. */
  int getTotalSteps();

  /** Returns current step ingredients in display-ready form. */
  List<String> getCurrentStepIngredients();

  /** Returns whether previous step navigation is available. */
  boolean canGoPrevious();

  /** Returns whether next step navigation is available. */
  boolean canGoNext();

  /** Returns whether the Finish action should be shown/enabled. */
  boolean isFinishAvailable();

  /** Returns whether cook mode is available for the loaded recipe. */
  boolean isCookModeAvailable();

  /** Returns whether full ingredient-list fallback is being shown for current step. */
  boolean isUsingIngredientFallback();

  /** Returns whether current step has no ingredients to show. */
  boolean isNoIngredientsForStep();

  /** Returns whether the recipe has been finished. */
  boolean isFinished();

  /** Returns the current status message. */
  String getStatusMessage();
}
