package com.example.robin.pattern_search;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.GlobalResources;


public class GameWindow extends Activity {

    private Button button;
    private TextView feedbackText;

    private static final int END_GAME = 3;
    private static final int POS_CONFIRM = 2;

    //Bounds wherein the position can be randomly generated
    private static final int LOWER_BOUND_X = -90;
    private static final int UPPER_BOUND_X = 90;
    private static final int LOWER_BOUND_Y = -90;
    private static final int UPPER_BOUND_Y = 90;

    private List<DevicePositionPair> confirmedPairs;
    private List<DevicePositionPair> wantedPairs;

    private final int nbDevices = GlobalResources.getInstance().getDevices().size() + 1;

    final Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_window);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        GlobalResources.getInstance().setHandler(handler);

        //Set this context in GlobalResources
        GlobalResources.getInstance().setContext(getBaseContext());

        //Launch the pattern detector in a different runnable
        Runnable runnablePattern = new Runnable() {
            @Override
            public void run() {
                new RunPatternDetector(activity);
            }
        };
        runnablePattern.run();

        //Create the list
        confirmedPairs = new ArrayList<>();

        //Calculate the game
        calculateGame();

        //Get accept button
        button = (Button) findViewById(R.id.confirmButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmButton();
            }

            ;
        });

        //Get feedback text
        feedbackText = (TextView) findViewById(R.id.feedbackText);
    }

    //Handler for signals, large parts of code taken from original Location Aware Smartphones project
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Log.d(TAG, "Handler called");
            //Data packet incoming
            if(msg.what == DataHandler.DATA_TYPE_COORDINATES) {
                //If coordinates are received, the master has to handle these coordinates
                //and store them in a list

            }else if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
                // TODO: Wat als device disconnect?
                //Don't know yet, probalby end the game
                endGame();
            }
            else if(msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED){
                //updateText();
            }
            else if(msg.what == DataHandler.DATA_TYPE_DATA_PACKET) {
                //Datapacket = confirmation of the position of a phone
                //When one of the phones sends his confirmation, this means that this position for this phone is final
                //So it can be put in a list, to check later on
                Serializable data;
                if ((data = GlobalResources.getInstance().readData()) != null) {
                    handleData((DataPacket) data);
                }
            }
        }
    };

    private void handleData(DataPacket dataPacket){
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case POS_CONFIRM:
                //Make new DevicePositionPair containing the address of the device from which the position was sent, as well as the position
                //Get the device address from the phone that confirmed its position
                String device = GlobalResources.getInstance().getReceivedList().get(GlobalResources.getInstance().getReceivedList().size() - 1);
                DevicePositionPair pair = new DevicePositionPair(device, GlobalResources.getInstance().getDevices().get(device));
                confirmedPairs.add(pair);
                break;
            //case LAUNCH_WIN:
                //launchWinIntent();
            case END_GAME:
                endGame();
            default:
                break;
        }
        //Remove address at last index since we do not need it
        //Should be the only one in the list ( list.size() == 1 --> index 0)
        //Log.d(TAG, "received list size: " + GlobalResources.getInstance().getReceivedList().size());
        if(GlobalResources.getInstance().getReceivedList().size() > 0)
            GlobalResources.getInstance().getReceivedList().remove(GlobalResources.getInstance().getReceivedList().size() - 1);
    }

    private void calculateGame(){

        //Get all devices, current position of them does not matter
        HashMap<String, Position> connectedDevices = (HashMap) GlobalResources.getInstance().getDevices();
        List<String> devicesList = new ArrayList<>(connectedDevices.keySet());
        //Add own device to the list
        if(connectedDevices.containsKey("ownpos") == false)
            devicesList.add("ownpos");

        //Populate the wantedPairs list
        Random random = new Random();
        for(int i = 0; i < devicesList.size(); i++) {
            //Generate random int between upper bound and lower bound, both inclusive
            Position wantedPosition;
            int randomX = (random.nextInt(UPPER_BOUND_X - LOWER_BOUND_X) + UPPER_BOUND_X);
            int randomY = (random.nextInt(UPPER_BOUND_Y - LOWER_BOUND_Y) + UPPER_BOUND_Y);

            //Z value and angle at 0°, do not actually matter
            wantedPosition = new Position(randomX, randomY, 0, 0);
            //Create new DevicePositionPair and add it to the wantedPairs list
            DevicePositionPair pair = new DevicePositionPair(devicesList.get(i), wantedPosition);
            wantedPairs.add(pair);
        }

        //TODO: What else has to happen?
    }

    private void confirmButton() {

        button.setClickable(false);

        //In this case you're the client
        if(GlobalResources.getInstance().getClient() == true) {
            //Send data of type DATA_PACKET, DataPacket itself is of type COLOR
            Position position = GlobalResources.getInstance().getDevice().getPosition();
            GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(POS_CONFIRM));
        }

        //In this case you're the master
        else {
            //TODO: calculate the distance for each phone to the wanted position
            checkAllDevicesConfirmed();
        }

    }

    private void checkAllDevicesConfirmed(){

    }

    private void endGame(){
        //Send to all other devices the information to end the game
        if(GlobalResources.getInstance().getClient() == false){
            for(Map.Entry<String, Position> entry : GlobalResources.getInstance().getDevices().entrySet()){
                GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(END_GAME));
            }
        }

        //Show toast saying that a device has disconnected and ending the game
        Toast toast = Toast.makeText(this, "A device has disconnected, ending the game!", Toast.LENGTH_LONG);
        toast.show();

        PatternDetector patternDetector = GlobalResources.getInstance().getPatternDetector();
        if(patternDetector != null)
            patternDetector.destroy();

        GlobalResources.getInstance().setContext(null);
        finish();
    }

}
