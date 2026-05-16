package app.cookyourbooks.gui.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import app.cookyourbooks.model.Instruction;

/** Mutable UI wrapper for editing a recipe instruction step's text. */
public final class EditableInstruction {

  private final StringProperty text = new SimpleStringProperty("");

  public EditableInstruction(String text) {
    this.text.set(text);
  }

  public static EditableInstruction fromInstruction(Instruction instruction) {
    return new EditableInstruction(instruction.getText());
  }

  public StringProperty textProperty() {
    return text;
  }

  public String getText() {
    return text.get();
  }

  public void setText(String text) {
    this.text.set(text);
  }
}
