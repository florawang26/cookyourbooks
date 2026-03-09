package app.cookyourbooks.cli.completion;

/**
 * Mutable holder for cook mode state.
 *
 * <p><b>IMPORTANT:</b> You MUST use this class to track whether the user is in cook mode. The test
 * harness calls {@link #setInCookMode(boolean)} to simulate cook mode for completion tests. Your
 * cook mode implementation must call {@code setInCookMode(true)} when entering cook mode and {@code
 * setInCookMode(false)} when exiting.
 *
 * <p>Your tab completer for cook mode commands should check {@link #isInCookMode()} to determine
 * whether to offer cook-mode-specific completions (next, prev, ingredients, quit).
 */
public final class CookModeHolder {

  private volatile boolean inCookMode;

  /** Returns true when the user is in interactive cook mode. */
  public boolean isInCookMode() {
    return inCookMode;
  }

  /** Sets whether the user is in cook mode. */
  public void setInCookMode(boolean inCookMode) {
    this.inCookMode = inCookMode;
  }
}
