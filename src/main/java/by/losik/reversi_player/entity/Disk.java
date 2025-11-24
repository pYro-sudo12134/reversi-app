package by.losik.reversi_player.entity;

import by.losik.reversi_player.exception.WrongPlacementException;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class Disk {
    private final Circle circle = new Circle();
    private final int row;
    private final int column;

    public Disk(boolean black, int row, int column) throws WrongPlacementException {
        this.circle.setRadius(15);
        this.circle.setStroke(Paint.valueOf("BLACK"));
        this.circle.setFill(Paint.valueOf(black ? "BLACK" : "WHITE"));
        if(row > 8 || row < 1){
            throw new WrongPlacementException("Incorrect column placement");
        }
        else{
            this.row = row;
        }
        if(column > 8 || column < 1){
            throw new WrongPlacementException("Incorrect row placement");
        }
        else{
            this.column = column;
        }
    }

    public Disk flip() throws WrongPlacementException {
        return new Disk(!circle.getFill().equals(Paint.valueOf("BLACK")), row, column);
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public Circle getCircle(){
        return this.circle;
    }

    public boolean equals(Disk disk) {
        return disk.getRow() == row && disk.getColumn() == column && disk.getCircle().getFill().equals(circle.getFill());
    }

    public boolean isBlack() {
        return circle.getFill().equals(Paint.valueOf("BLACK"));
    }
}
