package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.LobbyData;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.ClientVerticle;
import by.losik.reversi_player.verticles.SessionListVerticle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class SessionListController extends BaseController implements IAlertable, ILaunchable {
    public TableView<LobbyData> sessions;
    public TableColumn<LobbyData, String> id;
    private final SessionListVerticle sessionListVerticle = new SessionListVerticle();
    private LobbyData selectedLobby;
    private final ClientVerticle clientVerticle = new ClientVerticle();

    public void initialize() {
        sessions.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedLobby = newValue;
                        joinLobby(selectedLobby);
                    }
                });
    }

    private void joinLobby(LobbyData lobby) {
            clientVerticle.start();

            clientVerticle.connectToChild(lobby.getLobbyId())
                    .onSuccess(succ -> Platform.runLater(() -> {
                        try {
                        logger.info("Selected Lobby ID: {}", lobby.getLobbyId());
                        GameController gameController = (GameController) start(new Stage(), userData, "reversi-map");
                        gameController.setClientVerticle(clientVerticle);
                        clientVerticle.setConnectedChildId(lobby.getLobbyId());
                        gameController.setMultiplayer(true, false);

                        currentStage.close();
                        } catch (Exception e) {
                            logger.error("Failed to join lobby: {}", e.getMessage());
                            Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to join lobby: " + e.getMessage()));
                        }
                    }))
                    .onFailure(fail -> Platform.runLater(() -> {
                        logger.error("Exception occurred: " + fail.getMessage());
                        showAlert(Alert.AlertType.ERROR, "Error", fail.getMessage());

                    }));
    }

    public void returnToMenu() {
        try {
            currentStage.close();
            sessionListVerticle.stop();
            start(new Stage(), userData, "menu");
            logger.info("Returned to menu");
        } catch (Exception e) {
            logger.error("Failed to return to menu: {}", e.getMessage());
        }
    }

    public void refresh() {
        getAllLobbies();
    }

    public void getAllLobbies() {
        sessionListVerticle.setUserData(userData);
        sessionListVerticle.fetchAllLobbies()
                .onSuccess(lobbies -> Platform.runLater(() -> {
                    sessions.setItems(FXCollections.observableList(lobbies));
                    id.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().getLobbyId()));
                }))
                .onFailure(err -> Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to fetch lobbies: " + err.getMessage())));
    }
}