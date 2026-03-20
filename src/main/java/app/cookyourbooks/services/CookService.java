package app.cookyourbooks.services;

import java.util.List;
import java.util.Objects;

import app.cookyourbooks.model.Recipe;

/** Cook actor service focused on recipe lookup for cook mode sessions. */
public class CookService {

  private final LibrarianService librarianService;

  public CookService(LibrarianService librarianService) {
    this.librarianService =
        Objects.requireNonNull(librarianService, "librarianService must not be null");
  }

  public List<Recipe> findRecipes(String query) {
    return librarianService.findRecipes(query);
  }
}
