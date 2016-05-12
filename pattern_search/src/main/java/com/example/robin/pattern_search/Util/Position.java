package com.example.robin.pattern_search.Util;

/**
 * Created by Jasper on 12/05/2016.
 */
public class Position {

    private int positionX, positionY;

    public Position(int posX, int posY){
        this.positionX = posX;
        this.positionY = posY;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int distanceTo(Position other){
        int distance = (int) Math.sqrt(Math.pow(this.positionX - other.getPositionX(), 2) + Math.pow(this.positionY - other.getPositionY(), 2));
        return distance;
    }
}
