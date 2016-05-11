package com.example.robin.pattern_search;

import java.io.Serializable;

import be.groept.emedialab.server.data.Position;

/**
 * Created by Jasper on 29/04/2016.
 */
public class DevicePositionPair implements Serializable {

    private String deviceAddress;
    private Position position;

    public DevicePositionPair(String deviceAddress, Position position){
        this.deviceAddress = deviceAddress;
        this.position = position;
    }

    public void setDeviceAddress(String deviceAddress){
        this.deviceAddress = deviceAddress;
    }

    public void setPosition(Position position){
        this.position = position;
    }

    public String getDeviceAddress(){
        return this.deviceAddress;
    }

    public Position getPosition(){
        return this.position;
    }

}
