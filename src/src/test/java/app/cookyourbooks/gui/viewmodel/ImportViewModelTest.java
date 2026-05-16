package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.FakeRecipeOcrService;
import app.cookyourbooks.services.ocr.OcrException;

@SuppressWarnings("NullAway.Init")
@ExtendWith(MockitoExtension.class)
class ImportViewModelTest extends ViewModelTestBase {

  @Mock private LibrarianService librarianService;

  private FakeRecipeOcrService ocrService = new FakeRecipeOcrService(50);

  private ImportViewModelImpl vm;

  @BeforeEach
  void setUp() {
    vm = new ImportViewModelImpl(ocrService, librarianService);
  }

  @Test
  void initialState_isIdle() {
    assertThat(vm.getState()).isEqualTo("idle");
    assertThat(vm.getImportedRecipeTitle()).isEmpty();
    assertThat(vm.getErrorMessage()).isEmpty();
  }

  @Test
  void startImport_transitionsToProcessing() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    assertThat(vm.getState()).isEqualTo("processing");
  }

  @Test
  void successfulOcr_transitionsToReview() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();
    waitForFxEvents();
    assertThat(vm.getState()).isEqualTo("review");
    assertThat(vm.getImportedRecipeTitle()).isNotEmpty();
  }

  @Test
  void failedOcr_transitionsToError() throws InterruptedException {
    FakeRecipeOcrService failingOcrService =
        new FakeRecipeOcrService(50) {
          @Override
          public Recipe extractRecipe(Path imagePath) throws OcrException {
            throw new OcrException("Network failed", new RuntimeException());
          }
        };
    var failingVm = new ImportViewModelImpl(failingOcrService, librarianService);

    failingVm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();
    waitForFxEvents();
    assertThat(failingVm.getState()).isEqualTo("error");
    assertThat(failingVm.getErrorMessage()).isNotEmpty();
  }

  @Test
  void acceptImport_savesAndReturnsToIdle() throws InterruptedException {
    // Step 1 - get to review state first
    vm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();

    // Step 2 - select a collection
    vm.selectTargetCollection("collection-1");

    // Step 3 - accept
    vm.acceptImport();

    // Step 4 - assert
    assertThat(vm.getState()).isEqualTo("idle");
    verify(librarianService).saveRecipe(any(), any());
  }

  @Test
  void cancelImport_returnsToIdle() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    vm.cancelImport();
    assertThat(vm.getState()).isEqualTo("idle");
  }

  @Test
  void rejectImport_discardsAndReturnsToIdle() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();
    vm.rejectImport();
    assertThat(vm.getState()).isEqualTo("idle");
    assertThat(vm.getImportedRecipeTitle()).isEmpty();
  }

  @Test
  void collectionsLoaded_fromRepository() {
    when(librarianService.listCollections()).thenReturn(List.of());
    vm.loadCollections();
    assertThat(vm.getAvailableCollectionIds()).isEmpty();
  }

  @Test
  void preSaveEdit_titleAndIngredients() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();

    vm.importedTitleProperty().set("My Edited Title");

    assertThat(vm.getImportedRecipeTitle()).isEqualTo("My Edited Title");
  }

  @Test
  void acceptImport_noopWhenNoCollection() throws InterruptedException {
    vm.startImport(Path.of("test.jpg"));
    Thread.sleep(200);
    waitForFxEvents();

    // don't select a collection
    vm.acceptImport();

    assertThat(vm.getState()).isEqualTo("review");
    verify(librarianService, never()).saveRecipe(any(), any());
  }
}
