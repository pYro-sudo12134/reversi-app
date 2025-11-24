package by.losik.reversi_player.helper;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public interface IAlertable {
    default void warn(String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("oops!");
        alert.setHeaderText("Alert!");
        alert.setContentText(message);
        alert.setGraphic(null);

        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);

        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("file:src/main/resources/icons/question.png"));

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dialog.css")).toExternalForm());

        alert.showAndWait();
    }

    default void exit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit?");
        alert.setTitle("Exit");
        alert.setHeaderText("Exiting app");
        alert.setGraphic(null);

        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("file:src/main/resources/icons/question.png"));

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dialog.css")).toExternalForm());

        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Node button = dialogPane.lookupButton(buttonType);
            button.getStyleClass().add("custom-alert-button");
            button.setStyle("""
                    -fx-font-family: 'Cascadia Mono';
                    -fx-font-size: 14px;
                    -fx-background-color: #333;
                    -fx-background-insets: 0, 1;
                    -fx-background-radius: 5;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-padding: 8 15;
                    -fx-border-color: #808080 #707070 #606060 #707070;
                    -fx-border-width: 1;
                    -fx-border-radius: 5;""");
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.exit(0);
            }
        });
    }
}
