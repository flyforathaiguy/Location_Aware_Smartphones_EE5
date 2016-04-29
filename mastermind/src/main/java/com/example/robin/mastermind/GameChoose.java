package com.example.robin.mastermind;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.math.DistanceCalculation;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.DeviceColorPair;
import be.groept.emedialab.util.GlobalResources;

public class GameChoose extends ActionBarActivity {

    private FrameLayout frame;
    private int color;
    private Button button;
    private ImageView imageViewRED, imageViewBLUE, imageViewGREEN, imageViewYELLOW;
    private String TAG = "GameChoose";

    //All color values for sending to other device
    private static final int COLOR = 0;
    private static final int COLOR_BLUE = Color.BLUE;
    private static final int COLOR_RED = Color.RED;
    private static final int COLOR_YELLOW = Color.YELLOW;
    private static final int COLOR_GREEN = Color.GREEN;

    //Lists that contains the devices with their gamePosition and gameColor
    Map<String, Integer> deviceSequence = new HashMap<>();
    HashMap<String, Integer> deviceColors = new HashMap<>();
    List<DeviceColorPair> confirmedPairs = new ArrayList<>();

    //Integers for random variable calculation
    int low = 0;
    int high = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_choose);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        GlobalResources.getInstance().setHandler(handler);

        //Set this context in GlobalResources
        GlobalResources.getInstance().setContext(getBaseContext());

        //Launch the pattern detector in a different runnable
        final Activity activity = this;
        Runnable runnablePattern = new Runnable() {
            @Override
            public void run() {
                new RunPatternDetector(activity);
            }
        };
        runnablePattern.run();

        //Set click listeners for the four different colours
        setClickListeners();

        //Only the master has to do this
        if(GlobalResources.getInstance().getClient() == false) {
            //Calculate the lists for the game
            calculateGame();
        }
    }

    private void calculateGame(){

        //Get all devices, current position of them does not matter
        HashMap<String, Position> connectedDevices = (HashMap) GlobalResources.getInstance().getDevices();
        List<String> devicesList = new ArrayList<>(connectedDevices.keySet());
        //Add own device to the list
        devicesList.add("ownpos");
        int[] randoms = new int[high + 1];
        boolean contains = false;

        //Fill the array with out of range numbers
        for(int i = 0; i <= high; i++){
            randoms[i] = 20;
        }
        //Assign a random position (0 to 3, the sequence in which they have to be)
        int count = 0;
        int color = 0;
        int randomInt;
        Random random = new Random();
        while(true){
            //Generate random int between 0 and 3, both inclusive
             randomInt = (random.nextInt(high - low + 1) + low);
            //Check if this random variable already exists in the randoms array
            for(int i = 0; i <= high; i++){
                if(randoms[i] == randomInt){
                    contains = true;
                    break;
                }
            }
            //If it did not already contain --> Add to deviceSequence and to randoms array
            if(contains == false){
                randoms[count] = randomInt;
                deviceSequence.put(devicesList.get(count), randomInt);
                //Generate random int between 0 and 3, both inclusive
                randomInt = (int) (random.nextInt(high - low + 1) + low);
                switch(randomInt){
                    //Take 0 for red
                    case 0: color = Color.RED;
                        break;
                    //Take 1 for blue
                    case 1: color = Color.BLUE;
                        break;
                    //Take 2 for green
                    case 2: color = Color.GREEN;
                        break;
                    //Take 3 for yellow
                    case 3: color = Color.YELLOW;
                        break;
                }
                //Put device and color in the deviceColors list
                deviceColors.put(devicesList.get(count), color);
            }

            count++;
            //Check if the the entire list is populated
            if(count > 3)
                break;
        }

        //Sort the list
        deviceSequence = sortByValue(deviceSequence);
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
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptButton();
            }

            ;
        });
    }

    private void updateColor(int color){
        //Update the color in the background of the screen, as well as the variable in this class
        switch(color){
            case Color.RED:
                frame.setBackgroundColor(Color.RED);
                this.color = Color.RED;
                break;
            case Color.BLUE:
                frame.setBackgroundColor(Color.BLUE);
                this.color = Color.BLUE;
                break;
            case Color.GREEN:
                frame.setBackgroundColor(Color.GREEN);
                this.color = Color.GREEN;
                break;
            case Color.YELLOW:
                frame.setBackgroundColor(Color.YELLOW);
                this.color = Color.YELLOW;
                break;
            default: break;
        }
    }

    private void acceptButton(){

        //Disable all buttons
        button.setClickable(false);
        imageViewRED.setClickable(false);
        imageViewBLUE.setClickable(false);
        imageViewGREEN.setClickable(false);
        imageViewYELLOW.setClickable(false);

        //If you're a client, send your color to the master
        if(GlobalResources.getInstance().getClient() == true) {
            //Send data of type DATA_PACKET, DataPacket itself is of type COLOR
            GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(COLOR, color));
        }

        //If you're the master, check if all colors are in, calculate the correct positions & colors
        else{
            //Add your own pair to the list
            //ownpos is filled in rather than the device address because as master you do not need the devices address
            DeviceColorPair pair = new DeviceColorPair("ownpos", color);
            confirmedPairs.add(pair);
            checkAllColorsIn();
        }
    }

    //Handler for signals, large parts of code taken from original Location Aware Smartphones project
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
                /*
                BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                Log.e(TAG, "Device " + bluetoothDevice.getAddress() + " disconnected!");
                sortedValues.remove(bluetoothDevice.getAddress());
                if(sortedValues.size() <= SecondActivity.minNumberOfDevices){
                    leaveGame(mContentView);
                }
                */
            }
        }
    };

    private void checkAllColorsIn(){

        int correctPairs = 0, correctPositions = 0, correctColors = 0;

        //Check if all colors are in
        if( confirmedPairs.size() != high + 1)
            return;

        //Check how many full correct pairs there are (meaning position + color are correct)
        Map<String, Position> actualPositions = GlobalResources.getInstance().getDevices();
        //Add own position to the Map
        actualPositions.put("ownpos", GlobalResources.getInstance().getDevice().getPosition());
        LinkedHashMap<String, Point> line = DistanceCalculation.getLine(actualPositions);

        //If not all devices know their position, re-enable button on master phone to try again later and show this with a toast message
        if(line.size() != actualPositions.size()){
            button.setClickable(true);
            Toast toast = Toast.makeText(this, "Not all devices know their position!", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        List<String> lineOrder = new ArrayList<>(line.keySet());

        //TODO: Check if all devices have position, not NaN
        //Is line incrementing or decrementing order (suppose for now left -> right)?
        for(int i = 0; i <= high; i++){

            if(lineOrder.get(i).equals( getKeyByValue(deviceSequence, i) )){
                //TODO: Check if colors also match
            }



        }
    }

    private void handleData(DataPacket dataPacket){
        Log.d(TAG, "Reading datapacket of type " + dataPacket.getDataType());
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case COLOR:
                //Make new DeviceColorPair containing the address of the device from which the color was sent, as well as the color
                DeviceColorPair pair = new DeviceColorPair(GlobalResources.getInstance().getReceivedList().get(GlobalResources.getInstance().getReceivedList().size() - 1), (int)dataPacket.getOptionalData());
                confirmedPairs.add(pair);
                checkAllColorsIn();
                break;
            default:
                break;
        }
        //Remove address at last index since we do not need it
        //Should be the only one in the list ( list.size() == 1 --> index 0)
        GlobalResources.getInstance().getReceivedList().remove(GlobalResources.getInstance().getReceivedList().size() - 1);
    }

    //Generic sorting function
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map )
    {
        List<HashMap.Entry<K, V>> list =
                new LinkedList<HashMap.Entry<K, V>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        HashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    //Generic function to get Key by Value in Map
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void onPause(){
        super.onPause();
        //If this Activity is paused due to the Calibration being launched, do not destroy pattern detector!
        if(GlobalResources.getInstance().getPatternDetector() != null) {
            if(GlobalResources.getInstance().getCalibrated() == true) {
                GlobalResources.getInstance().getPatternDetector().destroy();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(GlobalResources.getInstance().getPatternDetector() != null && GlobalResources.getInstance().getPatternDetector().isPaused())
            GlobalResources.getInstance().getPatternDetector().setup();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PatternDetector patternDetector = GlobalResources.getInstance().getPatternDetector();
        if(patternDetector != null)
            patternDetector.destroy();
    }
}
