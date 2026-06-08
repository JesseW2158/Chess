package Chess;

import java.io.File;
import java.util.ServiceLoader;

import Chess.Core.ChessBoard;
import Chess.Core.AI.Util.Difficulty;
import Chess.Core.Util.IO.DataHelper;
import Chess.Core.Util.UI.OptionButton;
import Chess.Core.Util.IO.ChessIO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ChessGame extends Application {
    private Label status;
    private ChessBoard board;
    private static ChessGame instance;
    private boolean godmode = true;

    @Override
    public void start(Stage primaryStage) {
        instance = this;

        primaryStage.setTitle("Average Magnus Carlsen Match");
        primaryStage.getIcons().add(DataHelper.loadImage("Images/icon.png", 16, 16));

        BorderPane pane = new BorderPane();

        GridPane table = new GridPane();

        for (int i = 0; i < 8; i++) {
            table.add(newRowLabel(i), 0, i + 1, 1, 1);
            table.add(newRowLabel(i), 9, i + 1, 1, 1);
            table.add(newColLabel(i), i + 1, 0, 1, 1);
            table.add(newColLabel(i), i + 1, 9, 1, 1);
        }

        table.add(board = new ChessBoard(), 1, 1, 8, 8);
        table.setAlignment(Pos.CENTER);
        pane.setCenter(table);

        BorderPane menu = new BorderPane();
        menu.setPadding(new Insets(10, 10, 10, 0));

        GridPane options = new GridPane();
        options.setAlignment(Pos.BOTTOM_RIGHT);

        if (godmode) {
            options.add(new OptionButton(
                    "images/clear.png",
                    e -> board.clear(),
                    "Clear"),
                    0, 0, 1, 1);
            options.add(new OptionButton(
                    "images/swap.png",
                    e -> board.nextTurn(),
                    "Skip turn"),
                    1, 0, 1, 1);
        }

        options.add(new OptionButton(
                "Images/reset.png",
                e -> {
                    board.clear();
                    board.loadFromResource("init.json");
                },
                "Reset"),
                2, 0, 1, 1);
        options.add(new OptionButton(
                "Images/save.png",
                e -> {
                    File file = createFileChooser().showSaveDialog(primaryStage);
                    if (file != null) {
                        board.save(file);
                    }
                },
                "Save"),
                3, 0, 1, 1);
        options.add(new OptionButton(
                "Images/load.png",
                e -> {
                    File file = createFileChooser().showOpenDialog(primaryStage);
                    if (file != null) {
                        board.load(file);
                    }
                },
                "Load"),
                4, 0, 1, 1);
        options.add(new OptionButton(
                "Images/swap.png",
                e -> board.undo(),
                "Undo"),
                5, 0, 1, 1);
        options.add(createDifficultySelector(), 6, 0, 1, 1);
        menu.setRight(options);

        status = new Label();
        status.setAlignment(Pos.BOTTOM_LEFT);
        status.setPadding(new Insets(10, 0, 10, 10));
        menu.setLeft(status);

        pane.setBottom(menu);

        Scene scene = new Scene(pane, 640, 690);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());

        ServiceLoader.load(ChessIO.class).forEach(board::setIO);
        board.loadFromResource("init.json");
    }

    private Label newRowLabel(int i) {
        Label l = new Label(8 - i + "");
        l.setMinSize(20, 75);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private Label newColLabel(int i) {
        Label l = new Label((char) (i + 65) + "");
        l.setMinSize(75, 20);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    /** A dropdown that sets the AI's per-move thinking-time budget. */
    private ComboBox<Difficulty> createDifficultySelector() {
        ComboBox<Difficulty> selector = new ComboBox<>();
        selector.getItems().addAll(Difficulty.values());
        selector.setValue(board.getDifficulty());
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Difficulty d) {
                if (d == null) {
                    return "";
                }
                String name = d.name().charAt(0) + d.name().substring(1).toLowerCase();
                return name + " (" + d.budgetMillis() / 1000 + "s)";
            }

            @Override
            public Difficulty fromString(String s) {
                return null; // selection-only, never typed
            }
        });
        selector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                board.setDifficulty(val);
            }
        });
        selector.setTooltip(new Tooltip("AI difficulty (engine thinking time)"));
        return selector;
    }

    private FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        board.getIO().forEach((k, v) -> fileChooser.getExtensionFilters().add(
                new ExtensionFilter(v.getFileTypeDescription(), "*." + v.getFileExtension())));
        return fileChooser;
    }

    public static void displayStatusText(String text) {
        instance.status.setText(text);
    }

    public static boolean isGodmode() {
        return instance.godmode;
    }

    public static ChessBoard getBoard() {
        return instance.board;
    }
}
