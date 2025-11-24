package by.losik.reversi_player;

import by.losik.reversi_player.controller.MultSingController;
import by.losik.reversi_player.entity.UserData;
import by.losik.reversi_player.helper.IAlertable;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ReversiApplication extends Application implements IAlertable {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ReversiApplication.class.getResource("mult-sing.fxml"));
        stage.getIcons().add(new Image("file:src/main/resources/icons/othello.png"));
        stage.setResizable(false);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setMinWidth(300);
        stage.setMinHeight(200);
        MultSingController multSingController = fxmlLoader.getController();
        multSingController.setCurrentStage(stage);
        multSingController.setUserData(new UserData());
        stage.setTitle("Reversi session");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(windowEvent -> {
            exit();
            windowEvent.consume();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}