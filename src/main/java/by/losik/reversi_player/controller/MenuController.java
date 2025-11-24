package by.losik.reversi_player.controller;


import by.losik.reversi_player.entity.UserData;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.ClientVerticle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.Desktop;

import java.io.IOException;
import java.net.URI;

public class MenuController extends BaseController implements IAlertable, ILaunchable {
    public Hyperlink hyperlinkRule;
    public Button CreateGame;
    public Button FindGame;
    public Button Exit;
    public Button Logout;
    public Button Leaderboard;
    public Label nickname;
    private final ClientVerticle clientVerticle = new ClientVerticle();

    public void initialize() {
        Exit.setOnAction(event -> exit());
    }
    public void redirectToRules() {
        hyperlinkRule.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://brainking.com/ru/GameRules?tp=10"));
                }
                logger.info("Redirected.");
            } catch (Exception exception) {
                logger.error("Exception occurred: {}", exception.getMessage());
            }
        });
    }

    public void createGame() {
        clientVerticle.start();
        clientVerticle.createChild()
                .onSuccess(succ -> Platform.runLater(() -> {
                        try {
                            GameController gameController = (GameController) start(new Stage(), userData, "reversi-map");
                            gameController.setClientVerticle(clientVerticle);
                            gameController.setMultiplayer(true, true);
                            currentStage.close();
                        }
                        catch (Exception e){
                            logger.error("Exception occurred: {}", e.getMessage());
                            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                        }
                    }))
                .onFailure(fail -> Platform.runLater(() -> {
                    logger.error("Exception occurred: {}", fail.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", fail.getMessage());
                }));
    }

    public void findGame() {
        try{
            start(new Stage(), userData, "session-list");
            currentStage.close();
        }
        catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    public void logout() {
        try{
            start(new Stage(), null, "mult-sing");
            currentStage.close();
            logger.info("Logout successful");
        }
        catch (IOException ioException){
            logger.error("Exception occurred: {}", ioException.getMessage());
        }
    }

    public void showLeaderboard() {
        try{
            start(new Stage(), userData, "leaderboard");
            logger.info("Leaderboard shown.");
        }
        catch (IOException ioException){
            logger.error("Exception occurred: {}", ioException.getMessage());
        }
    }

    @Override
    public void setUserData(UserData userData) {
        this.nickname.setText(userData.getUsername());
        this.userData = userData;
    }
}
