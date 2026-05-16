package app.cookyourbooks.gui.view;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import app.cookyourbooks.gui.viewmodel.ImportViewModel;
import app.cookyourbooks.gui.viewmodel.ImportViewModel.CollectionSummary;

@SuppressWarnings("NullAway.Init")
public class ImportViewController {

  @FXML private VBox idlePane;
  @FXML private VBox processingPane;
  @FXML private VBox reviewPane;
  @FXML private VBox errorPane;

  @FXML private Button selectFileButton;
  @FXML private Label statusLabel;
  @FXML private ProgressBar progressBar;
  @FXML private Button cancelButton;
  @FXML private TextField titleField;
  @FXML private ListView<String> ingredientsList;
  @FXML private ComboBox<CollectionSummary> collectionsDropdown;
  @FXML private Button acceptButton;
  @FXML private Button rejectButton;
  @FXML private Label errorLabel;
  @FXML private Button backButton;

  private final ImportViewModel viewModel;

  public ImportViewController(ImportViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    // bindings go here
    // show idle pane only when state is "idle"
    idlePane.visibleProperty().bind(viewModel.stateProperty().isEqualTo("idle"));
    idlePane.managedProperty().bind(viewModel.stateProperty().isEqualTo("idle"));
    // processingPane, reviewPane, and errorPane?
    // Just change "idle" to the right state for each one.
    processingPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo("processing"));
    processingPane.managedProperty().bind(viewModel.stateProperty().isEqualTo("processing"));

    reviewPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo("review"));
    reviewPane.managedProperty().bind(viewModel.stateProperty().isEqualTo("review"));

    errorPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo("error"));
    errorPane.managedProperty().bind(viewModel.stateProperty().isEqualTo("error"));

    // bind status label to status message
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // bind error label to error message
    errorLabel.textProperty().bind(viewModel.errorMessageProperty());

    // bind title field to imported title (two-way binding)
    titleField.textProperty().bindBidirectional(viewModel.importedTitleProperty());

    // bind ingredients list
    ingredientsList.setItems(viewModel.importedIngredientsProperty());

    // bind collections dropdown — show title, pass ID to ViewModel
    collectionsDropdown.setItems(viewModel.availableCollectionsProperty());
    collectionsDropdown.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(CollectionSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });
    collectionsDropdown.setButtonCell(
        new ListCell<>() {
          @Override
          protected void updateItem(CollectionSummary item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.title());
          }
        });
    collectionsDropdown.setOnAction(
        e -> {
          CollectionSummary selected = collectionsDropdown.getValue();
          if (selected != null) {
            viewModel.selectTargetCollection(selected.id());
          }
        });
    // open a file chooser and start the OCR import
    selectFileButton.setOnAction(
        e -> {
          FileChooser fileChooser = new FileChooser();
          fileChooser.setTitle("Select Recipe Image");
          fileChooser
              .getExtensionFilters()
              .add(
                  new FileChooser.ExtensionFilter(
                      "Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
          File file = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
          if (file != null) {
            viewModel.loadCollections();
            viewModel.startImport(file.toPath());
          }
        });

    // wire up buttons
    cancelButton.setOnAction(e -> viewModel.cancelImport());
    rejectButton.setOnAction(e -> viewModel.rejectImport());
    acceptButton.setOnAction(e -> viewModel.acceptImport());
    backButton.setOnAction(e -> viewModel.cancelImport());
  }
}
