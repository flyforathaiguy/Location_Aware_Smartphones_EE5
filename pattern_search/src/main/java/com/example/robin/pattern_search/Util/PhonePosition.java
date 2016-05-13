package com.example.robin.pattern_search.Util;

/**
 * Created by Jasper on 12/05/2016.
 */
public class PhonePosition {

    private double positionX, positionY;

    public PhonePosition(double posX, double posY){
        this.positionX = posX;
        this.positionY = posY;
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double distanceTo(PhonePosition other){
        double distance = Math.sqrt(Math.pow(this.positionX - other.getPositionX(), 2) + Math.pow(this.positionY - other.getPositionY(), 2));
        return distance;
    }
}
