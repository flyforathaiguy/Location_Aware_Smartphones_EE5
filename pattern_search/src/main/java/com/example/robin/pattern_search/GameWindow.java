package com.example.robin.pattern_search;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.server.data.Device;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.GlobalResources;


public class GameWindow extends Activity {

    private Button button;
    private TextView ratingFeedback;
    private TextView positionText;
    private RatingBar rating;
    private double randomX;
    private double randomY;
    private boolean wonGame;

    private String TAG = "GameWindow";

    private static final int END_GAME = 3;
    private static final int RATING_CHOOSE = 2;
    private static final int LAUNCH_WIN = 1;
    private static final int LAUNCH_LOS = 0;

    final Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_window);

        //Initialize the rating and the feedback
        rating = (RatingBar) findViewById(R.id.rating);
        ratingFeedback = (TextView) findViewById(R.id.ratingFeedback);
        rating.setRating(0);

        //Initialize the position text
        positionText = (TextView) findViewById(R.id.positionText);


        //If you're the master, you have to make a random location
        //of where the players need to find you
        if(GlobalResources.getInstance().getClient() == false) {
            Log.d(TAG, "Master making random location");
            makeRandomLocation();
        }


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


        //Get accept button
        button = (Button) findViewById(R.id.confirmButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button being pressed");
                confirmButton();
            }

            ;
        });
    }

    //Handler for signals, large parts of code taken from original Location Aware Smartphones project
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Log.d(TAG, "Handler called");
            //Data packet incoming
            if(msg.what == DataHandler.DATA_TYPE_DATA_PACKET) {
                //This will send the dataPacket to the DataHandler
                Serializable data;
                if ((data = GlobalResources.getInstance().readData()) != null) {
                    handleData((DataPacket) data);
                }
            }
            else if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
                //Don't know yet, probalby end the game
                endGame();
            }
            else if(msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED){
                updatePosition();
            }
        }
    };

    private void updatePosition(){

        Position position = GlobalResources.getInstance().getDevice().getPosition();
        if(position.getFoundPattern()){
            positionText.setTextColor(Color.parseColor("blue"));
            positionText.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(be.groept.emedialab.R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        }else{
            positionText.setTextColor(Color.parseColor("red"));
        }
    }

    private void handleData(DataPacket dataPacket){
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case RATING_CHOOSE:
                Log.d(TAG, "Setting ratings");
                setRating((int) dataPacket.getOptionalData());
            case LAUNCH_WIN:
                Log.d(TAG, "Launching win intent");
                launchWinIntent();
            case LAUNCH_LOS:
                Log.d(TAG, "Launching lose intent");
                launchLoserIntent();
            case END_GAME:
                Log.d(TAG, "Ending game");
                endGame();
            default:
                break;
        }
    }

    private void launchWinIntent() {
        wonGame = true;
        Intent intent = new Intent(getBaseContext(), WinnerActivity.class);
        startActivity(intent);
    }

    private void launchLoserIntent() {
        Intent intent = new Intent(getBaseContext(), LoserActivity.class);
        startActivity(intent);
    }

    private void confirmButton() {

        button.setClickable(false);

        //Calculate how far the target is from each phone if you're master
        if(GlobalResources.getInstance().getClient() == false) {
            Log.d(TAG, "Master is calculating distance to target");
            targetCalculation();
        }
    }

    private void targetCalculation() {
        //Iterates through all of the devices in the map
        //and calculates the distance between the random
        //location and the position of the phone
        for(Map.Entry<String, Position> entry : GlobalResources.getInstance().getDevices().entrySet()){

            Location loc1 = new Location("");
            loc1.setLatitude(entry.getValue().getX());
            loc1.setLongitude(entry.getValue().getY());

            Location loc2 = new Location("");
            loc2.setLatitude(randomX);
            loc2.setLongitude(randomY);
            float distance = loc1.distanceTo(loc2);

            Log.d(TAG, "phone: " + entry.getKey() + " is at a distance of " + distance);

            if(distance <= 4) {

                String winner = entry.getKey();
                GlobalResources.getInstance().sendData(winner, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_WIN));

                Log.d(TAG, "phone: " + entry.getKey() + " has won and is at a distance of " + distance);

                for(Map.Entry<String, Position> temp : GlobalResources.getInstance().getDevices().entrySet()){
                    if(!(temp.getKey().equals(winner))) {
                        GlobalResources.getInstance().sendData(temp.getKey(), DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_LOS));
                    }
                }
            }

            else {
                Log.d(TAG, "Rating Calculation will begin");
                ratingCalculation(entry.getKey(), distance);
            }
        }
    }

    private void ratingCalculation(String phoneId, float distance) {

        if(distance <= 8) {
            GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 4));
        }

        else if(distance <= 20) {
            GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 3));

        }

        else if(distance <= 40) {
            GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 2));

        }

        else if(distance <= 70) {
            GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 1));

        }

        else {
            GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 0));
        }
    }

    private void setRating(int stars) {

        if(stars == 0) {
            rating.setRating(0);
            ratingFeedback.setText("Nowhere near!");
        }

        if(stars == 1) {
            rating.setRating(1);
            ratingFeedback.setText("Better than nothing!");
        }

        if(stars == 2) {
            rating.setRating(2);
            ratingFeedback.setText("On the way!");
        }

        if(stars == 3) {
            rating.setRating(3);
            ratingFeedback.setText("Almost there!");
        }

        if(stars == 4) {
            rating.setRating(4);
            ratingFeedback.setText("Getting real close!");
        }
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


    private void makeRandomLocation(){

        //Getting the position of the master
        Position masterPosition = GlobalResources.getInstance().getDevice().getPosition();
        double masterX = masterPosition.getX();
        double masterY = masterPosition.getY();

        //Boundaries of where the phone can detect the pattern are hardcoded
        //Left to right boundary is 90cm
        //Top to bottom is 130cm
        Random rand = new Random();
        int randomNumberX = (rand.nextInt(90)-45);
        int randomNumberY = (rand.nextInt(130)-65);

        Log.d(TAG, "RandomX = " + randomNumberX);
        Log.d(TAG, "RandomY = " + randomNumberY);

        //Check if the random location is not too close to the master phone,
        //otherwise the game will end too soon
        while(Math.abs(masterX - randomNumberX) <= 12) {

            randomNumberX = (rand.nextInt(90)-45);
            randomX = randomNumberX;

        }

        while(Math.abs(masterY - randomNumberY) <= 20) {

            randomNumberY = (rand.nextInt(130)-65);
            randomY = randomNumberY;
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        //If this Activity is paused due to the Calibration being launched, do not destroy pattern detector!
        if(GlobalResources.getInstance().getPatternDetector() != null) {
            if(GlobalResources.getInstance().getCalibrated() == true) {
                if(wonGame == false) {
                    GlobalResources.getInstance().getPatternDetector().destroy();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(GlobalResources.getInstance().getPatternDetector() != null && GlobalResources.getInstance().getPatternDetector().isPaused())
            GlobalResources.getInstance().getPatternDetector().setup();

        //Coming back from the feedback screen / won game screen?
        if(wonGame == true){
            //Re-enable all buttons
            button.setClickable(true);

            if(wonGame == true){
                wonGame = false;
            }
        }
    }

    private Thread getThread(){
        return new Thread() {
            @Override
            public void run() {
                new RunPatternDetector(activity);
            }
        };
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PatternDetector patternDetector = GlobalResources.getInstance().getPatternDetector();
        if(patternDetector != null)
            patternDetector.destroy();
    }
}
