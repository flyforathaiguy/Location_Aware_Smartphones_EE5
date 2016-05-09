package com.example.robin.mastermind;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.Random;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.math.DistanceCalculation;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.DeviceColorPair;
import be.groept.emedialab.util.GlobalResources;

public class GameChoose extends Activity {

    private FrameLayout frame;
    private int ownColor = 0;
    private Button button;
    private ImageView imageViewRED, imageViewBLUE, imageViewGREEN, imageViewYELLOW;
    private TextView feedbackText;
    private String TAG = "GameChoose";

    //All ownColor values for sending to other device
    private static final int COLOR = 0;
    private static final int LAUNCH_FEEDBACK = 1;
    private static final int LAUNCH_WIN = 2;
    private static final int END_GAME = 3;
    public static final int ALL_CORRECT = 0;
    public static final int CORRECT_COLOR = 1;
    public static final int CORRECT_POS = 2;
    public static final int ALL_WRONG = 3;

    final Activity activity = this;

    //boolean to check whether or not feedback has started (for the onPause and onResume methods)
    private boolean launchedFeedback = false;

    //Lists that contains the devices with their gamePosition and gameColor
    Map<String, Integer> deviceSequence = new HashMap<>();
    HashMap<String, Integer> deviceColors = new HashMap<>();
    List<DeviceColorPair> confirmedPairs = new ArrayList<>();

