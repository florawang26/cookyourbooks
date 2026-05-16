package app.cookyourbooks.adapters;

/**
 * Thrown when a PDF export operation fails.
 *
 * <p>This may occur when the target file cannot be written, or when the PDF document cannot be
 * created.
 */
public class PdfExportException extends Exception {

  /**
   * Constructs a PdfExportException with the specified message.
   *
   * @param message the detail message
   */
  public PdfExportException(String message) {
    super(message);
  }

  /**
   * Constructs a PdfExportException with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public PdfExportException(String message, Throwable cause) {
    super(message, cause);
  }
}
