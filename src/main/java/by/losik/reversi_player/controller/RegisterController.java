package by.losik.reversi_player.controller;

import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.RegistrationVerticle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController extends BaseController implements IAlertable, ILaunchable {
    public TextField username;
    public TextField password;
    private final RegistrationVerticle registrationVerticle = new RegistrationVerticle();

    public void register() {
        if (username.getText().isEmpty() || password.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username and password are required");
            return;
        }

        registrationVerticle.setUserData(userData);
        registrationVerticle.getUserData().setPassword(password.getText());
        registrationVerticle.getUserData().setUsername(username.getText());

        registrationVerticle.registerUser()
                .onSuccess(registeredUser -> Platform.runLater(() -> {
                    userData = registeredUser;
                    showAlert(Alert.AlertType.CONFIRMATION, "Success",
                            String.format("Registration successful, %s!", registeredUser.getUsername()));
                    try {
                        currentStage.close();
                        start(new Stage(), userData, "login");
                    } catch (IOException e) {
                        logger.error("Failed to open login screen: {}", e.getMessage());
                    }
                }))
                .onFailure(err -> Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Error",
                                "Registration failed: " + extractErrorMessage(err))));
    }

    private String extractErrorMessage(Throwable err) {
        return err.getMessage() != null ? err.getMessage() : "Invalid credentials (username must be unique)";
    }
}