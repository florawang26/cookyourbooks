package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianService;

class SearchViewModelImplTest extends ViewModelTestBase {

  // Recipes with no ingredients
  private static final Recipe WAFFLES =
      new Recipe("r1", "Waffles", null, List.of(), List.of(), List.of());
  private static final Recipe OMELETTE =
      new Recipe("r2", "Omelette", null, List.of(), List.of(), List.of());

  // Recipe with flour ingredient (for ingredient filter tests)
  private static final Recipe PANCAKES =
      new Recipe(
          "r3",
          "Pancakes",
          null,
          List.of(new VagueIngredient("flour", null, null, null)),
          List.of(),
          List.of());

  // Recipe with both flour and eggs (for AND intersection test)
  private static final Recipe CREPES =
      new Recipe(
          "r4",
          "Crepes",
          null,
          List.of(
              new VagueIngredient("flour", null, null, null),
              new VagueIngredient("eggs", null, null, null)),
          List.of(),
          List.of());

  private LibrarianService mockService;
  private NavigationService navigationService;

  @BeforeEach
  void setUp() {
    mockService = mock(LibrarianService.class);
    navigationService = new NavigationService();

    // Default: listAllRecipes() returns all recipes unless overridden in a specific test.
    when(mockService.listAllRecipes()).thenReturn(List.of(WAFFLES, OMELETTE, PANCAKES, CREPES));
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  /** Constructs a ViewModel and waits for the initial search to settle. */
  private SearchViewModelImpl makeViewModel() throws InterruptedException {
    SearchViewModelImpl vm = new SearchViewModelImpl(mockService, navigationService);
    waitForCondition(() -> !vm.isSearching());
    return vm;
  }

  /**
   * Waits for a debounced search triggered by setQuery() to complete. setQuery() schedules a 300ms
   * timer before firing, so we sleep past the debounce window first, then wait for the background
   * search to finish.
   */
  private void waitForDebouncedSearch(SearchViewModelImpl vm) throws InterruptedException {
    Thread.sleep(400); // outlast the 300ms debounce timer
    waitForCondition(() -> !vm.isSearching());
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  // S1/S2 — query-based search delegates to resolveRecipes()
  @Test
  void nonEmptyQuery_returnsTitleMatches() throws InterruptedException {
    when(mockService.resolveRecipes("waffle")).thenReturn(List.of(WAFFLES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("waffle");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getResultIds()).containsExactly("r1");
  }

  // S3 — ingredient filter narrows results to recipes containing that ingredient
  @Test
  void ingredientFilter_narrowsResultsToMatchingRecipes() throws InterruptedException {
    when(mockService.searchByIngredient("flour")).thenReturn(List.of(PANCAKES, CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.addIngredientFilter("flour");
    waitForCondition(() -> !viewModel.isSearching());

    assertThat(viewModel.getResultIds()).containsExactlyInAnyOrder("r3", "r4");
  }

  // S3 — adding and removing a filter updates the active filter list
  @Test
  void addAndRemoveIngredientFilter_updatesFilterList() throws InterruptedException {
    when(mockService.searchByIngredient("flour")).thenReturn(List.of(PANCAKES, CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.addIngredientFilter("flour");
    waitForCondition(() -> !viewModel.isSearching());
    assertThat(viewModel.getIngredientFilters()).containsExactly("flour");

    viewModel.removeIngredientFilter("flour");
    waitForCondition(() -> !viewModel.isSearching());
    assertThat(viewModel.getIngredientFilters()).isEmpty();
  }

  // S4 — multiple ingredient filters use AND logic (intersection)
  @Test
  void multipleIngredientFilters_andLogic_returnsIntersection() throws InterruptedException {
    // Only CREPES has both flour AND eggs — PANCAKES only has flour
    when(mockService.searchByIngredient("flour")).thenReturn(List.of(PANCAKES, CREPES));
    when(mockService.searchByIngredient("eggs")).thenReturn(List.of(CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.addIngredientFilter("flour");
    waitForCondition(() -> !viewModel.isSearching());
    viewModel.addIngredientFilter("eggs");
    waitForCondition(() -> !viewModel.isSearching());

    assertThat(viewModel.getResultIds()).containsExactly("r4");
  }

  // S4 — AND logic applies across query results and ingredient filters combined
  @Test
  void nonEmptyQuery_multipleIngredientFilters_returnsIntersection() throws InterruptedException {
    when(mockService.resolveRecipes("pan")).thenReturn(List.of(PANCAKES, CREPES));
    when(mockService.searchByIngredient("eggs")).thenReturn(List.of(CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("pan");
    waitForDebouncedSearch(viewModel);

    viewModel.addIngredientFilter("eggs");
    waitForCondition(() -> !viewModel.isSearching());

    assertThat(viewModel.getResultIds()).containsExactly("r4");
  }

  // S5 — clearFilters() resets query and filters, returning all recipes
  @Test
  void clearFilters_resetsResultsToAllRecipes() throws InterruptedException {
    when(mockService.resolveRecipes("waffle")).thenReturn(List.of(WAFFLES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("waffle");
    waitForDebouncedSearch(viewModel);

    viewModel.clearFilters();
    waitForCondition(() -> !viewModel.isSearching());

    assertThat(viewModel.getResultIds()).containsExactlyInAnyOrder("r1", "r2", "r3", "r4");
  }

  // S6 — isSearching is true while the background search runs, false when done
  @Test
  void isSearching_trueWhileSearchRunning_falseWhenComplete() throws InterruptedException {
    when(mockService.resolveRecipes("waffle"))
        .thenAnswer(
            inv -> {
              Thread.sleep(500);
              return List.of(WAFFLES);
            });

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("waffle");
    Thread.sleep(400); // debounce fires, search starts
    waitForFxEvents(); // flush so isSearching().set(true) has landed
    assertThat(viewModel.isSearching()).isTrue();

    waitForCondition(() -> !viewModel.isSearching());
    assertThat(viewModel.isSearching()).isFalse();
  }

  // S7 — search does not fire until 300ms after the last keystroke
  @Test
  void debounce_searchDoesNotFireBeforeWindowExpires() throws InterruptedException {
    when(mockService.resolveRecipes("waffle")).thenReturn(List.of(WAFFLES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("waffle");

    // Immediately after setQuery, the debounce timer is running but hasn't fired yet.
    // Results must still reflect the previous state (all recipes from the initial load).
    assertThat(viewModel.getResultIds()).containsExactlyInAnyOrder("r1", "r2", "r3", "r4");

    // After the debounce window passes, the search fires and results update.
    waitForDebouncedSearch(viewModel);
    assertThat(viewModel.getResultIds()).containsExactly("r1");
  }

  // S8 — selectNextResult/selectPreviousResult cycle through results
  @Test
  void selectedResultId_reflectsSelectedIndex() throws InterruptedException {
    when(mockService.resolveRecipes("pan")).thenReturn(List.of(PANCAKES, CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("pan");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getSelectedResultId()).isNull();

    viewModel.selectNextResult();
    assertThat(viewModel.getSelectedResultId()).isEqualTo("r3");
    viewModel.selectNextResult();
    assertThat(viewModel.getSelectedResultId()).isEqualTo("r4");
    viewModel.selectPreviousResult();
    assertThat(viewModel.getSelectedResultId()).isEqualTo("r3");
  }

  // S9 — navigateToSelectedResult() pushes the selected recipe ID to NavigationService
  @Test
  void navigateToSelectedResult_callsNavigationService() throws InterruptedException {
    when(mockService.resolveRecipes("pan")).thenReturn(List.of(PANCAKES, CREPES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("pan");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getSelectedResultId()).isNull();

    viewModel.selectNextResult();
    assertThat(viewModel.getSelectedResultId()).isEqualTo("r3");

    viewModel.navigateToSelectedResult();
    assertThat(navigationService.getSelectedRecipeId()).isEqualTo("r3");
  }

  // S10 — status message reflects result count, or "No results found" when empty
  @Test
  void statusMessage_reflectsResultCount() throws InterruptedException {
    when(mockService.resolveRecipes("waffle")).thenReturn(List.of(WAFFLES));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("waffle");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getStatusMessage()).isEqualTo("1 result");
  }

  @Test
  void statusMessage_reflectsPluralResultCount() throws InterruptedException {
    when(mockService.resolveRecipes("a")).thenReturn(List.of(WAFFLES, OMELETTE));

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("a");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getStatusMessage()).isEqualTo("2 results");
  }

  @Test
  void statusMessage_noResults_showsNotFoundMessage() throws InterruptedException {
    when(mockService.resolveRecipes("xyz")).thenReturn(List.of());

    SearchViewModelImpl viewModel = makeViewModel();
    viewModel.setQuery("xyz");
    waitForDebouncedSearch(viewModel);

    assertThat(viewModel.getStatusMessage()).isEqualTo("No results found");
  }

  // S11 — empty query with no filters returns all recipes
  @Test
  void emptyQueryNoFilters_returnsAllRecipes() throws InterruptedException {
    SearchViewModelImpl viewModel = makeViewModel();

    assertThat(viewModel.getResultIds()).containsExactlyInAnyOrder("r1", "r2", "r3", "r4");
  }
}
