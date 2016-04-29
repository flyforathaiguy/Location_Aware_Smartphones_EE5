package com.example.robin.mastermind;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.GlobalResources;

public class GameChoose extends ActionBarActivity {

    private FrameLayout frame;
    private int color;
    private Button button;
    private ImageView imageViewRED, imageViewBLUE, imageViewGREEN, imageViewYELLOW;
    private String TAG = "GameChoose";

    //All color values for sending to other device
    private static final int COLOR_BLUE = Color.BLUE;
    private static final int COLOR_RED = Color.RED;
    private static final int COLOR_YELLOW = Color.YELLOW;
    private static final int COLOR_GREEN = Color.GREEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_choose);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Launch the pattern detector in a different runnable
        final Activity activity = this;
        Runnable runnablePattern = new Runnable() {
            @Override
            public void run() {
                new RunPatternDetector(activity);
            }
        };
        runnablePattern.run();
        GlobalResources.getInstance().setHandler(handler);

        //Set click listeners for the four different colours
        setClickListeners();
    }

    private void calculateGame(){

        //Get all devices, current position of them does not matter
        HashMap<String, Position> connectedDevices = (HashMap) GlobalResources.getInstance().getDevices();
        HasmMap<String, Integer> deviceSequence = new HashMap<String, Integer>();

        //Assign a random position
        while(true){



        }
        ArrayList<Integer> newNumbers = populateList(connectedDevices.size());

        while(newNumbers.equals(oldNumbers))
            newNumbers = populateList(connectedDevices.size());
        oldNumbers = newNumbers;

        int i = 0;
        for(String key: connectedDevices.keySet()){
            if(!levelUp) {
                GlobalResources.getInstance().sendData(key, new DataPacket(TYPE_DISPLAY, newNumbers.get(i)));
            }else{
                GlobalResources.getInstance().sendData(key, new DataPacket(TYPE_LEVEL_UP, newNumbers.get(i)));
            }
            numbersToPhone.put(key, newNumbers.get(i));
            Log.d(TAG, "Number " + newNumbers.get(i) + " sent to " + key);
            i++;
        }
        if(levelUp){
            levelUp(newNumbers.get(i));
        }else{
            displayImage(newNumbers.get(i));
        }
        numbersToPhone.put("ownpos", newNumbers.get(i));

        sortedValues = sortMapByValues(numbersToPhone);
    }

    private void setClickListeners(){
        //All click listeners for the colour imageViews
        //For the Red Color
        imageViewRED = (ImageView) findViewById(R.id.imageViewRED);
        imageViewRED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.RED);
            };
        });

        //For the Blue Color
        imageViewBLUE = (ImageView) findViewById(R.id.imageViewBLUE);
        imageViewBLUE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.BLUE);
            };
        });

        //For the Green Color
        imageViewGREEN = (ImageView) findViewById(R.id.imageViewBLUE);
        imageViewGREEN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.GREEN);
            };
        });

        //For the Yellow Color
        imageViewYELLOW = (ImageView) findViewById(R.id.imageViewBLUE);
        imageViewYELLOW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.YELLOW);
            };
        });

        //Get background frame
        frame = (FrameLayout) findViewById(R.id.Frame);

        //Get accept button
        button = (Button) findViewById(R.id.acceptButton);
    }

    private void updateColor(int color){
        //Update the color in the background of the screen, as well as the variable in this class
        switch(color){
            case Color.RED:
                frame.setBackgroundColor(Color.RED);
                color = Color.RED;
                break;
            case Color.BLUE:
                frame.setBackgroundColor(Color.BLUE);
                color = Color.BLUE;
                break;
            case Color.GREEN:
                frame.setBackgroundColor(Color.GREEN);
                color = Color.GREEN;
                break;
            case Color.YELLOW:
                frame.setBackgroundColor(Color.YELLOW);
                color = Color.YELLOW;
                break;
            default: break;
        }
    }

    private void acceptButton(View v){

        //Disable all buttons
        button.setClickable(false);
        imageViewRED.setClickable(false);
        imageViewBLUE.setClickable(false);
        imageViewGREEN.setClickable(false);
        imageViewYELLOW.setClickable(false);

        //TODO: als ge de client zijt, verstuur uw kleur naar de master

        //TODO: als ge master zijt en alle kleuren zijn binnen, check voor correctheid van kleur & positie en geef signaal naar elke gsm om feedback / win te launchen, launch ook zelf feedback of win
    }


    //Handler for signales, large parts of code taken from orignal Location Aware Smartphones project
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "Handler called");
            //Data packet incoming
            if(msg.what == DataHandler.DATA_TYPE_DATA_PACKET){
                //Datapacket = the color that one of the phones has
                //When one of the phones sends his color, this means that this color for this phone is final
                //So it can be put in a list, to check later on for correct position & color
                Serializable data;
                if((data = GlobalResources.getInstance().readData()) != null){
                    handleData((DataPacket) data);
                }
            }else if(msg.what == DataHandler.DATA_TYPE_COORDINATES) {
                //If coordinates are received, nothing special has to be done,
                //Only when all devices have sent their color, the coordinates will be used, these
                //will be asked from
            }else if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
                  //Don't know yet, probalby end the game
                BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                Log.e(TAG, "Device " + bluetoothDevice.getAddress() + " disconnected!");
                sortedValues.remove(bluetoothDevice.getAddress());
                if(sortedValues.size() <= SecondActivity.minNumberOfDevices){
                    leaveGame(mContentView);
                }
            }
        }
    };

    private void handleData(DataPacket dataPacket){
        Log.d(TAG, "Reading datapacket of type " + dataPacket.getDataType());
        switch(dataPacket.getDataType()){
            case COLOR_RED:
                break;
            case COLOR_BLUE:
                break;
            case COLOR_GREEN:
                break;
            case COLOR_YELLOW:
                break;
        }
    }
}
