package by.losik.reversi_player.exception;

public class WrongPlacementException extends Exception{
    private final String message;
    public WrongPlacementException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
