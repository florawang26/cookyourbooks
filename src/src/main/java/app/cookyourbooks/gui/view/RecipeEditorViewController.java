package app.cookyourbooks.gui.view;

import java.io.File;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.NavigationService.View;
import app.cookyourbooks.gui.viewmodel.EditableIngredient;
import app.cookyourbooks.gui.viewmodel.EditableInstruction;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModel;

/** Controller for RecipeEditorView.fxml. */
@SuppressWarnings("NullAway.Init") // FXML fields are injected by FXMLLoader.
public final class RecipeEditorViewController {

  @FXML private Button backToRecipesButton;
  @FXML private Button editButton;

  @FXML private Label titleLabel;
  @FXML private TextField titleField;
  @FXML private ListView<EditableInstruction> instructionsListView;
  @FXML private Button addInstructionButton;
  @FXML private ListView<EditableIngredient> ingredientsListView;
  @FXML private Button addIngredientButton;

  @FXML private Label statusLabel;
  @FXML private HBox editActionBar;
  @FXML private Button discardButton;
  @FXML private Button saveButton;
  @FXML private Button exportPdfButton;
  @FXML private Button cookModeButton;

  private final RecipeEditorViewModel viewModel;
  private final NavigationService navigationService;

  public RecipeEditorViewController(
      RecipeEditorViewModel viewModel, NavigationService navigationService) {
    this.viewModel = viewModel;
    this.navigationService = navigationService;
  }

  @SuppressWarnings("UnusedMethod") // Called reflectively by FXMLLoader.
  @FXML
  private void initialize() {
    backToRecipesButton.setOnAction(e -> navigationService.navigateTo(View.LIBRARY));
    cookModeButton.setOnAction(
        e -> {
          String selectedId = navigationService.getSelectedRecipeId();
          if (selectedId != null && !selectedId.isBlank()) {
            navigationService.navigateToCookMode(selectedId);
          }
        });
    editButton.setOnAction(
        e -> {
          if (viewModel.isEditing()) {
            viewModel.discardChanges();
            if (viewModel.isEditing()) {
              viewModel.toggleEditMode();
            }
          } else {
            viewModel.toggleEditMode();
          }
        });
    addIngredientButton.setOnAction(e -> viewModel.addIngredient());
    addInstructionButton.setOnAction(e -> viewModel.addInstructionStep());
    discardButton.setOnAction(e -> viewModel.discardChanges());
    saveButton.setOnAction(e -> viewModel.save());
    exportPdfButton.setOnAction(e -> onExportPdfClicked());

    titleField.textProperty().bindBidirectional(viewModel.titleProperty());
    titleField.editableProperty().bind(viewModel.editingProperty());
    titleField.disableProperty().bind(viewModel.isSavingProperty());
    titleField.visibleProperty().bind(viewModel.editingProperty());
    titleField.managedProperty().bind(viewModel.editingProperty());

    titleLabel.textProperty().bind(viewModel.titleProperty());
    titleLabel.visibleProperty().bind(viewModel.editingProperty().not());
    titleLabel.managedProperty().bind(viewModel.editingProperty().not());

    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    editButton
        .textProperty()
        .bind(Bindings.when(viewModel.editingProperty()).then("Cancel").otherwise("Edit"));
    editButton.disableProperty().bind(viewModel.isSavingProperty());

    editActionBar.visibleProperty().bind(viewModel.editingProperty());
    editActionBar.managedProperty().bind(viewModel.editingProperty());

    addIngredientButton.visibleProperty().bind(viewModel.editingProperty());
    addIngredientButton.managedProperty().bind(viewModel.editingProperty());
    addIngredientButton.disableProperty().bind(viewModel.isSavingProperty());

    addInstructionButton.visibleProperty().bind(viewModel.editingProperty());
    addInstructionButton.managedProperty().bind(viewModel.editingProperty());
    addInstructionButton.disableProperty().bind(viewModel.isSavingProperty());

    discardButton
        .disableProperty()
        .bind(viewModel.isSavingProperty().or(viewModel.isDirtyProperty().not()));
    saveButton
        .textProperty()
        .bind(Bindings.when(viewModel.isSavingProperty()).then("Saving...").otherwise("Save"));
    saveButton
        .disableProperty()
        .bind(
            viewModel
                .isSavingProperty()
                .or(viewModel.isValidProperty().not())
                .or(viewModel.isDirtyProperty().not()));

    exportPdfButton
        .textProperty()
        .bind(
            Bindings.when(viewModel.isExportingProperty())
                .then("Exporting...")
                .otherwise("Export to PDF"));
    exportPdfButton
        .disableProperty()
        .bind(
            viewModel
                .isExportingProperty()
                .or(viewModel.isSavingProperty())
                .or(
                    Bindings.createBooleanBinding(
                        () -> viewModel.getRecipeId() == null,
                        viewModel.titleProperty(),
                        viewModel.ingredientsProperty())));

    @SuppressWarnings("unchecked")
    ObservableList<EditableIngredient> ingredientRows =
        (ObservableList<EditableIngredient>) viewModel.ingredientsProperty();
    ingredientsListView.setItems(ingredientRows);
    ingredientsListView.disableProperty().bind(viewModel.isSavingProperty());
    ingredientsListView.setCellFactory(lv -> new IngredientCell());

    @SuppressWarnings("unchecked")
    ObservableList<EditableInstruction> instructionRows =
        (ObservableList<EditableInstruction>) viewModel.instructionsProperty();
    instructionsListView.setItems(instructionRows);
    instructionsListView.disableProperty().bind(viewModel.isSavingProperty());
    instructionsListView.setCellFactory(lv -> new InstructionCell());
    Label noStepsPlaceholder = new Label("No steps yet. Click Add Step in edit mode.");
    noStepsPlaceholder.getStyleClass().add("status-label");
    instructionsListView.setPlaceholder(noStepsPlaceholder);

    navigationService
        .selectedRecipeIdProperty()
        .addListener(
            (obs, oldId, newId) -> {
              if (newId != null && !newId.isBlank()) {
                viewModel.loadRecipe(newId);
              }
            });

    String selectedId = navigationService.getSelectedRecipeId();
    if (selectedId != null && !selectedId.isBlank()) {
      viewModel.loadRecipe(selectedId);
    }
  }

