package app.cookyourbooks.gui.view;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.CookModeViewModel;

/** Controller for CookModeView.fxml. */
@SuppressWarnings("NullAway.Init") // FXML fields are injected by FXMLLoader.
public final class CookModeViewController {

  @FXML private Label recipeTitleLabel;
  @FXML private Label stepLabel;
  @FXML private Label instructionTextLabel;
  @FXML private TextArea notesTextArea;
  @FXML private ListView<String> ingredientsListView;
  @FXML private Button previousStepButton;
  @FXML private Button nextStepButton;
  @FXML private Button exitCookModeButton;

  private final CookModeViewModel viewModel;
  private final NavigationService navigationService;

  public CookModeViewController(CookModeViewModel viewModel, NavigationService navigationService) {
    this.viewModel = viewModel;
    this.navigationService = navigationService;
  }

  @SuppressWarnings("UnusedMethod") // Called reflectively by FXMLLoader.
  @FXML
  private void initialize() {
    // Text bindings
    recipeTitleLabel.textProperty().bind(viewModel.recipeTitleProperty());
    stepLabel.textProperty().bind(viewModel.stepIndicatorProperty());
    instructionTextLabel.textProperty().bind(viewModel.currentStepTextProperty());

    // Ingredients list
    ingredientsListView.setItems(viewModel.currentStepIngredientsProperty());

    // Button actions
    previousStepButton.setOnAction(e -> viewModel.previousStep());
    nextStepButton.setOnAction(
        e -> {
          if (viewModel.finishAvailableProperty().get()) {
            viewModel.finishCooking();
          } else {
            viewModel.nextStep();
          }
        });
    exitCookModeButton.setOnAction(e -> viewModel.exitCookMode());

    // Button disable bindings
    previousStepButton.disableProperty().bind(viewModel.canGoPreviousProperty().not());
    nextStepButton
        .disableProperty()
        .bind(viewModel.canGoNextProperty().not().and(viewModel.finishAvailableProperty().not()));

    // Next button text changes to "Finish Recipe" on last step
    nextStepButton
        .textProperty()
        .bind(
            Bindings.when(viewModel.finishAvailableProperty())
                .then("Finish Recipe")
                .otherwise("Next Step"));

    // Load recipe when navigating to cook mode
    navigationService
        .currentViewProperty()
        .addListener(
            (obs, oldView, newView) -> {
              if (newView == NavigationService.View.COOK_MODE) {
                String recipeId = navigationService.getSelectedRecipeId();
                if (recipeId != null && !recipeId.isBlank()) {
                  viewModel.loadRecipe(recipeId);
                }
              }
            });
  }
}
