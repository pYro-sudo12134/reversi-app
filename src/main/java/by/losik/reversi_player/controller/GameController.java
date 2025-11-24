package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.Disk;
import by.losik.reversi_player.exception.WrongPlacementException;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.ClientVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class GameController extends BaseController implements IAlertable, ILaunchable {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 50;
    private static final int OFFSET = 10;
    private final AtomicBoolean gameStarted = new AtomicBoolean(false);
    private boolean isBlackTurn = true;
    private boolean playBG = true;
    private Boolean multiplayer = false;
    private boolean isHost = false;
    private String opponentId;
    public VBox rotationBox;
    public MediaView bgMusicView;
    public MediaView mediaView;
    public Label whiteScoreLabelCount;
    public Label blackScoreLabelCount;
    public Label turnLabel;
    public GridPane gridPane;
    private RotateTransition rotateToBlack;
    private RotateTransition rotateToWhite;
    private MediaPlayer bgMusicPlayer;
    private ClientVerticle clientVerticle;

    public void initialize() {
        try {
            setupGame();
        } catch (Exception e) {
            logger.error("Initialization error: {}", e.getMessage());
            warn("Failed to initialize game");
        }
    }

    private void setupGame() throws WrongPlacementException {
        resetGame();
        setupAnimations();
        setupBackgroundMusic();

        if (multiplayer) {
            initializeMultiplayer();
        } else {
            gridPane.setDisable(false);
        }
    }

    private void setupAnimations() {
        rotateToBlack = new RotateTransition(Duration.millis(1000), rotationBox);
        rotateToBlack.setFromAngle(0);
        rotateToBlack.setToAngle(180);
        rotateToBlack.setAxis(Rotate.Z_AXIS);

        rotateToWhite = new RotateTransition(Duration.millis(1000), rotationBox);
        rotateToWhite.setFromAngle(180);
        rotateToWhite.setToAngle(0);
        rotateToWhite.setAxis(Rotate.Z_AXIS);

        if (!isHost && multiplayer) {
            rotateToBlack.play();
        }
    }

    private void resetGame() throws WrongPlacementException {
        isBlackTurn = true;
        updateTurnLabel();
        whiteScoreLabelCount.setText("2");
        blackScoreLabelCount.setText("2");
        setupGridClickHandler();
        placeInitialDisks();
    }

    private void updateTurnLabel() {
        if (multiplayer) {
            turnLabel.setText(isHost ?
                    (isBlackTurn ? "Your Turn (Black)" : "Opponent's Turn (White)") :
                    (isBlackTurn ? "Opponent's Turn (Black)" : "Your Turn (White)"));
        } else {
            turnLabel.setText(isBlackTurn ? "Black's Turn" : "White's Turn");
        }
    }

    private void initializeMultiplayer() {
        gridPane.setDisable(true);

        if (isHost) {
            initializeHostGame();
        } else {
            initializeGuestGame();
        }
    }

    private void initializeHostGame() {
        clientVerticle.start();
        clientVerticle.createChild()
                .onSuccess(childInfo -> {
                    clientVerticle.setConnectedChildId(childInfo.getString("id"));
                    waitForOpponentJoin();
                })
                .onFailure(err -> {
                    logger.error("Failed to create game: {}", err.getMessage());
                    warn("Failed to create multiplayer game");
                });
    }

    private void initializeGuestGame() {
        clientVerticle.start();
        clientVerticle.connectToChild(clientVerticle.getConnectedChildId())
                .onSuccess(v -> {
                    sendJoinMessage();
                    waitForGameStart();
                })
                .onFailure(err -> {
                    logger.error("Failed to join game: {}", err.getMessage());
                    warn("Failed to join multiplayer game");
                });
    }

    private void sendJoinMessage() {
        JsonObject joinMessage = new JsonObject()
                .put("type", "join")
                .put("userId", userData.getId());

        clientVerticle.sendWebSocketMessage(joinMessage)
                .onFailure(err -> logger.error("Failed to send join message: {}", err.getMessage()));
    }

    private void waitForOpponentJoin() {
        clientVerticle.waitForNextMessage()
                .onSuccess(message -> Platform.runLater(() -> {
                    processMessage(message);
                    gridPane.setDisable(false);
                    updateWindowTitle();
                    showAlert(Alert.AlertType.INFORMATION, "Game Started", "Opponent has joined!");
                }))
                .onFailure(err -> logger.error("Error waiting for opponent: {}", err.getMessage()));
    }

    private void waitForGameStart() {
        clientVerticle.waitForNextMessage()
                .onSuccess(message -> Platform.runLater(() -> {
                    processMessage(message);
                    gridPane.setDisable(false);
                    updateWindowTitle();
                }))
                .onFailure(err -> logger.error("Error waiting for game start: {}", err.getMessage()));
    }

    private void updateWindowTitle() {
        currentStage.setTitle("Reversi - Game ID: " + clientVerticle.getConnectedChildId());
    }

    private void setupGridClickHandler() {
        gridPane.setOnMouseClicked(event -> {
            if (multiplayer && ((isHost && !isBlackTurn) || (!isHost && isBlackTurn))) {
                warn("Wait for your turn!");
                return;
            }

            int col = calculateBoardCoordinate(event.getX());
            int row = calculateBoardCoordinate(event.getY());

            try {
                if (isValidMove(row, col)) {
                    makeMove(row, col);
                } else {
                    warn("Invalid move!");
                }
            } catch (WrongPlacementException e) {
                logger.error("Move error: {}", e.getMessage());
                warn(e.getMessage());
            }
        });
    }

    private void makeMove(int row, int col) throws WrongPlacementException {
        List<Disk> disksToFlip = getDisksToFlip(row, col);
        placeDisk(new Disk(isBlackTurn, row, col));
        flipDisks(disksToFlip);
        playSound("chips_flip.wav");
        updateScores();

        if (!multiplayer) {
            isBlackTurn = !isBlackTurn;
            if (!checkGameEnd()) {
                updateGameState();
            }
            return;
        }

        isBlackTurn = !isBlackTurn;
        gridPane.setDisable(true);

        sendMoveToOpponent(row, col)
                .compose(v -> {
                    if (checkGameEnd()) {
                        return Future.succeededFuture();
                    }
                    return clientVerticle.waitForNextMessage();
                })
                .onSuccess(msg -> Platform.runLater(() -> {
                    if (msg != null) processMessage(msg);
                    updateGameState();
                }))
                .onFailure(err -> {
                    logger.error("Move failed: {}", err.getMessage());
                    warn("Network error - failed to communicate with opponent");
                });
    }

    private Future<Void> sendMoveToOpponent(int row, int col) {
        JsonObject moveMessage = new JsonObject()
                .put("type", "move")
                .put("userId", userData.getId())
                .put("x", row)
                .put("y", col);

        return clientVerticle.sendWebSocketMessage(moveMessage);
    }

    private void processMessage(JsonObject message) {
        String type = message.getString("type");
        String userId = message.getString("userId");

        if (userId == null || userId.equals(userData.getId())) return;

        opponentId = userId;

        switch (type) {
            case "join" -> handleOpponentJoin();
            case "move" -> handleOpponentMove(message);
            case "gameEnd" -> handleGameEnd(message);
            default -> logger.warn("Unknown message type: {}", type);
        }
    }

    private void handleOpponentJoin() {
        if (!gameStarted.compareAndSet(false, true)) {
            logger.warn("Opponent tried to join an already started game");
            return;
        }
        Platform.runLater(() -> {
            showAlert(Alert.AlertType.INFORMATION, "Game Started", "Opponent has joined!");
            updateTurnLabel();
        });
    }

    private void handleOpponentMove(JsonObject message) {
        Platform.runLater(() -> {
            try {
                int x = message.getInteger("x");
                int y = message.getInteger("y");

                List<Disk> disksToFlip = getDisksToFlip(x, y);
                placeDisk(new Disk(isBlackTurn, x, y));
                flipDisks(disksToFlip);
                playSound("chips_flip.wav");

                isBlackTurn = !isBlackTurn;
                updateGameState();
                gridPane.setDisable(false);
            } catch (Exception e) {
                logger.error("Invalid opponent move: {}", e.getMessage());
            }
        });
    }

    private void handleGameEnd(JsonObject message) {
        gameStarted.set(false);
        String winnerId = message.getString("winner");

        Platform.runLater(() -> {
            if (winnerId.equals(userData.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "You Won!", "Congratulations!");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Game Over", "You lost this match");
            }
            exitToMenu();
        });
    }

    private List<Disk> getDisksToFlip(int row, int col) {
        boolean currentColor = isBlackTurn;
        Paint currentPaint = Paint.valueOf(currentColor ? "BLACK" : "WHITE");
        List<Disk> allDisksToFlip = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                List<Disk> disksInDirection = new ArrayList<>();
                int r = row + dx;
                int c = col + dy;

                while (isWithinBounds(r, c)) {
                    Disk disk = getDiskAt(r, c);
                    if (disk == null) break;

                    if (disk.getCircle().getFill().equals(currentPaint)) {
                        allDisksToFlip.addAll(disksInDirection);
                        break;
                    } else {
                        disksInDirection.add(disk);
                    }

                    r += dx;
                    c += dy;
                }
            }
        }
        return allDisksToFlip;
    }

    private void placeInitialDisks() throws WrongPlacementException {
        gridPane.getChildren().removeIf(node -> node instanceof Circle);
        placeDisk(new Disk(false, 4, 4));
        placeDisk(new Disk(true, 4, 5));
        placeDisk(new Disk(true, 5, 4));
        placeDisk(new Disk(false, 5, 5));
    }

    private void placeDisk(Disk disk) throws WrongPlacementException {
        if (!isPositionEmpty(disk.getRow(), disk.getColumn())) {
            throw new WrongPlacementException("Position already occupied");
        }

        Circle diskCircle = disk.getCircle();
        GridPane.setRowIndex(diskCircle, disk.getRow());
        GridPane.setColumnIndex(diskCircle, disk.getColumn());
        gridPane.getChildren().add(diskCircle);
    }

    private void flipDisks(List<Disk> disksToFlip) {
        disksToFlip.forEach(disk -> {
            removeDisk(disk.getRow(), disk.getColumn());
            try {
                placeDisk(new Disk(!disk.isBlack(), disk.getRow(), disk.getColumn()));
            } catch (WrongPlacementException e) {
                logger.error("Error flipping disk: {}", e.getMessage());
            }
        });
    }

    private void removeDisk(int row, int col) {
        gridPane.getChildren().removeIf(node ->
                node instanceof Circle &&
                        GridPane.getRowIndex(node) == row &&
                        GridPane.getColumnIndex(node) == col
        );
    }

    private boolean isWithinBounds(int row, int col) {
        return row >= 1 && row <= BOARD_SIZE && col >= 1 && col <= BOARD_SIZE;
    }

    private boolean isPositionEmpty(int row, int col) {
        return getDiskAt(row, col) == null;
    }

    private boolean isValidMove(int row, int col) {
        return isWithinBounds(row, col) &&
                isPositionEmpty(row, col) &&
                !getDisksToFlip(row, col).isEmpty();
    }

    private Disk getDiskAt(int row, int col) {
        for (Node node : gridPane.getChildren()) {
            Integer nodeRow = GridPane.getRowIndex(node);
            Integer nodeCol = GridPane.getColumnIndex(node);

            if (nodeRow != null && nodeCol != null &&
                    nodeRow == row && nodeCol == col &&
                    node instanceof Circle) {
                try {
                    return new Disk(
                            ((Circle) node).getFill().equals(Paint.valueOf("BLACK")),
                            row, col
                    );
                } catch (WrongPlacementException e) {
                    logger.error("Invalid disk creation: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    private boolean checkGameEnd() {
        boolean currentPlayerHasMoves = hasValidMoves(isBlackTurn);

        if (!currentPlayerHasMoves) {
            boolean opponentHasMoves = hasValidMoves(!isBlackTurn);

            if (!opponentHasMoves) {
                endGame();
            } else {
                isBlackTurn = !isBlackTurn;
                turnLabel.setText(isBlackTurn ?
                        "Black's Turn (Opponent skipped)" :
                        "White's Turn (Opponent skipped)");
            }
            return true;
        }
        return false;
    }

    private boolean hasValidMoves(boolean forBlack) {
        boolean originalTurn = isBlackTurn;
        isBlackTurn = forBlack;

        boolean hasMoves = IntStream.rangeClosed(1, BOARD_SIZE)
                .boxed()
                .flatMap(row -> IntStream.rangeClosed(1, BOARD_SIZE)
                        .mapToObj(col -> new int[]{row, col}))
                .anyMatch(pos -> isValidMove(pos[0], pos[1]));

        isBlackTurn = originalTurn;
        return hasMoves;
    }

    private void endGame() {
        int blackScore = Integer.parseInt(blackScoreLabelCount.getText());
        int whiteScore = Integer.parseInt(whiteScoreLabelCount.getText());

        String winner;
        String winnerId = "";
        String loserId = "";

        if (blackScore > whiteScore) {
            winner = "Black wins!";
            if (multiplayer) {
                winnerId = isHost ? userData.getId() : opponentId;
                loserId = isHost ? opponentId : userData.getId();
            }
        } else if (whiteScore > blackScore) {
            winner = "White wins!";
            if (multiplayer) {
                winnerId = isHost ? opponentId : userData.getId();
                loserId = isHost ? userData.getId() : opponentId;
            }
        } else {
            winner = "It's a tie!";
        }

        showAlert(Alert.AlertType.INFORMATION, "Game Over", winner);

        if (multiplayer) {
            JsonObject endMessage = new JsonObject()
                    .put("type", "gameEnd")
                    .put("winner", winnerId)
                    .put("loser", loserId);

            clientVerticle.sendWebSocketMessage(endMessage)
                    .onComplete(res -> exitToMenu());
        } else {
            exitToMenu();
        }
    }

    private void updateGameState() {
        updateTurnLabel();
        updateScores();
        updateRotationAnimation();
    }

    private void updateRotationAnimation() {
        if (isHost) {
            if (!isBlackTurn) rotateToBlack.play();
            else rotateToWhite.play();
        } else {
            if (isBlackTurn) rotateToBlack.play();
            else rotateToWhite.play();
        }
    }

    private void updateScores() {
        int blackCount = 0;
        int whiteCount = 0;

        for (Node node : gridPane.getChildren()) {
            if (node instanceof Circle) {
                if (((Circle) node).getFill().equals(Paint.valueOf("BLACK"))) {
                    blackCount++;
                } else {
                    whiteCount++;
                }
            }
        }

        blackScoreLabelCount.setText(String.valueOf(blackCount));
        whiteScoreLabelCount.setText(String.valueOf(whiteCount));
    }

    private int calculateBoardCoordinate(double mousePosition) {
        return Math.floorDiv((int) Math.floor(mousePosition - OFFSET), CELL_SIZE) + 1;
    }

    private void playSound(String filename) {
        Media sound = new Media(new File("src/main/resources/sound/" + filename).toURI().toString());
        MediaPlayer player = new MediaPlayer(sound);
        mediaView.setMediaPlayer(player);
        player.play();
    }

    private void setupBackgroundMusic() {
        try {
            Media sound = new Media(new File("src/main/resources/sound/background.mp3").toURI().toString());
            bgMusicPlayer = new MediaPlayer(sound);
            bgMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgMusicPlayer.setVolume(0.05);
            bgMusicView.setMediaPlayer(bgMusicPlayer);
            if (playBG) bgMusicPlayer.play();
        } catch (Exception e) {
            logger.error("Could not load background music: {}", e.getMessage());
        }
    }

    public void exitAction() {
        if (multiplayer && gameStarted.get()) {
            JsonObject endMessage = new JsonObject()
                    .put("type", "gameEnd")
                    .put("winner", opponentId)
                    .put("loser", userData.getId());

            clientVerticle.sendWebSocketMessage(endMessage)
                    .onComplete(res -> exitToMenu());
        } else {
            exitToMenu();
        }
    }

    private void exitToMenu() {
        stopMedia();
        try {
            BaseController controller = (multiplayer) ?
                    (MenuController) start(new Stage(), userData, "menu") :
                    (MultSingController) start(new Stage(), null, "mult-sing");
            currentStage.close();
            logger.info("{}", controller == null);
        } catch (IOException e) {
            logger.error("Failed to navigate back: {}", e.getMessage());
        }
    }

    private void stopMedia() {
        if (bgMusicPlayer != null) bgMusicPlayer.stop();
    }

    public void toggleMusic() {
        playBG = !playBG;
        if (playBG) bgMusicPlayer.play();
        else bgMusicPlayer.pause();
    }

    @Override
    public void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.setGraphic(null);

            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image("file:src/main/resources/icons/question.png"));

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/dialog.css")).toExternalForm());

            alert.showAndWait();
        });
    }

    public void setMultiplayer(Boolean multiplayer, boolean isHost) {
        this.multiplayer = multiplayer;
        this.isHost = isHost;
    }

    public void setClientVerticle(ClientVerticle clientVerticle) {
        this.clientVerticle = clientVerticle;
    }
}