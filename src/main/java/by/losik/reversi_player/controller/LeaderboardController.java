package by.losik.reversi_player.controller;

import by.losik.reversi_player.entity.UserData;
import by.losik.reversi_player.helper.IAlertable;
import by.losik.reversi_player.helper.ILaunchable;
import by.losik.reversi_player.verticles.LeaderboardVerticle;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class LeaderboardController extends BaseController implements IAlertable, ILaunchable {
    public TableColumn<UserData, Double> winRate;
    public TableColumn<UserData, Long> winsAmount;
    public TableColumn<UserData, String> username;
    public TableView<UserData> userWins;
    private final LeaderboardVerticle leaderboardVerticle = new LeaderboardVerticle();

    public void returnToMenu() {
        try {
            currentStage.close();
            leaderboardVerticle.stop();
            logger.info("Returned to menu");
        } catch (Exception e) {
            logger.error("Exception occurred: {}", e.getMessage());
        }
    }

    public void refresh() {
        getAllUsers();
    }

    public void getAllUsers() {
        leaderboardVerticle.setUserData(userData);
        leaderboardVerticle.fetchAllUsers()
                .onSuccess(userDataList -> Platform.runLater(() -> {
                    userWins.setItems(FXCollections.observableList(userDataList));
                    username.setCellValueFactory(x -> new SimpleStringProperty(x.getValue().getUsername()));
                    winsAmount.setCellValueFactory(x -> new SimpleLongProperty(x.getValue().getWins()).asObject());
                    winRate.setCellValueFactory(x ->
                            new SimpleDoubleProperty(x.getValue().getLosses() == 0 ? 1 :
                                    x.getValue().getWins().doubleValue() /
                                            x.getValue().getLosses().doubleValue()).asObject());
                }))
                .onFailure(err -> Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to load leaderboard: " + err.getMessage())));
    }
}