  private void onExportPdfClicked() {
    Window owner =
        exportPdfButton.getScene() == null ? null : exportPdfButton.getScene().getWindow();

    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Recipe as PDF");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().getFirst());

    String recipeTitle = viewModel.getTitle().trim();
    String defaultName = recipeTitle.isEmpty() ? "recipe" : recipeTitle;
    chooser.setInitialFileName(defaultName + ".pdf");

    File userHome = new File(System.getProperty("user.home", "."));
    if (userHome.exists() && userHome.isDirectory()) {
      chooser.setInitialDirectory(userHome);
    }

    File selected = chooser.showSaveDialog(owner);
    if (selected == null) {
      return;
    }

    String selectedPath = selected.getAbsolutePath();
    if (!selectedPath.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
      selected = new File(selectedPath + ".pdf");
    }

    viewModel.exportToPdf(selected.toPath());
  }

  private final class IngredientCell extends ListCell<EditableIngredient> {

    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final Button removeButton = new Button("-");
    private final HBox row = new HBox(8, nameField, descriptionField, removeButton);

    private @Nullable EditableIngredient boundItem;

    private IngredientCell() {
      nameField.setPromptText("Ingredient");
      descriptionField.setPromptText("Description / amount");
      nameField.getStyleClass().add("recipe-ingredient-name");
      descriptionField.getStyleClass().add("recipe-ingredient-amount");
      removeButton.getStyleClass().add("recipe-ingredient-remove");
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

      nameField.editableProperty().bind(viewModel.editingProperty());
      descriptionField.editableProperty().bind(viewModel.editingProperty());

      removeButton.visibleProperty().bind(viewModel.editingProperty());
      removeButton.managedProperty().bind(viewModel.editingProperty());
      removeButton.disableProperty().bind(viewModel.isSavingProperty());
      removeButton.setOnAction(e -> viewModel.removeIngredient(getIndex()));

      removeButton.setMinWidth(24);
      removeButton.setPrefWidth(24);
      removeButton.setMaxWidth(24);

      nameField.setMaxWidth(Double.MAX_VALUE);
      descriptionField.setMinWidth(84);
      descriptionField.setPrefWidth(102);
      descriptionField.setMaxWidth(114);
      HBox.setHgrow(nameField, javafx.scene.layout.Priority.ALWAYS);
      HBox.setHgrow(descriptionField, javafx.scene.layout.Priority.NEVER);

      row.disableProperty().bind(viewModel.isSavingProperty());
      row.visibleProperty().bind(Bindings.isNotNull(itemProperty()));
      row.setFillHeight(true);
      row.setMaxWidth(Double.MAX_VALUE);
      row.setSpacing(6);
      row.prefWidthProperty().bind(widthProperty().subtract(12));
    }

