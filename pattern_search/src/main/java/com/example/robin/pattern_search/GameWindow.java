package com.example.robin.pattern_search;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView feedbackText;
    private TextView ratingFeedback;
    private TextView positionText;
    private RatingBar rating;
    private double randomX;
    private double randomY;
    private boolean wonGame;
    private double stars;

    private static final int END_GAME = 3;
    private static final int POS_CONFIRM = 2;
    private static final int LAUNCH_WIN = 1;


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
            if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
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
            case LAUNCH_WIN:
                launchWinIntent();
            case END_GAME:
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

    private void confirmButton() {

        button.setClickable(false);

        //Calculate how far the target is from each phone
        targetCalculation();
    }

    private void targetCalculation() {
        //Iterates through all of the devices in the map
        //and calculates the distance between the random
        //location and the position of the phone
        for(Map.Entry<String, Position> entry : GlobalResources.getInstance().getDevices().entrySet()){
            if(entry.getKey().equals(GlobalResources.getInstance().getDevice())) {
                Location loc1 = new Location("");
                loc1.setLatitude(entry.getValue().getX());
                loc1.setLongitude(entry.getValue().getY());

                Location loc2 = new Location("");
                loc2.setLatitude(randomX);
                loc2.setLongitude(randomY);

                float distanceInMeters = loc1.distanceTo(loc2);

                if(distanceInMeters <= 4) {

                    launchWinIntent();

                }

                ratingCalculation(GlobalResources.getInstance().getDevice(), distanceInMeters);
            }
        }
    }

    private void ratingCalculation(Device device, float distance) {

        if(distance <= 8) {
            rating.setRating(4);
            ratingFeedback.setText("Getting real close!");
        }

        else if(distance <= 20) {
            rating.setRating(3);
            ratingFeedback.setText("Almost there!");

        }

        else if(distance <= 40) {
            rating.setRating(2);
            ratingFeedback.setText("On the way!");

        }

        else if(distance <= 70) {
            rating.setRating(1);
            ratingFeedback.setText("Better than nothing!");

        }

        else {
            rating.setRating(0);
            ratingFeedback.setText("Nowhere near!");
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
