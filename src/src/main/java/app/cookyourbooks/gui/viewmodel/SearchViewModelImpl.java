package app.cookyourbooks.gui.viewmodel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;

/** Implementation of {@link SearchViewModel} for the Search &amp; Filter feature. */
public final class SearchViewModelImpl implements SearchViewModel {

  /**
   * A search result entry. Holds the recipe ID (for navigation) and title (for display).
   *
   * <p>We use a record because it is immutable and gives us equals/hashCode for free.
   */
  public record SearchResult(String id, String title) {}

  private final LibrarianService librarianService;
  private final NavigationService navigationService;

  private final SimpleStringProperty query = new SimpleStringProperty("");
  private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
  private final ObservableList<String> ingredientFilters = FXCollections.observableArrayList();
  private final SimpleBooleanProperty searching = new SimpleBooleanProperty(false);
  private final SimpleStringProperty statusMessage = new SimpleStringProperty("");

  // -1 means nothing is selected
  private int selectedIndex = -1;

  // The currently-running background search task. Cancelled before starting a new one.
  private @Nullable Task<?> currentTask;

  // Single-thread executor used only for scheduling the 300ms debounce timer.
  private final ScheduledExecutorService debounceScheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
          });

  // Handle to the pending debounce timer so we can cancel it when the user types again.
  private @Nullable ScheduledFuture<?> pendingDebounce;

  /**
   * Constructs a SearchViewModelImpl.
   *
   * @param librarianService the service used for all recipe search operations
   * @param navigationService the shared navigation state used to open a recipe in the editor
   */
  public SearchViewModelImpl(
      LibrarianService librarianService, NavigationService navigationService) {
    this.librarianService = librarianService;
    this.navigationService = navigationService;

    // Re-run search whenever the user navigates to the Search view so that
    // changes made in other views (e.g. recipe title edits) are reflected.
    navigationService
        .currentViewProperty()
        .addListener(
            (obs, oldView, newView) -> {
              if (newView == NavigationService.View.SEARCH) {
                runSearch();
              }
            });

    // Load all recipes on startup so the results list is not empty when the view opens.
    runSearch();
  }

  // ── Observable properties ──────────────────────────────────────────────────

  @Override
  public StringProperty queryProperty() {
    return query;
  }

  @Override
  public ObservableList<SearchResult> resultsProperty() {
    return results;
  }

  @Override
  public ObservableList<String> ingredientFiltersProperty() {
    return ingredientFilters;
  }

  @Override
  public BooleanProperty searchingProperty() {
    return searching;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  // ── Commands ───────────────────────────────────────────────────────────────

  @Override
  public void setQuery(String q) {
    query.set(q);
    // Cancel any pending debounce timer and schedule a fresh one.
    if (pendingDebounce != null) {
      pendingDebounce.cancel(false);
    }
    pendingDebounce =
        debounceScheduler.schedule(
            () -> Platform.runLater(this::runSearch), 300, TimeUnit.MILLISECONDS);
  }

  @Override
  public void addIngredientFilter(String ingredient) {
    if (!ingredient.isBlank() && !ingredientFilters.contains(ingredient)) {
      ingredientFilters.add(ingredient);
      runSearch();
    }
  }

  @Override
  public void removeIngredientFilter(String ingredient) {
    ingredientFilters.remove(ingredient);
    runSearch();
  }

  @Override
  public void clearFilters() {
    if (pendingDebounce != null) {
      pendingDebounce.cancel(false);
    }
    query.set("");
    ingredientFilters.clear();
    runSearch();
  }

  @Override
  public void selectNextResult() {
    if (results.isEmpty()) {
      return;
    }
    selectedIndex = (selectedIndex + 1) % results.size();
  }

  @Override
  public void selectPreviousResult() {
    if (results.isEmpty()) {
      return;
    }
    selectedIndex = (selectedIndex - 1 + results.size()) % results.size();
  }

  @Override
  public void navigateToSelectedResult() {
    if (selectedIndex >= 0 && selectedIndex < results.size()) {
      navigationService.navigateToRecipe(results.get(selectedIndex).id());
    }
  }

  @Override
  public void selectedResult(int index) {
    // Sets selectedIndex directly so navigateToSelectedResult() uses the correct result.
    selectedIndex = index;
  }

  // ── Non-JavaFX accessors (for grading tests) ──────────────────────────────

  @Override
  public String getQuery() {
    return query.get();
  }

  @Override
  public List<String> getResultIds() {
    return results.stream().map(SearchResult::id).toList();
  }

  @Override
  public List<String> getIngredientFilters() {
    return List.copyOf(ingredientFilters);
  }

  @Override
  public boolean isSearching() {
    return searching.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public @Nullable String getSelectedResultId() {
    if (selectedIndex >= 0 && selectedIndex < results.size()) {
      return results.get(selectedIndex).id();
    }
    return null;
  }

  // ── Search logic ───────────────────────────────────────────────────────────

  /**
   * Executes the search on a background thread. Cancels any in-flight search first to handle race
   * conditions — since BackgroundTaskRunner does not fire callbacks on cancel, the stale result
   * never overwrites the new one.
   */
  private void runSearch() {
    // Cancel the previous task so its onSuccess never fires after ours.
    if (currentTask != null) {
      currentTask.cancel();
    }

    String currentQuery = query.get().trim();
    List<String> activeFilters = List.copyOf(ingredientFilters);

    searching.set(true);
    statusMessage.set("Searching...");
    selectedIndex = -1;

    currentTask =
        BackgroundTaskRunner.run(
            () -> computeResults(currentQuery, activeFilters),
            matchedRecipes -> {
              results.setAll(
                  matchedRecipes.stream()
                      .map(r -> new SearchResult(r.getId(), r.getTitle()))
                      .toList());
              searching.set(false);
              statusMessage.set(
                  matchedRecipes.isEmpty()
                      ? "No results found"
                      : matchedRecipes.size()
                          + " result"
                          + (matchedRecipes.size() == 1 ? "" : "s"));
            },
            error -> {
              searching.set(false);
              statusMessage.set("Search failed: " + error.getMessage());
            });
  }

  /**
   * Computes the result list based on the current query and ingredient filters. Runs on the
   * background thread.
   *
   * <p>Four cases:
   *
   * <ul>
   *   <li>Empty query, no filters → all recipes
   *   <li>Empty query, filters active → searchByIngredient() AND-intersected
   *   <li>Non-empty query, no filters → resolveRecipes()
   *   <li>Non-empty query, filters active → resolveRecipes() AND-intersected with each
   *       searchByIngredient()
   * </ul>
   */
  private List<Recipe> computeResults(String currentQuery, List<String> activeFilters) {
    boolean hasQuery = !currentQuery.isEmpty();
    boolean hasFilters = !activeFilters.isEmpty();

    List<Recipe> base;
    if (!hasQuery && !hasFilters) {
      return librarianService.listAllRecipes();
    } else if (!hasQuery) {
      // Start with all recipes then intersect with each ingredient filter.
      base = librarianService.listAllRecipes();
    } else {
      base = librarianService.resolveRecipes(currentQuery);
    }

    if (!hasFilters) {
      return base;
    }

    // AND intersection: keep only recipes whose ID survives retainAll for every filter.
    Set<String> survivingIds = new HashSet<>();
    for (Recipe r : base) {
      survivingIds.add(r.getId());
    }
    for (String filter : activeFilters) {
      Set<String> filterIds = new HashSet<>();
      for (Recipe r : librarianService.searchByIngredient(filter)) {
        filterIds.add(r.getId());
      }
      survivingIds.retainAll(filterIds);
    }

    return base.stream().filter(r -> survivingIds.contains(r.getId())).toList();
  }
}
