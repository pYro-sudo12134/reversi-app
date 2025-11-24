package by.losik.reversi_player.entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyData {
    private String lobbyId;
    private List<String> players = new CopyOnWriteArrayList<>();
    private boolean isAlive;

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }
}
