package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.services.LibrarianService;

/** Implementation of {@link LibraryViewModel}. */
public class LibraryViewModelImpl implements LibraryViewModel {

  // ── Nested summary records ───────────────────────────────────────────────

  /** Lightweight summary of a collection for display in the View. */
  public record CollectionSummary(
      String id, String title, SourceType sourceType, int recipeCount) {}

  /** Lightweight summary of a recipe for display in the View. */
  public record RecipeSummary(String id, String title) {}

  // ── Dependencies ─────────────────────────────────────────────────────────

  private final LibrarianService librarianService;

  // ── Observable state ─────────────────────────────────────────────────────

  private final ObservableList<CollectionSummary> allCollections =
      FXCollections.observableArrayList();
  private final FilteredList<CollectionSummary> filteredCollections =
      new FilteredList<>(allCollections);

  private final ObservableList<RecipeSummary> recipes = FXCollections.observableArrayList();

  private final BooleanProperty loading = new SimpleBooleanProperty(false);
  private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private final StringProperty undoMessage = new SimpleStringProperty("");
  private final StringProperty filterText = new SimpleStringProperty("");
  private final StringProperty selectedRecipeId = new SimpleStringProperty("");

  // ── Undo state ───────────────────────────────────────────────────────────

  @Nullable private CollectionSummary pendingDelete = null;
  @Nullable private ScheduledFuture<?> undoTimer = null;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  // ── Selection state ──────────────────────────────────────────────────────

  @Nullable private String selectedCollectionId = null;

  // ── Constructor ──────────────────────────────────────────────────────────

  /**
   * Creates a new LibraryViewModelImpl.
   *
   * @param librarianService the service used to load and manage collections
   * @param navigationService the shared navigation service used to refresh on view entry
   */
  public LibraryViewModelImpl(
      LibrarianService librarianService, NavigationService navigationService) {
    this.librarianService = librarianService;

    // Re-fetch collections whenever the user navigates to the Library view so that
    // changes made in other views (e.g. recipe title edits) are reflected.
    navigationService
        .currentViewProperty()
        .addListener(
            (obs, oldView, newView) -> {
              if (newView == NavigationService.View.LIBRARY) {
                refresh();
              }
            });

    // Wire the filter: whenever filterText changes, update the predicate on filteredCollections
    filterText.addListener(
        (obs, oldVal, newVal) ->
            filteredCollections.setPredicate(
                c ->
                    newVal == null
                        || newVal.isBlank()
                        || c.title()
                            .toLowerCase(Locale.ROOT)
                            .contains(newVal.toLowerCase(Locale.ROOT))));
  }

  // ── Observable property accessors ────────────────────────────────────────

  @Override
  public ObservableList<CollectionSummary> collectionsProperty() {
    return filteredCollections;
  }

  @Override
  public StringProperty filterTextProperty() {
    return filterText;
  }

  @Override
  public ObservableList<RecipeSummary> recipesProperty() {
    return recipes;
  }

  @Override
  public BooleanProperty loadingProperty() {
    return loading;
  }

  @Override
  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  @Override
  public StringProperty undoMessageProperty() {
    return undoMessage;
  }

  @Override
  public StringProperty selectedRecipeIdProperty() {
    return selectedRecipeId;
  }

  @Override
  public BooleanBinding viewRecipeButtonEnabledProperty() {
    return selectedRecipeId.isNotEmpty();
  }

  // ── Commands ─────────────────────────────────────────────────────────────

  @Override
  public void refresh() {
    loading.set(true);
    BackgroundTaskRunner.run(
        () -> librarianService.listCollections(),
        collections -> {
          allCollections.setAll(
              collections.stream()
                  .map(
                      c ->
                          new CollectionSummary(
                              c.getId(), c.getTitle(), c.getSourceType(), c.getRecipes().size()))
                  .toList());
          loading.set(false);
          // Re-select the current collection so the recipes list reflects any updates.
          if (selectedCollectionId != null) {
            selectCollection(selectedCollectionId);
          }
        },
        error -> loading.set(false));
  }

  @Override
  public void selectCollection(String collectionId) {
    librarianService
        .findCollectionById(collectionId)
        .ifPresentOrElse(
            collection -> {
              selectedCollectionId = collectionId;
              selectedRecipeId.set("");
              recipes.setAll(
                  collection.getRecipes().stream()
                      .map(r -> new RecipeSummary(r.getId(), r.getTitle()))
                      .toList());
            },
            () -> {
              selectedCollectionId = null;
              selectedRecipeId.set("");
              recipes.clear();
            });
  }

  @Override
  public void createCollection(String title) {
    if (title == null || title.isBlank()) {
      return;
    }
    var collection = librarianService.createCollection(title);
    allCollections.add(
        new CollectionSummary(
            collection.getId(),
            collection.getTitle(),
            collection.getSourceType(),
            collection.getRecipes().size()));
  }

  @Override
  public void deleteCollection(String collectionId) {
    allCollections.stream()
        .filter(c -> c.id().equals(collectionId))
        .findFirst()
        .ifPresent(
            summary -> {
              // Cancel any existing undo timer — previous delete becomes permanent immediately
              if (undoTimer != null) {
                undoTimer.cancel(false);
                if (pendingDelete != null) {
                  librarianService.deleteCollection(pendingDelete.id());
                }
              }

              allCollections.remove(summary);
              pendingDelete = summary;
              undoAvailable.set(true);
              undoMessage.set("Deleted: " + summary.title());

              // Schedule permanent deletion after 5 seconds
              undoTimer =
                  scheduler.schedule(
                      () -> {
                        librarianService.deleteCollection(collectionId);
                        Platform.runLater(
                            () -> {
                              pendingDelete = null;
                              undoAvailable.set(false);
                              undoMessage.set("");
                            });
                      },
                      5,
                      TimeUnit.SECONDS);
            });
  }

  @Override
  public void undoDelete() {
    if (pendingDelete == null || !undoAvailable.get()) {
      return;
    }
    if (undoTimer != null) {
      undoTimer.cancel(false);
      undoTimer = null;
    }
    allCollections.add(pendingDelete);
    pendingDelete = null;
    undoAvailable.set(false);
    undoMessage.set("");
  }

  @Override
  public void selectRecipe(String recipeId) {
    selectedRecipeId.set(recipeId);
  }

  // ── Non-JavaFX accessors (grading contract) ───────────────────────────────

  @Override
  public List<String> getCollectionIds() {
    return filteredCollections.stream().map(CollectionSummary::id).toList();
  }

  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId;
  }

  @Override
  public List<String> getRecipeIds() {
    return recipes.stream().map(RecipeSummary::id).toList();
  }

  @Override
  public boolean isLoading() {
    return loading.get();
  }

  @Override
  public boolean isUndoAvailable() {
    return undoAvailable.get();
  }

  @Override
  public String getFilterText() {
    return filterText.get();
  }

  @Override
  public @Nullable String getSelectedRecipeId() {
    return selectedRecipeId.get();
  }
}
