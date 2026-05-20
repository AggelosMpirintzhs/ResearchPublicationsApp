package util;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class JavaFxTestSupport {

    private JavaFxTestSupport() {
    }

    static void initializeJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(latch::countDown);
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        if (!completed) {
            throw new RuntimeException("JavaFX toolkit did not start in time.");
        }
    }

    static void runOnJavaFxThread(ThrowingRunnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        if (!completed) {
            throw new RuntimeException("JavaFX action did not finish in time.");
        }

        Throwable throwable = error.get();

        if (throwable == null) {
            return;
        }

        if (throwable instanceof Exception) {
            throw (Exception) throwable;
        }

        if (throwable instanceof AssertionError) {
            throw (AssertionError) throwable;
        }

        throw new RuntimeException(throwable);
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}