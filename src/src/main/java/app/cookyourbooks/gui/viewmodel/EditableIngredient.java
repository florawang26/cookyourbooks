package app.cookyourbooks.gui.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.VagueIngredient;

/**
 * Mutable, UI-friendly wrapper around immutable domain {@link Ingredient} objects.
 *
 * <p>This type is designed for JavaFX form binding where users may temporarily enter incomplete
 * values (for example, blank names while typing). A view model can later validate and convert this
 * wrapper back into a domain ingredient for persistence.
 */
public final class EditableIngredient {

  private final StringProperty name = new SimpleStringProperty("");
  private final StringProperty description = new SimpleStringProperty("");
  private final @Nullable Ingredient originalIngredient;

  /**
   * Creates a mutable ingredient row from plain editable values.
   *
   * @param name ingredient name shown in the editor
   * @param description ingredient description shown in the editor
   */
  public EditableIngredient(String name, String description) {
    this(name, description, null);
  }

  private EditableIngredient(
      String name, String description, @Nullable Ingredient originalIngredient) {
    this.name.set(name == null ? "" : name);
    this.description.set(description == null ? "" : description);
    this.originalIngredient = originalIngredient;
  }

  /**
   * Creates an editable wrapper from an immutable domain ingredient.
   *
   * @param ingredient domain ingredient to wrap
   * @return mutable wrapper suitable for UI editing
   */
  public static EditableIngredient fromIngredient(Ingredient ingredient) {
    if (ingredient instanceof MeasuredIngredient measured) {
      return new EditableIngredient(
          ingredient.getName(), measured.getQuantity().toString(), ingredient);
    }
    if (ingredient instanceof VagueIngredient vague) {
      return new EditableIngredient(
          ingredient.getName(), nullToEmpty(vague.getDescription()), ingredient);
    }
    return new EditableIngredient(ingredient.getName(), "", ingredient);
  }

  /** Returns a mutable JavaFX property for ingredient name binding. */
  public StringProperty nameProperty() {
    return name;
  }

  /** Returns a mutable JavaFX property for ingredient description binding. */
  public StringProperty descriptionProperty() {
    return description;
  }

  /** Returns the current ingredient name text. */
  public String getName() {
    return name.get();
  }

  /** Returns the current ingredient description text. */
  public String getDescription() {
    return description.get();
  }

  /** Sets ingredient name text (null is normalized to empty string). */
  public void setName(String value) {
    name.set(value == null ? "" : value);
  }

  /** Sets ingredient description text (null is normalized to empty string). */
  public void setDescription(String value) {
    description.set(value == null ? "" : value);
  }

  /**
   * Converts this editable wrapper back into a domain ingredient.
   *
   * <p>If the original ingredient was measured, this preserves quantity/preparation/notes while
   * applying the edited name. Otherwise this produces a {@link VagueIngredient} using current name
   * and description text.
   *
   * @return immutable domain ingredient
   */
  public Ingredient toIngredient() {
    if (originalIngredient instanceof MeasuredIngredient measured) {
      return new MeasuredIngredient(
          getName(), measured.getQuantity(), measured.getPreparation(), measured.getNotes());
    }

    String desc = emptyToNull(getDescription());
    if (originalIngredient instanceof VagueIngredient vague) {
      return new VagueIngredient(getName(), desc, vague.getPreparation(), vague.getNotes());
    }

    return new VagueIngredient(getName(), desc, null, null);
  }

  private static String nullToEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }

  private static @Nullable String emptyToNull(String value) {
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
