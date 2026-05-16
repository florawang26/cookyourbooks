package app.cookyourbooks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.cookyourbooks.adapters.MarkdownExporter;
import app.cookyourbooks.adapters.PdfExportException;
import app.cookyourbooks.adapters.PdfExporter;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

class PlannerServiceImplTest {

  private PlannerService plannerService;
  private Recipe recipe;

  @BeforeEach
  void setUp() {
    plannerService =
        new PlannerServiceImpl(
            new ShoppingListAggregator(), new MarkdownExporter(), new PdfExporter());

    recipe =
        new Recipe(
            "Pasta",
            null,
            List.of(
                new MeasuredIngredient("pasta", new ExactQuantity(200.0, Unit.GRAM), null, null)),
            List.of(new Instruction(1, "Boil the pasta.", List.of())),
            List.of());
  }

  @Test
  void exportToPdf_writesNonEmptyPdfFile(@TempDir Path tempDir)
      throws PdfExportException, IOException {
    Path output = tempDir.resolve("pasta.pdf");

    plannerService.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void exportToPdf_throwsPdfExportException_whenPathIsInvalid(@TempDir Path tempDir) {
    Path invalidPath = tempDir.resolve("nonexistent-dir/pasta.pdf");

    assertThatThrownBy(() -> plannerService.exportToPdf(recipe, invalidPath))
        .isInstanceOf(PdfExportException.class);
  }
}
