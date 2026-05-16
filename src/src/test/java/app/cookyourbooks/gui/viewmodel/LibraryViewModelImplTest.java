package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway.Init")
class LibraryViewModelImplTest extends ViewModelTestBase {

  // Mock dependencies
  @Mock private LibrarianService librarianService;
  @Mock private app.cookyourbooks.gui.NavigationService navigationService;

  // Instance field — persists across setUp() and test methods
  private LibraryViewModelImpl viewModel;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    when(navigationService.currentViewProperty())
        .thenReturn(new javafx.beans.property.SimpleObjectProperty<>());
    viewModel = new LibraryViewModelImpl(librarianService, navigationService);
  }

  // REFRESH
  @Test
  void loadingStateDuringRefesh() throws InterruptedException {
    assertFalse(viewModel.loadingProperty().get(), "Initial loading state should be false");

    when(librarianService.listCollections())
        .thenAnswer(
            ignored -> {
              Thread.sleep(250);
              return List.of();
            });

    // Trigger refresh and check loading state
    viewModel.refresh();
    assertTrue(viewModel.loadingProperty().get(), "Loading state should be true during refresh");

    awaitCondition(() -> !viewModel.isLoading());
    assertFalse(viewModel.loadingProperty().get(), "Loading state should be false after refresh");
  }

  @Test
  void refreshLoadsCollections() throws InterruptedException {
    // Given: Mock service returns two collections
    RecipeCollection collection1 =
        makeCollection(
            "coll-1",
            "Family Recipes",
            List.of(makeRecipe("r1", "Pizza"), makeRecipe("r2", "Pasta")));
    RecipeCollection collection2 =
        makeCollection("coll-2", "Desserts", List.of(makeRecipe("r3", "Brownies")));

    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    // When: refresh is called
    viewModel.refresh();

    // Then: wait for async load to complete
    awaitCondition(() -> !viewModel.isLoading());

    // Then: verify collections are loaded with correct data
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-1", "coll-2");
    assertThat(viewModel.collectionsProperty()).hasSize(2);

    // Verify first collection summary
    var collection = viewModel.collectionsProperty().get(0);
    assertThat(collection).isInstanceOf(LibraryViewModelImpl.CollectionSummary.class);
    LibraryViewModelImpl.CollectionSummary summary =
        (LibraryViewModelImpl.CollectionSummary) collection;
    assertThat(summary.id()).isEqualTo("coll-1");
    assertThat(summary.title()).isEqualTo("Family Recipes");
    assertThat(summary.recipeCount()).isEqualTo(2);

    // Verify loading state is cleared
    assertFalse(viewModel.isLoading());
  }

  @Test
  void refreshExposesCollectionSummarySourceTypeAndRecipeCount() throws InterruptedException {
    RecipeCollection collection =
        makeCollection(
            "coll-1",
            "Family Recipes",
            List.of(makeRecipe("r1", "Pizza"), makeRecipe("r2", "Pasta")));
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    var summary = viewModel.collectionsProperty().get(0);
    assertThat(summary.sourceType()).isEqualTo(SourceType.PERSONAL);
    assertThat(summary.recipeCount()).isEqualTo(2);
  }

  // SELECT COLLECTION
  @Test
  void selectCollectionUpdatesSelectedId() throws InterruptedException {
    // Given: Mock service returns one collection
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.findCollectionById("coll-1")).thenReturn(Optional.of(collection));

    // When: selectCollection is called
    viewModel.selectCollection("coll-1");

    // Then: selected collection ID should be updated
    assertThat(viewModel.getSelectedCollectionId()).isEqualTo("coll-1");
  }

  @Test
  void selectCollectionPopulatesRecipeListAndSelectRecipeIsSafe() throws InterruptedException {
    RecipeCollection collection =
        makeCollection(
            "coll-1",
            "Family Recipes",
            List.of(makeRecipe("r1", "Pizza"), makeRecipe("r2", "Pasta")));
    when(librarianService.findCollectionById("coll-1")).thenReturn(Optional.of(collection));

    viewModel.selectCollection("coll-1");

    assertThat(viewModel.getRecipeIds()).containsExactly("r1", "r2");

    viewModel.selectRecipe("r2");
    assertThat(viewModel.getSelectedRecipeId()).isEqualTo("r2");
    assertThat(viewModel.selectedRecipeIdProperty().get()).isEqualTo("r2");
    assertThat(viewModel.getRecipeIds()).containsExactly("r1", "r2");
  }

  @Test
  void selectNonexistentCollectionHandledGracefully() throws InterruptedException {
    when(librarianService.findCollectionById("missing")).thenReturn(Optional.empty());

    viewModel.selectCollection("missing");

    assertThat(viewModel.getSelectedCollectionId()).isNull();
    assertThat(viewModel.getRecipeIds()).isEmpty();
  }

  // CREATE COLLECTION
  @Test
  void createCollectionAddsToList() throws InterruptedException {
    // Given: Mock service creates a collection
    RecipeCollection newCollection = makeCollection("coll-1", "New Collection", List.of());
    when(librarianService.createCollection("New Collection")).thenReturn(newCollection);

    // When: createCollection is called
    viewModel.createCollection("New Collection");

    // Then: new collection should be added to the list
    assertThat(viewModel.getCollectionIds()).contains("coll-1");
  }

  @Test
  void createCollectionThenRefreshKeepsCreatedCollectionVisible() throws InterruptedException {
    RecipeCollection newCollection = makeCollection("coll-1", "New Collection", List.of());
    when(librarianService.createCollection("New Collection")).thenReturn(newCollection);
    when(librarianService.listCollections()).thenReturn(List.of(newCollection));

    viewModel.createCollection("New Collection");
    awaitCondition(() -> viewModel.getCollectionIds().contains("coll-1"));
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-1");

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    assertThat(viewModel.getCollectionIds()).containsExactly("coll-1");
  }

  @Test
  void createCollectionWithEmptyTitleDoesNothing() throws InterruptedException {
    // When: createCollection is called with empty title
    viewModel.createCollection("   ");

    // Then: no collection should be added
    assertThat(viewModel.collectionsProperty()).isEmpty();
  }

  // DELETE COLLECTION
  @Test
  void deleteCollectionRemovesFromList() throws InterruptedException {
    // Given: Mock service returns one collection
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    // When: deleteCollection is called
    viewModel.deleteCollection("coll-1");

    // Then: collection should be removed from the list
    assertThat(viewModel.getCollectionIds()).doesNotContain("coll-1");
  }

  @Test
  void deleteCollectionCallsServiceOnlyAfterUndoTimeoutExpires() throws InterruptedException {
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    viewModel.deleteCollection("coll-1");

    // Deletion should not be finalized immediately; it should be deferred until timeout.
    verify(librarianService, never()).deleteCollection("coll-1");

    awaitCondition(() -> !viewModel.isUndoAvailable(), 7000);
    verify(librarianService, timeout(1000).times(1)).deleteCollection("coll-1");
  }

  @Test
  void deleteNonExistentCollectionDoesNothing() throws InterruptedException {
    // Given: Mock service returns one collection
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    // When: deleteCollection is called with non-existent ID
    viewModel.deleteCollection("non-existent-id");

    // Then: original collection should still be in the list
    assertThat(viewModel.getCollectionIds()).contains("coll-1");
  }

  @Test
  void deleteCollectionEnablesUndo() throws InterruptedException {
    // Given: Mock service returns one collection
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    // When: deleteCollection is called
    viewModel.deleteCollection("coll-1");

    // Then: undo should be available with correct message
    assertTrue(viewModel.isUndoAvailable());
    assertThat(viewModel.undoMessageProperty().get()).isEqualTo("Deleted: Family Recipes");
  }

  @Test
  void deleteCollectionThenDeleteAnotherCancelsFirstUndo() throws InterruptedException {
    // Given: Mock service returns two collections
    RecipeCollection collection1 = makeCollection("coll-1", "Family Recipes", List.of());
    RecipeCollection collection2 = makeCollection("coll-2", "Desserts", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    // When: delete first collection
    viewModel.deleteCollection("coll-1");

    // When: delete second collection before undoing first
    viewModel.deleteCollection("coll-2");

    // Then: first undo should be cancelled and second undo should be available
    assertTrue(viewModel.isUndoAvailable());
    assertThat(viewModel.undoMessageProperty().get()).isEqualTo("Deleted: Desserts");
  }

  // UNDO DELETE
  @Test
  void deleteCollectionThenUndoRestoresCollection() throws InterruptedException {
    // Given: Mock service returns one collection
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    // When: deleteCollection is called
    viewModel.deleteCollection("coll-1");

    // When: undoDelete is called
    viewModel.undoDelete();

    // Then: collection should be restored to the list
    assertThat(viewModel.getCollectionIds()).contains("coll-1");
    assertFalse(viewModel.isUndoAvailable());
  }

  @Test
  void undoStateClearsAfterTimeout() throws InterruptedException {
    RecipeCollection collection = makeCollection("coll-1", "Family Recipes", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    viewModel.deleteCollection("coll-1");
    assertTrue(viewModel.isUndoAvailable());
    assertThat(viewModel.undoMessageProperty().get()).isEqualTo("Deleted: Family Recipes");

    awaitCondition(() -> !viewModel.isUndoAvailable(), 7000);
    assertFalse(viewModel.isUndoAvailable());
    assertThat(viewModel.undoMessageProperty().get()).isEmpty();
  }

  @Test
  void undoDeleteWithoutPendingDeleteDoesNothing() throws InterruptedException {
    // When: undoDelete is called without a pending delete
    viewModel.undoDelete();

    // Then: nothing should happen (no exceptions, undo remains unavailable)
    assertFalse(viewModel.isUndoAvailable());
  }

  @Test
  void filterTextFiltersByTitleCaseInsensitiveSubstring() throws InterruptedException {
    RecipeCollection collection1 = makeCollection("coll-1", "Family Recipes", List.of());
    RecipeCollection collection2 = makeCollection("coll-2", "Desserts", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    viewModel.filterTextProperty().set("SeR");

    assertThat(viewModel.getCollectionIds()).containsExactly("coll-2");
  }

  @Test
  void filteredListUpdatesImmediatelyAsUserTypes() throws InterruptedException {
    RecipeCollection collection1 = makeCollection("coll-1", "Family Recipes", List.of());
    RecipeCollection collection2 = makeCollection("coll-2", "Desserts", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    viewModel.filterTextProperty().set("d");
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-2");

    viewModel.filterTextProperty().set("fa");
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-1");
  }

  @Test
  void undoDeleteWithActiveFilterRespectsFilterAndClearShowsAll() throws InterruptedException {
    RecipeCollection collection1 = makeCollection("coll-1", "Family Recipes", List.of());
    RecipeCollection collection2 = makeCollection("coll-2", "Desserts", List.of());
    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    viewModel.refresh();
    awaitCondition(() -> !viewModel.isLoading());

    viewModel.filterTextProperty().set("des");
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-2");

    viewModel.deleteCollection("coll-2");
    assertThat(viewModel.getCollectionIds()).isEmpty();

    viewModel.undoDelete();
    assertThat(viewModel.getCollectionIds()).containsExactly("coll-2");

    viewModel.filterTextProperty().set("des");
    viewModel.deleteCollection("coll-2");
    viewModel.filterTextProperty().set("fam");
    viewModel.undoDelete();

    assertThat(viewModel.getCollectionIds()).containsExactly("coll-1");

    viewModel.filterTextProperty().set("");
    assertThat(viewModel.getCollectionIds()).containsExactlyInAnyOrder("coll-1", "coll-2");
  }

  // ─── Test Helpers ───────────────────────────────────────────────────

  private static RecipeCollection makeCollection(String id, String title, List<Recipe> recipes) {
    return PersonalCollectionImpl.builder().id(id).title(title).recipes(recipes).build();
  }

  private static Recipe makeRecipe(String id, String title) {
    return new Recipe(
        id,
        title,
        null,
        List.<Ingredient>of(new VagueIngredient("salt", "to taste", null, null)),
        List.of(new Instruction(1, "mix", List.of())),
        List.of());
  }

  private static void awaitCondition(BooleanSupplier condition) throws InterruptedException {
    awaitCondition(condition, 2000);
  }

  private static void awaitCondition(BooleanSupplier condition, long timeoutMs)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      waitForFxEvents();
    }
    throw new AssertionError("Timed out waiting for condition");
  }
}
