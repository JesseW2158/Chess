# Chess

A desktop chess game written in Java with a JavaFX interface. Play against a
built-in AI engine that searches with negamax alpha-beta, iterative deepening,
and a transposition table. The board supports the full rules of chess, plus
save/load, undo, and an editor mode for setting up custom positions.

## Features

- **Full chess rules** — legal move generation, castling, en passant, and pawn
  promotion (with a piece-selection dialog).
- **Game-end detection** — checkmate, stalemate, draw by threefold repetition,
  and the 50-move rule.
- **AI opponent** — plays Black by default. The engine uses:
  - Negamax alpha-beta search with quiescence search
  - Iterative deepening under a per-move time budget
  - A transposition table (Zobrist hashing, ~1M entries)
  - Move ordering via MVV-LVA, killer moves, and history heuristics
  - Null-move pruning and late-move reductions
- **Difficulty levels** — each level is a thinking-time budget: Easy (1s),
  Medium (2s), Hard (5s), Impossible (10s).
- **Undo** — takes back to the previous human-to-move position.
- **Save / load** — positions are stored as JSON via a pluggable I/O layer
  (`ServiceLoader`).
- **Editor ("godmode")** — clear the board, skip a turn, and right-click any
  square to add or remove pieces.

## Requirements

- **JDK 21.** The Gradle build is configured with a Java 21 toolchain. If a
  matching JDK isn't found on your machine, Gradle will auto-download one (via
  the foojay resolver), so a manual install usually isn't required.
- JavaFX is supplied automatically by the Gradle JavaFX plugin — no separate
  JavaFX SDK download is needed when running through Gradle.

## Running

From the project root:

```sh
# Windows
gradlew.bat run

# macOS / Linux
./gradlew run
```

The app launches through `Chess.Launcher`, a thin wrapper around the JavaFX
`Application`. This sidesteps the "JavaFX runtime components are missing" error
you'd otherwise hit when launching `ChessGame` directly, so the game starts
cleanly under `./gradlew run`, an IDE "Run" button, or a plain `java -jar`.

> Note: `Chess.ChessGame` has no `main` method by design — always start from
> `Chess.Launcher`.

## Building a JAR

```sh
./gradlew jar
```

The resulting JAR declares `Chess.Launcher` as its `Main-Class`.

## Running the tests

```sh
./gradlew test
```

Tests run on JUnit 4 and cover the engine, evaluation, transposition table,
Zobrist hashing, repetition detection, and move generation (including perft).

## How to play

1. Launch the game; you play White, the AI plays Black.
2. Click one of your pieces, then click a destination square to move.
3. When a pawn reaches the last rank, choose the promotion piece in the dialog.
4. Use the toolbar at the bottom for **Reset**, **Save**, **Load**, **Undo**,
   and to pick the AI **difficulty**. In editor mode you also get **Clear** and
   **Skip turn**, plus a right-click menu on each square to place pieces.

Game status (check, checkmate, stalemate, draws, and the AI's last move) is
shown in the bottom-left of the window.

## Project structure

```
src/main/java/Chess/
├── Launcher.java          # entry point (JavaFX wrapper)
├── ChessGame.java         # main window, toolbar, difficulty selector
└── Core/
    ├── ChessBoard.java    # board state, turn flow, game-end checks
    ├── AI/                # search engine, transposition table, evaluation
    ├── Pieces/            # piece types and their move logic
    └── Util/              # squares, colors, JSON I/O, UI dialogs

src/main/resources/        # piece images, icons, initial position (init.json)
src/test/java/             # JUnit tests
```

## License

[MIT License](LICENSE) © 2026 Jesse Wang
