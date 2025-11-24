package by.losik.reversi_player.helper;

import by.losik.reversi_player.ReversiApplication;
import by.losik.reversi_player.controller.BaseController;
import by.losik.reversi_player.entity.UserData;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public interface ILaunchable extends IAlertable {
    default BaseController start(Stage stage, UserData userData, String name) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ReversiApplication.class.getResource(name+".fxml"));
        stage.getIcons().add(new Image("file:src/main/resources/icons/othello.png"));
        stage.setResizable(false);
        Scene scene = new Scene(fxmlLoader.load());
        BaseController controller = fxmlLoader.getController();
        controller.setUserData(userData);
        controller.setCurrentStage(stage);
        stage.setTitle("Reversi session");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(windowEvent -> {
            exit();
            windowEvent.consume();
        });

        return controller;
    }
}