    @Override
    protected void updateItem(EditableIngredient item, boolean empty) {
      super.updateItem(item, empty);

      if (boundItem != null) {
        nameField.textProperty().unbindBidirectional(boundItem.nameProperty());
        descriptionField.textProperty().unbindBidirectional(boundItem.descriptionProperty());
      }

      if (empty || item == null) {
        boundItem = null;
        setGraphic(null);
        setText(null);
        return;
      }

      boundItem = item;
      nameField.textProperty().bindBidirectional(item.nameProperty());
      descriptionField.textProperty().bindBidirectional(item.descriptionProperty());
      setText(null);
      setGraphic(row);
    }
  }

  private final class InstructionCell extends ListCell<EditableInstruction> {

    private final Label stepLabel = new Label();
    private final TextArea stepTextArea = new TextArea();
    private final Button removeButton = new Button("-");
    private final Region spacer = new Region();
    private final HBox headerRow = new HBox(8, stepLabel, spacer, removeButton);
    private final VBox row = new VBox(6, headerRow, stepTextArea);

    private @Nullable EditableInstruction boundItem;

    private InstructionCell() {
      stepLabel.getStyleClass().add("recipe-step-header");
      stepLabel.getStyleClass().add("recipe-step-label");

      stepTextArea.setPromptText("Describe this step");
      stepTextArea.getStyleClass().add("recipe-step-input");
      stepTextArea.setWrapText(true);
      stepTextArea.editableProperty().bind(viewModel.editingProperty());
      stepTextArea.setPrefRowCount(2);
      stepTextArea.textProperty().addListener((obs, oldText, newText) -> updateTextAreaRows());
      stepTextArea.widthProperty().addListener((obs, oldWidth, newWidth) -> updateTextAreaRows());

      removeButton.getStyleClass().add("recipe-step-remove");
      removeButton.visibleProperty().bind(viewModel.editingProperty());
      removeButton.managedProperty().bind(viewModel.editingProperty());
      removeButton.disableProperty().bind(viewModel.isSavingProperty());
      removeButton.setOnAction(e -> viewModel.removeInstructionStep(getIndex()));

      removeButton.setMinWidth(24);
      removeButton.setPrefWidth(24);
      removeButton.setMaxWidth(24);

      HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
      stepTextArea.setMaxWidth(Double.MAX_VALUE);

      row.disableProperty().bind(viewModel.isSavingProperty());
      row.visibleProperty().bind(Bindings.isNotNull(itemProperty()));
      row.setFillWidth(true);
      row.setMaxWidth(Double.MAX_VALUE);
      row.prefWidthProperty().bind(widthProperty().subtract(16));

      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

      indexProperty()
          .addListener(
              (obs, oldValue, newValue) -> {
                if (boundItem != null && newValue.intValue() >= 0) {
                  stepLabel.setText("Step " + (newValue.intValue() + 1));
                }
              });
    }

    @Override
    protected void updateItem(EditableInstruction item, boolean empty) {
      super.updateItem(item, empty);

      if (boundItem != null) {
        stepTextArea.textProperty().unbindBidirectional(boundItem.textProperty());
      }

      if (empty || item == null) {
        boundItem = null;
        setGraphic(null);
        setText(null);
        return;
      }

      boundItem = item;
      stepLabel.setText("Step " + (getIndex() + 1));
      stepTextArea.textProperty().bindBidirectional(item.textProperty());
      updateTextAreaRows();
      setText(null);
      setGraphic(row);
    }

    private void updateTextAreaRows() {
      String text = stepTextArea.getText();
      if (text == null) {
        text = "";
      }

      // Approximate wrapped lines from control width to grow the TextArea naturally.
      double width = stepTextArea.getWidth();
      double usableWidth = Math.max(120.0, width - 24.0);
      int charsPerLine = Math.max(12, (int) Math.floor(usableWidth / 7.2));

      int visualLines = 0;
      String[] paragraphs = text.isEmpty() ? new String[] {""} : text.split("\\R", -1);
      for (String paragraph : paragraphs) {
        int paragraphLength = Math.max(1, paragraph.length());
        visualLines += (int) Math.ceil((double) paragraphLength / charsPerLine);
      }

      int rows = Math.max(2, Math.min(10, visualLines));
      stepTextArea.setPrefRowCount(rows);
    }
  }
}
