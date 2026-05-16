package app.cookyourbooks.gui;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeAll;

public abstract class ViewModelTestBase {
  @BeforeAll
  static void initToolkit() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // already initialized
    }
  }

  protected static void waitForFxEvents() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(latch::countDown);
    latch.await();
  }

  /**
   * Polls until {@code condition} is true, flushing FX events between each check.
   *
   * <p>Use this when a background thread posts a result to the FX thread and you need to wait for
   * it to land before asserting. Times out after 3 seconds to prevent tests hanging forever.
   */
  protected static void waitForCondition(java.util.function.BooleanSupplier condition)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3000;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > deadline) {
        throw new AssertionError("Timed out waiting for condition");
      }
      waitForFxEvents();
    }
  }
}