    //Integers for random variable calculation
    int low = 0;
    //High = number of connected devices (clients) + 1 (master)
    int high = GlobalResources.getInstance().getDevices().size() + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_choose);
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

        //Set click listeners for the four different colours
        setClickListeners();
        Log.d(TAG, "clickListeners set");

        //Only the master has to do this
        if(GlobalResources.getInstance().getClient() == false) {
            //Calculate the lists for the game
            calculateGame();
        }
        Log.d(TAG, "Calculated game");
        Log.d(TAG, "DeviceSequence:");
        Log.d(TAG, deviceSequence.toString());
        Log.d(TAG, "DeviceColors:");
        Log.d(TAG, deviceColors.toString());
    }

    private void calculateGame(){

        //Get all devices, current position of them does not matter
        HashMap<String, Position> connectedDevices = (HashMap) GlobalResources.getInstance().getDevices();
        List<String> devicesList = new ArrayList<>(connectedDevices.keySet());
        //Add own device to the list
        devicesList.add("ownpos");
        int[] randoms = new int[high];
        boolean contains = false;

        //Fill the array with out of range numbers
        for(int i = 0; i < high; i++){
            randoms[i] = 20;
        }
        //Assign a random position (low to high, the sequence in which they have to be)
        int count = 0;
        int color = 0;
        int randomInt;
        Random random = new Random();
        while(true){
            //Generate random int between 0 and 3, (low and high) both inclusive
             randomInt = (random.nextInt(high - low ) + low);
            //Check if this random variable already exists in the randoms array
            for(int i = 0; i < high; i++){
                if(randoms[i] == randomInt){
                    contains = true;
                    break;
                }
            }
            //If it did not already contain --> Add to deviceSequence and to randoms array
            if(contains == false){
                randoms[count] = randomInt;
                deviceSequence.put(devicesList.get(count), randomInt);
                //Generate random int between low and hugh, both inclusive, this time for color
                //Does not check to see if this color already exists since the same color can occur multiple times
                randomInt = (random.nextInt(high - low + 1) + low);
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
                //Put device and ownColor in the deviceColors list
                deviceColors.put(devicesList.get(count), color);
                count++;
            }
            contains = false;

            //Check if the the entire list is populated
            if(count >= high)
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
        imageViewGREEN = (ImageView) findViewById(R.id.imageViewGREEN);
        imageViewGREEN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.GREEN);
            };
        });

        //For the Yellow Color
        imageViewYELLOW = (ImageView) findViewById(R.id.imageViewYELLOW);
        imageViewYELLOW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateColor(Color.YELLOW);
            };
        });

        //Get background frame
        frame = (FrameLayout) findViewById(R.id.Frame);

        //Get feedback text
        feedbackText = (TextView) findViewById(R.id.positionText);

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
        //Update the ownColor in the background of the screen, as well as the variable in this class
        switch(color){
            case Color.RED:
                frame.setBackgroundColor(Color.RED);
                this.ownColor = Color.RED;
                break;
            case Color.BLUE:
                frame.setBackgroundColor(Color.BLUE);
                this.ownColor = Color.BLUE;
                break;
            case Color.GREEN:
                frame.setBackgroundColor(Color.GREEN);
                this.ownColor = Color.GREEN;
                break;
            case Color.YELLOW:
                frame.setBackgroundColor(Color.YELLOW);
                this.ownColor = Color.YELLOW;
                break;
            default: break;
        }
    }

    private void updateText(){

        Position position = GlobalResources.getInstance().getDevice().getPosition();
        if(position.getFoundPattern()){
            feedbackText.setTextColor(Color.parseColor("green"));
            feedbackText.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(be.groept.emedialab.R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        }else{
            feedbackText.setTextColor(Color.parseColor("red"));
        }
    }

    private void acceptButton(){

        //Only accept when the user has actually chosen a color
        if(ownColor == 0)
            return;

        //Disable all buttons
        button.setClickable(false);
        imageViewRED.setClickable(false);
        imageViewBLUE.setClickable(false);
        imageViewGREEN.setClickable(false);
        imageViewYELLOW.setClickable(false);

        //If you're a client, send your ownColor to the master
        if(GlobalResources.getInstance().getClient() == true) {
            //Send data of type DATA_PACKET, DataPacket itself is of type COLOR
            GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(COLOR, ownColor));
        }

        //If you're the master, check if all colors are in, calculate the correct positions & colors
        else{
            //Add your own pair to the list
            //ownpos is filled in rather than the device address because as master you do not need the devices address
            //Perhaps this is executed after re-enabling the button when not all devices knew their position --> Do not add own color again
            boolean addPair = true;
            for(DeviceColorPair pair : confirmedPairs){
                if(pair.getDeviceAddress().equals("ownpos")){
                    addPair = false;
                    break;
                }
            }
            if(addPair){
                DeviceColorPair pair = new DeviceColorPair("ownpos", ownColor);
                confirmedPairs.add(pair);
            }
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
                //Datapacket = the ownColor that one of the phones has
                //When one of the phones sends his ownColor, this means that this ownColor for this phone is final
                //So it can be put in a list, to check later on for correct position & ownColor
                Serializable data;
                if((data = GlobalResources.getInstance().readData()) != null){
                    handleData((DataPacket) data);
                }
            }else if(msg.what == DataHandler.DATA_TYPE_COORDINATES) {
                //If coordinates are received, nothing special has to be done,
                //Only when all devices have sent their ownColor, the coordinates will be used, these
                //will be asked from
            }else if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
                // TODO: Wat als device disconnect?
                  //Don't know yet, probalby end the game
                endGame();
                /*
                BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                Log.e(TAG, "Device " + bluetoothDevice.getAddress() + " disconnected!");
                sortedValues.remove(bluetoothDevice.getAddress());
                if(sortedValues.size() <= SecondActivity.minNumberOfDevices){
                    leaveGame(mContentView);
                }
                */
            }
            else if(msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED){
                updateText();
            }
        }
    };

    private void handleData(DataPacket dataPacket){
        Log.d(TAG, "Reading datapacket of type " + dataPacket.getDataType());
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case COLOR:
                //Make new DeviceColorPair containing the address of the device from which the ownColor was sent, as well as the ownColor
                DeviceColorPair pair = new DeviceColorPair(GlobalResources.getInstance().getReceivedList().get(GlobalResources.getInstance().getReceivedList().size() - 1), (int)dataPacket.getOptionalData());
                confirmedPairs.add(pair);
                Log.d(TAG, "Color: " + dataPacket.getOptionalData());
                //Only master receives color from other devices
                checkAllColorsIn();
                break;
            case LAUNCH_FEEDBACK:
                launchFeedbackIntent((int) dataPacket.getOptionalData());
                break;
            case LAUNCH_WIN:
                launchWinIntent();
            case END_GAME:
                endGame();
            default:
                break;
        }
        //Remove address at last index since we do not need it
        //Should be the only one in the list ( list.size() == 1 --> index 0)
        Log.d(TAG, "received list size: " + GlobalResources.getInstance().getReceivedList().size());
        if(GlobalResources.getInstance().getReceivedList().size() > 0)
            GlobalResources.getInstance().getReceivedList().remove(GlobalResources.getInstance().getReceivedList().size() - 1);
    }

    private void checkAllColorsIn(){

        //Check if all colors are in
        if( confirmedPairs.size() != high){
            Toast toast = Toast.makeText(this, "Not all colors are in yet!", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        //Check how many full correct pairs there are (meaning position + ownColor are correct)
        Map<String, Position> actualPositions = GlobalResources.getInstance().getDevices();
        //Add own position to the Map
        actualPositions.put("ownpos", GlobalResources.getInstance().getDevice().getPosition());
        //Check if all devices know their position (not unknown with last position)
        for(Map.Entry<String, Position> object : actualPositions.entrySet()){
            Log.d(TAG, "pattern found: " + object.getValue().getFoundPattern());
            if(object.getValue().getFoundPattern() == false){
                button.setClickable(true);
                Toast toast = Toast.makeText(this, "Not all devices know their position!", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }
        LinkedHashMap<String, Point> line = DistanceCalculation.getLine(actualPositions);

        //If not all devices know their position, re-enable button on master phone to try again later and show this with a toast message
        Log.d(TAG, "Line size: " + line.size());
        Log.d(TAG, "actual size: " + actualPositions.size());
        if(line.size() != actualPositions.size()){
            button.setClickable(true);
            Toast toast = Toast.makeText(this, "Not all devices know their position!", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        List<String> lineOrder = new ArrayList<>(line.keySet());

        int fullMatches = 0, correctPosCount = 0, correctColorCount = 0;

        //Is line incrementing or decrementing order (suppose for now left -> right)?
        for(int i = 0; i < high; i++){

            //Get the key from value
            String key = getKeyByValue(deviceSequence, i);

            //Position matches the wanted position
            if(lineOrder.get(i).equals( key )){

                //Check if colors also match
                if (checkColorMatch(key)){
                    fullMatches++;
                }

                //Color did not match the wanted ownColor, only position was correct
                else{
                    correctPosCount++;
                }
            }

            //Position did not match, check if colors match
            else {
                if(checkColorMatch(key))
                    correctColorCount++;
            }
        }

        //TODO: Send message to other phones to launch the Win Activity
        //Get the list of other devices, need their String to send data packet
        Map<String, Position> deviceList = GlobalResources.getInstance().getDevices();

        if(fullMatches == high){
            //Send the information to launch the Win Activity to other devices
            for(Map.Entry<String, Position> entry : deviceList.entrySet()){
                GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_WIN));
            }

            //Master: Launch the Win activity
            launchWinIntent();
        }

        //Send message to other phones to launch the FeedBack Activity
        else{
            //Send the data packet to other devices
            for(Map.Entry<String, Position> entry : deviceList.entrySet()){
                //First send out full matches, then correct positions, then correct colors, then all wrong
                if(fullMatches > 0){
                    GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_FEEDBACK, ALL_CORRECT));
                    fullMatches--;
                }
                else if(correctPosCount>0){
                    GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_FEEDBACK, CORRECT_POS));
                    correctPosCount--;
                }
                else if(correctColorCount>0){
                    GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_FEEDBACK, CORRECT_COLOR));
                    correctColorCount--;
                }
                else{
                    GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_FEEDBACK, ALL_WRONG));
                }
            }

            //Launch the feedback for self
            //Checking for full matches not necessary (otherwise fullMatches == high would have been true)
            if(correctPosCount>0){
                launchFeedbackIntent(CORRECT_POS);
            }
            else if(correctColorCount>0){
                launchFeedbackIntent(CORRECT_COLOR);
            }
            else{
                launchFeedbackIntent(ALL_WRONG);
            }
        }
    }

    private void launchFeedbackIntent(int feedbackType){
        launchedFeedback = true;
        Intent intent = new Intent(getBaseContext(), Feedback_activity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("feedback", feedbackType);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void launchWinIntent(){
        Intent intent = new Intent(getBaseContext(), WinActivity.class);
        startActivity(intent);
    }

    private boolean checkColorMatch(String key){
        for(int index = 0; index < confirmedPairs.size(); index++){
            if(confirmedPairs.get(index).getDeviceAddress().equals(key)){
                //Color matches the wanted ownColor
                if(confirmedPairs.get(index).getColor() == deviceColors.get(key).intValue()){
                    return true;
                }
            }
        }
        return false;
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

    //Get the Key from the Map by Value
    public String getKeyByValue(Map<String, Integer> map, int value){
        for(Map.Entry<String, Integer> entry : map.entrySet()){
            if(entry.getValue().intValue() == value)
                return entry.getKey();
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
        //if(GlobalResources.getInstance().getPatternDetector() != null && GlobalResources.getInstance().getPatternDetector().isPaused())
        //    GlobalResources.getInstance().getPatternDetector().setup();

        //Will continuously call the RunPatternDetector class
        Thread runPatternThread = getThread();
        runPatternThread.run();

        //Coming back from the feedback screen?
        if(launchedFeedback == true){
            //Re-enable all buttons
            button.setClickable(true);
            imageViewRED.setClickable(true);
            imageViewBLUE.setClickable(true);
            imageViewGREEN.setClickable(true);
            imageViewYELLOW.setClickable(true);

            launchedFeedback = false;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PatternDetector patternDetector = GlobalResources.getInstance().getPatternDetector();
        if(patternDetector != null)
            patternDetector.destroy();
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

    private Thread getThread(){
        return new Thread() {
            @Override
            public void run() {
                new RunPatternDetector(activity);
            }
        };
    }
}
