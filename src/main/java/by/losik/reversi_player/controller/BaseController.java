package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.UserData;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    UserData userData;
    protected Stage currentStage;
    public void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.setGraphic(null);

            var stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image("file:src/main/resources/icons/question.png"));

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dialog.css")).toExternalForm());

            alert.showAndWait();
        });
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }
}
