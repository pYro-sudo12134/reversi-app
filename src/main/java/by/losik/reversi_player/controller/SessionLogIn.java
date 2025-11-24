package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.UserData;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.LoginVerticle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SessionLogIn extends BaseController implements IAlertable, ILaunchable {
    public Button signIn;
    public Button signUp;
    public TextField username;
    public TextField password;
    private final LoginVerticle loginVerticle = new LoginVerticle();
    private UserData userData;

    public void signIn() {
        if (username.getText().isEmpty() || password.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username and password are required");
            return;
        }
        loginVerticle.setUserData(userData);
        loginVerticle.getUserData().setPassword(password.getText());
        loginVerticle.getUserData().setUsername(username.getText());

        loginVerticle.login()
                .onSuccess(v -> Platform.runLater(() -> {
                    try {
                        showAlert(Alert.AlertType.INFORMATION, "Success", String.format("Welcome, %s!", loginVerticle.getUserData().getUsername()));
                        MenuController menuController = (MenuController) start(new Stage(), userData, "menu");
                        menuController.setUserData(userData);
                        currentStage.close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                }))
                .onFailure(err -> Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Invalid credentials")));
    }

    public void signUp() {
        showAlert(Alert.AlertType.INFORMATION, "Sign Up", "Redirecting to sign up page");
        try {
            currentStage.close();
            start(new Stage(), userData, "register");
        } catch (IOException ioException) {
            logger.error("Exception occurred: {}", ioException.getMessage());
        }
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }
}
