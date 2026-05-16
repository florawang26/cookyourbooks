package app.cookyourbooks.adapters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import app.cookyourbooks.model.Recipe;

/** Exports recipes to PDF format. */
public class PdfExporter {

  /** Constructs a new PdfExporter. */
  public PdfExporter() {
    // Stateless exporter - no initialization needed
  }

  /**
   * Exports a recipe to a PDF file.
   *
   * @param recipe the recipe to export
   * @param file the path to write the PDF file
   * @throws PdfExportException if the file cannot be written or the PDF cannot be created
   */
  public void exportToFile(Recipe recipe, Path file) throws PdfExportException {
    try {
      Files.write(file, buildPdfBytes(recipe));
    } catch (IOException e) {
      throw new PdfExportException("Failed to write PDF to file: " + file, e);
    }
  }

  private byte[] buildPdfBytes(Recipe recipe) {
    StringBuilder content = new StringBuilder();
    content.append("BT\n");
    content.append("/F1 20 Tf\n");
    content.append("72 720 Td\n");
    content.append("(").append(escapePdfText(recipe.getTitle())).append(") Tj\n");

    int yOffset = 24;
    if (recipe.getServings() != null) {
      content.append("/F1 12 Tf\n");
      content.append("0 -").append(yOffset).append(" Td\n");
      content.append("(").append(escapePdfText("Serves: " + recipe.getServings())).append(") Tj\n");
      yOffset = 18;
    }

    content.append("/F1 14 Tf\n");
    content.append("0 -").append(yOffset).append(" Td\n");
    content.append("(Ingredients) Tj\n");
    content.append("/F1 11 Tf\n");
    for (var ingredient : recipe.getIngredients()) {
      content.append("0 -14 Td\n");
      content.append("(").append(escapePdfText("- " + ingredient.toString())).append(") Tj\n");
    }

    content.append("/F1 14 Tf\n");
    content.append("0 -20 Td\n");
    content.append("(Instructions) Tj\n");
    content.append("/F1 11 Tf\n");
    List<String> instructionTexts = new ArrayList<>();
    for (int i = 0; i < recipe.getInstructions().size(); i++) {
      instructionTexts.add((i + 1) + ". " + recipe.getInstructions().get(i).toString());
    }
    for (String instruction : instructionTexts) {
      content.append("0 -14 Td\n");
      content.append("(").append(escapePdfText(instruction)).append(") Tj\n");
    }

    content.append("0 -20 Td\n");
    content.append("/F1 9 Tf\n");
    content
        .append("(")
        .append(escapePdfText("Exported from CookYourBooks - https://www.cookyourbooks.app"))
        .append(") Tj\n");
    content.append("ET\n");

    StringBuilder pdf = new StringBuilder();
    List<Integer> offsets = new ArrayList<>();

    pdf.append("%PDF-1.4\n");
    offsets.add(0);
    offsets.add(pdf.length());
    pdf.append("1 0 obj\n");
    pdf.append("<< /Type /Catalog /Pages 2 0 R >>\n");
    pdf.append("endobj\n");

    offsets.add(pdf.length());
    pdf.append("2 0 obj\n");
    pdf.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
    pdf.append("endobj\n");

    offsets.add(pdf.length());
    pdf.append("3 0 obj\n");
    pdf.append(
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\n");
    pdf.append("endobj\n");

    offsets.add(pdf.length());
    pdf.append("4 0 obj\n");
    pdf.append("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n");
    pdf.append("endobj\n");

    offsets.add(pdf.length());
    pdf.append("5 0 obj\n");
    pdf.append("<< /Length ").append(content.length()).append(" >>\n");
    pdf.append("stream\n");
    pdf.append(content);
    pdf.append("endstream\n");
    pdf.append("endobj\n");

    int xrefStart = pdf.length();
    pdf.append("xref\n");
    pdf.append("0 6\n");
    pdf.append("0000000000 65535 f \n");
    for (int i = 1; i < offsets.size(); i++) {
      pdf.append(String.format("%010d 00000 n \n", offsets.get(i)));
    }
    pdf.append("trailer\n");
    pdf.append("<< /Size 6 /Root 1 0 R >>\n");
    pdf.append("startxref\n");
    pdf.append(xrefStart).append('\n');
    pdf.append("%%EOF\n");

    return pdf.toString().getBytes(StandardCharsets.US_ASCII);
  }

  private String escapePdfText(String text) {
    StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      switch (ch) {
        case '(', ')', '\\' -> escaped.append('\\').append(ch);
        default -> escaped.append(ch <= 0x7F ? ch : '?');
      }
    }
    return escaped.toString();
  }
}
