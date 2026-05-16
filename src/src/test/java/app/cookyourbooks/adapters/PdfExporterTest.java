package app.cookyourbooks.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;

class PdfExporterTest {

  private PdfExporter exporter;
  private Recipe fullRecipe;
  private Recipe noServingsRecipe;

  @BeforeEach
  void setUp() {
    exporter = new PdfExporter();

    fullRecipe =
        new Recipe(
            "Chocolate Cake",
            new Servings(8),
            List.of(new MeasuredIngredient("flour", new ExactQuantity(2.0, Unit.CUP), null, null)),
            List.of(new Instruction(1, "Mix ingredients.", List.of())),
            List.of());

    noServingsRecipe =
        new Recipe(
            "Simple Toast",
            null,
            List.of(
                new MeasuredIngredient("bread", new ExactQuantity(2.0, Unit.WHOLE), null, null)),
            List.of(new Instruction(1, "Toast the bread.", List.of())),
            List.of());
  }

  @Test
  void exportToFile_writesNonEmptyPdfFile(@TempDir Path tempDir)
      throws PdfExportException, IOException {
    Path output = tempDir.resolve("recipe.pdf");

    exporter.exportToFile(fullRecipe, output);

    assertThat(output).exists();
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void exportToFile_succeedsWithNullServings(@TempDir Path tempDir)
      throws PdfExportException, IOException {
    Path output = tempDir.resolve("no-servings.pdf");

    exporter.exportToFile(noServingsRecipe, output);

    assertThat(output).exists();
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void exportToFile_throwsPdfExportException_whenPathIsInvalid(@TempDir Path tempDir) {
    Path invalidPath = tempDir.resolve("nonexistent-dir/recipe.pdf");

    assertThatThrownBy(() -> exporter.exportToFile(fullRecipe, invalidPath))
        .isInstanceOf(PdfExportException.class)
        .hasMessageContaining("Failed to write PDF to file");
  }
}
