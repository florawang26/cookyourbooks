package app.cookyourbooks.gui.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import app.cookyourbooks.gui.viewmodel.SearchViewModel;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl.SearchResult;

/**
 * Controller for SearchView.fxml.
 *
 * <p>Binds each UI element to the corresponding ViewModel property or command. All user interaction
 * flows through the ViewModel — this class contains no business logic.
 */
@SuppressWarnings(
    "NullAway.Init") // FXML fields are injected by the FXMLLoader, not the constructor
public class SearchViewController {

  @FXML private TextField searchField;
  @FXML private ProgressIndicator searchingIndicator;
  @FXML private Label statusLabel;
  @FXML private Button clearButton;

  @FXML private TextField ingredientFilterField;
  @FXML private Button addFilterButton;
  @FXML private ListView<String> activeFiltersListView;

  @FXML private ListView<SearchResult> resultsListView;

  private final SearchViewModel viewModel;

  /**
   * Constructs a SearchViewController with the given ViewModel.
   *
   * @param viewModel the ViewModel this controller binds to
   */
  public SearchViewController(SearchViewModel viewModel) {
    this.viewModel = viewModel;
  }

  /** Called by FXMLLoader after all @FXML fields are injected. */
  @FXML
  private void initialize() {
    // ── Search field ──────────────────────────────────────────────────────
    // When the user types, tell the ViewModel (which debounces internally).
    searchField.textProperty().addListener((obs, oldVal, newVal) -> viewModel.setQuery(newVal));

    // Keyboard navigation: Up/Down move selection in results; Enter opens the recipe.
    searchField.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.DOWN) {
            viewModel.selectNextResult();
            syncListViewSelection();
            event.consume();
          } else if (event.getCode() == KeyCode.UP) {
            viewModel.selectPreviousResult();
            syncListViewSelection();
            event.consume();
          } else if (event.getCode() == KeyCode.ENTER) {
            viewModel.navigateToSelectedResult();
            event.consume();
          }
        });

    // ── Status / loading ──────────────────────────────────────────────────
    searchingIndicator.visibleProperty().bind(viewModel.searchingProperty());
    searchingIndicator.managedProperty().bind(viewModel.searchingProperty());
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // ── Clear button ──────────────────────────────────────────────────────
    clearButton.setOnAction(
        e -> {
          searchField.clear();
          viewModel.clearFilters();
        });

    // ── Results list ──────────────────────────────────────────────────────
    // Cast is safe: resultsProperty() is ObservableList<SearchResult> at runtime.
    @SuppressWarnings("unchecked")
    var typedResults =
        (javafx.collections.ObservableList<SearchResult>) viewModel.resultsProperty();
    resultsListView.setItems(typedResults);

    // Display the recipe title in each row.
    resultsListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });

    // When the user clicks a result with the mouse, keep selectedIndex in sync.
    resultsListView
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener(
            (obs, oldIdx, newIdx) -> {
              int idx = newIdx.intValue();
              if (idx >= 0) {
                viewModel.selectedResult(idx);
              }
            });

    // Double-click on a result navigates to it.
    resultsListView.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2) {
            int idx = resultsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
              // Align the ViewModel's selectedIndex before navigating.
              viewModel.selectedResult(idx);
              viewModel.navigateToSelectedResult();
            }
          }
        });

    // Pressing Enter on a selected result navigates to it.
    resultsListView.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            int idx = resultsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
              viewModel.selectedResult(idx);
              viewModel.navigateToSelectedResult();
            }
            event.consume();
          }
        });

    // ── Ingredient filter panel ───────────────────────────────────────────
    activeFiltersListView.setItems(viewModel.ingredientFiltersProperty());

    // Show each filter term with a "×" remove button.
    activeFiltersListView.setCellFactory(
        lv ->
            new ListCell<>() {
              private final Button removeBtn = new Button("×");

              {
                removeBtn.setOnAction(
                    e -> {
                      String item = getItem();
                      if (item != null) {
                        viewModel.removeIngredientFilter(item);
                      }
                    });
              }

              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  setGraphic(null);
                } else {
                  setText(item);
                  setGraphic(removeBtn);
                }
              }
            });

    addFilterButton.setOnAction(
        e -> {
          String ingredient = ingredientFilterField.getText().trim();
          if (!ingredient.isEmpty()) {
            viewModel.addIngredientFilter(ingredient);
            ingredientFilterField.clear();
          }
        });

    // Pressing Enter in the ingredient field is the same as clicking Add.
    ingredientFilterField.setOnAction(e -> addFilterButton.fire());
  }

  /** Updates the ListView's visual selection to match the ViewModel's selectedIndex. */
  private void syncListViewSelection() {
    String selectedId = viewModel.getSelectedResultId();
    if (selectedId == null) {
      resultsListView.getSelectionModel().clearSelection();
      return;
    }
    // Find the index of the selected result in the ListView and select it visually.
    var items = resultsListView.getItems();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).id().equals(selectedId)) {
        resultsListView.getSelectionModel().select(i);
        resultsListView.scrollTo(i);
        return;
      }
    }
  }
}
