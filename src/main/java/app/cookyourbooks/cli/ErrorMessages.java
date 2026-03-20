package app.cookyourbooks.cli;

/** Shared CLI error message helpers. */
public final class ErrorMessages {

  private ErrorMessages() {}

  public static String recipeNotFound(String title) {
    return "Recipe not found: '" + title + "'. Use 'search' to find recipes by ingredient.";
  }

  public static String collectionNotFound(String title) {
    return "Collection not found: '" + title + "'. Use 'collections' to see available collections.";
  }
}
