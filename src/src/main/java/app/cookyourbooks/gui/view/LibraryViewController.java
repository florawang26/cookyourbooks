package app.cookyourbooks.gui.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.LibraryViewModel;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;

/**
 * Controller for the Library View ({@code LibraryView.fxml}).
 *
 * <p>Responsibilities:
 *
 * <ol>
 *   <li>Bind UI controls to the ViewModel's observable properties
 *   <li>Forward user actions (button clicks, list selections) to ViewModel commands
 *   <li>Call {@code viewModel.refresh()} when the view first appears
 * </ol>
 *
 * <p>This class does NOT create or own the ViewModel — it receives it via constructor so that the
 * ViewModel's lifecycle is managed by {@code CookYourBooksGuiApp}.
 */
@SuppressWarnings("NullAway.Init") // FXML fields are injected by FXMLLoader after construction
public class LibraryViewController {

  // ── FXML-injected controls ──────────────────────────────────────────────
  // Each fx:id in LibraryView.fxml corresponds to a field here.
  // These are null until FXMLLoader calls initialize().

  @FXML private TextField filterField;
  @FXML private Button refreshButton;
  @FXML private Button newCollectionButton;

  @FXML private ListView<LibraryViewModelImpl.CollectionSummary> collectionsListView;
  @FXML private ListView<LibraryViewModelImpl.RecipeSummary> recipesListView;
  @FXML private Button deleteCollectionButton;

  @FXML private Button viewRecipeButton;

  @FXML private ProgressIndicator loadingIndicator;

  @FXML private HBox undoBar;
  @FXML private Label undoMessageLabel;
  @FXML private Button undoButton;

  // ── ViewModel ───────────────────────────────────────────────────────────

  private final LibraryViewModel viewModel;
  private final NavigationService navigationService;

  /**
   * Constructs the controller with the given ViewModel and navigation service.
   *
   * @param viewModel the library ViewModel (created and owned by CookYourBooksGuiApp)
   * @param navigationService the shared navigation service
   */
  public LibraryViewController(LibraryViewModel viewModel, NavigationService navigationService) {
    this.viewModel = viewModel;
    this.navigationService = navigationService;
  }

  /**
   * Called by FXMLLoader after all @FXML fields have been injected.
   *
   * <p>TODO: Implement the following in order:
   *
   * <ol>
   *   <li>Bind filterField.textProperty() to viewModel.filterTextProperty() (bidirectional)
   *   <li>Bind loadingIndicator.visibleProperty() to viewModel.loadingProperty() (unidirectional)
   *   <li>Bind loadingIndicator.managedProperty() to viewModel.loadingProperty() (unidirectional)
   *   <li>Bind undoBar visibility/managed to viewModel.undoAvailableProperty()
   *   <li>Bind undoMessageLabel.textProperty() to viewModel.undoMessageProperty()
   *   <li>Set up collectionsListView selection listener → viewModel.selectCollection()
   *   <li>Set up recipesListView selection listener → viewModel.selectRecipe()
   *   <li>Wire refreshButton → viewModel.refresh()
   *   <li>Wire newCollectionButton → prompt for title → viewModel.createCollection()
   *   <li>Wire deleteCollectionButton → viewModel.deleteCollection()
   *   <li>Wire undoButton → viewModel.undoDelete()
   *   <li>Call viewModel.refresh() to load initial data
   * </ol>
   */
  @FXML
  @SuppressWarnings({"UnusedMethod", "unchecked"}) // called reflectively by FXMLLoader
  private void initialize() {
    // Bidirectional — filter field and ViewModel stay in sync
    filterField.textProperty().bindBidirectional(viewModel.filterTextProperty());

    // Unidirectional — ViewModel drives the indicator
    loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
    loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

    // Undo bar visibility
    undoBar.visibleProperty().bind(viewModel.undoAvailableProperty());
    undoBar.managedProperty().bind(viewModel.undoAvailableProperty());

    // Undo message
    undoMessageLabel.textProperty().bind(viewModel.undoMessageProperty());

    // View recipe button (alternative to double-clicking a recipe)
    viewRecipeButton.disableProperty().bind(viewModel.viewRecipeButtonEnabledProperty().not());

    // Collections list view is populated by the ViewModel's collectionsProperty
    collectionsListView.setItems(
        (ObservableList<LibraryViewModelImpl.CollectionSummary>) viewModel.collectionsProperty());

    collectionsListView.setCellFactory(
        listView ->
            new ListCell<>() {
              @Override
              protected void updateItem(
                  LibraryViewModelImpl.CollectionSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });

    // Collections list listener
    collectionsListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                viewModel.selectCollection(newVal.id());
              }
            });

    // Recipes list view is populated by the ViewModel's recipesProperty
    recipesListView.setItems(
        (ObservableList<LibraryViewModelImpl.RecipeSummary>) viewModel.recipesProperty());

    recipesListView.setCellFactory(
        listView ->
            new ListCell<>() {
              @Override
              protected void updateItem(LibraryViewModelImpl.RecipeSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });

    // Double-click a recipe to open it in the editor
    recipesListView.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2) {
            var selected = recipesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
              navigationService.navigateToRecipe(selected.id());
            }
          }
        });

    // Enter key on the recipes list opens the selected recipe in the editor
    recipesListView.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            var selected = recipesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
              navigationService.navigateToRecipe(selected.id());
            }
          }
        });

    // Recipes list listener
    recipesListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                viewModel.selectRecipe(newVal.id());
              }
            });

    // Refresh button
    refreshButton.setOnAction(e -> viewModel.refresh());

    // View recipe button
    viewRecipeButton.setOnAction(
        e -> {
          var selected = recipesListView.getSelectionModel().getSelectedItem();
          if (selected != null) {
            navigationService.navigateToRecipe(selected.id());
          }
        });

    // New collection button
    newCollectionButton.setOnAction(
        e -> {
          TextInputDialog dialog = new TextInputDialog();
          dialog.setTitle("New Collection");
          dialog.setHeaderText(null);
          dialog.setContentText("Enter collection name:");
          dialog
              .showAndWait()
              .ifPresent(
                  title -> {
                    if (!title.isBlank()) {
                      viewModel.createCollection(title);
                    }
                  });
        });

    // Delete collection button
    deleteCollectionButton.setOnAction(
        e -> {
          var selected = collectionsListView.getSelectionModel().getSelectedItem();
          if (selected != null) {
            viewModel.deleteCollection(selected.id());
          }
        });

    // Undo button
    undoButton.setOnAction(e -> viewModel.undoDelete());

    // Load initial data
    viewModel.refresh();
  }
}
