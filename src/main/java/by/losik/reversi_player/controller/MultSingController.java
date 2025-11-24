package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.UserData;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class MultSingController extends BaseController implements IAlertable, ILaunchable {
    public void local() {
        try {
            GameController gameController = (GameController) start(new Stage(), null, "reversi-map");
            gameController.setMultiplayer(false, true);
            logger.info("Singleplayer has been chosen");
            currentStage.close();
        } catch (Exception exception) {
            logger.error("Failed to start singleplayer game: ", exception);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to start singleplayer game: " + exception.getMessage());
        }
    }

    public void multiplayer() {
        try {
            start(new Stage(), new UserData(), "login");
            currentStage.close();
            logger.info("Multiplayer has been chosen");
        } catch (IOException ioException) {
            logger.error("Failed to start multiplayer session", ioException);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to start multiplayer: " + ioException.getMessage());
        }
    }
}