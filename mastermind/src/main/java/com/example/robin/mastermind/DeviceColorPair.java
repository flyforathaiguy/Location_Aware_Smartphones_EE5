package com.example.robin.mastermind;

import java.io.Serializable;

/**
 * Created by Jasper on 29/04/2016.
 */
public class DeviceColorPair implements Serializable {

    private String deviceAddress;
    private int color;

    //Comment voor Robin
    public DeviceColorPair(String deviceAddress, int color){
        this.deviceAddress = deviceAddress;
        this.color = color;
    }

    public void setDeviceAddress(String deviceAddress){
        this.deviceAddress = deviceAddress;
    }

    public void setColor(int color){
        this.color = color;
    }

    public String getDeviceAddress(){
        return this.deviceAddress;
    }

    public int getColor(){
        return this.color;
    }

}
