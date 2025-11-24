package by.losik.reversi_player.entity;

public class UserData {
    private String id;
    private String username;
    private String password;
    private Long wins;
    private Long losses;
    private String bearer;
    private String role;

    public UserData() {
    }

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public Long getWins() {
        return this.wins;
    }

    public Long getLosses() {
        return this.losses;
    }

    public String getBearer() {
        return this.bearer;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setWins(Long wins) {
        this.wins = wins;
    }

    public void setLosses(Long losses) {
        this.losses = losses;
    }

    public void setBearer(String bearer) {
        this.bearer = bearer;
    }

    public String toString() {
        return "UserData(id=" + this.getId() +
                ", username=" + this.getUsername() +
                ", password=" + this.getPassword() +
                ", wins=" + this.getWins() +
                ", losses=" + this.getLosses() +
                ", bearer=" + this.getBearer() +
                ", role=" + this.getRole()+ ")";
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
