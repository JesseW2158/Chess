package Chess;

import javafx.application.Application;

/**
 * The application's only entry point. It intentionally does NOT extend
 * {@link Application}.
 *
 * <p>When the class named on the command line extends {@code Application}, the JVM
 * launcher requires JavaFX to be present as named modules on the module path and
 * otherwise fails with "JavaFX runtime components are missing, and are required to
 * run this application." Launching through this wrapper sidesteps that check, so the
 * app also starts when JavaFX is only on the classpath (an IDE "Run" button or a
 * plain {@code java -jar}). {@code ./gradlew run} uses this same entry point.
 *
 * <p>{@link ChessGame} deliberately has no {@code main} method so it cannot be run
 * directly (doing so would hit the error above) -- always start from here.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(ChessGame.class, args);
    }
}