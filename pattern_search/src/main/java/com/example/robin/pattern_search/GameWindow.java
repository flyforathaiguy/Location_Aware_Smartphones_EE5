package com.example.robin.pattern_search;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
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
    private TextView ratingFeedback;
    private TextView positionText;
    private RatingBar rating;
    private RelativeLayout frame;
    private double randomX;
    private double randomY;
    private boolean wonGame = false;
    private boolean randomLocationMade = false;

    private String TAG = "GameWindow";

    public static final int END_GAME = 3;
    public static final int RATING_CHOOSE = 2;
    public static final int LAUNCH_WIN = 1;
    public static final int LAUNCH_LOS = 0;

    private static final int LEFT_BOUNDARY = 90;
    private static final int TOP_BOUNDARY = 130;

    final Activity activity = this;
    private Thread runPatternThread;

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        GlobalResources.getInstance().setHandler(handler);

        //Set this context in GlobalResources
        GlobalResources.getInstance().setContext(getBaseContext());

        frame = (RelativeLayout) findViewById(R.id.frame);

        //Get accept button
        button = (Button) findViewById(R.id.confirmButton);
        if(GlobalResources.getInstance().getClient() == true) {
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
        }else {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Button being pressed");
                    confirmButton();
                };
            });
        }
    }

    //Handler for signals, large parts of code taken from original Location Aware Smart phones project
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
                //Log.d(TAG, "Position being updated");
                updatePosition();
            }
        }
    };

    private void handleData(DataPacket dataPacket){
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case RATING_CHOOSE:
                Log.d(TAG, "Setting ratings");
                setStars((int) dataPacket.getOptionalData());
                break;
            case LAUNCH_WIN:
                Log.d(TAG, "Launching win intent");
                launchWinIntent();
                break;
            case LAUNCH_LOS:
                Log.d(TAG, "Launching lose intent");
                launchLoserIntent();
                break;
            case END_GAME:
                Log.d(TAG, "Ending game");
                endGame();
                break;
            default:
                break;
        }
    }

    private void updatePosition(){

        Position position = GlobalResources.getInstance().getDevice().getPosition();
        if(position.getFoundPattern()){
            positionText.setTextColor(Color.parseColor("green"));
            positionText.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(be.groept.emedialab.R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        }else{
            positionText.setTextColor(Color.parseColor("red"));
        }
    }

    private void launchWinIntent() {
        wonGame = true;
        Intent intent = new Intent(getBaseContext(), WinnerActivity.class);
        startActivity(intent);
    }

    private void launchLoserIntent() {
        wonGame = false;
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
        Position randomPosition = new Position(randomX, randomY, 0, 0);

        Map<String, Position> positions = GlobalResources.getInstance().getDevices();
        if(!(positions.containsKey("ownpos")))
            positions.put("ownpos", GlobalResources.getInstance().getDevice().getPosition());

        double distance;
        String winnerString = "";
        for(Map.Entry<String, Position> entry : positions.entrySet()) {

            distance = entry.getValue().getXYDistance(randomPosition);
            Log.d(TAG, "phone: " + entry.getKey() + " is at a distance of " + distance);

            //Check if we have a winner
            if (distance <= 4) {
                Toast toast = Toast.makeText(this, "Someone has won", Toast.LENGTH_LONG);
                toast.show();
                Log.d(TAG, "phone: " + entry.getKey() + " has won and is at a distance of " + distance);
                winnerString = entry.getKey();
                wonGame = true;
            }
        }

        Log.d(TAG, "" + wonGame);

        //Start launching other intents or ratings
        /*if(wonGame == true){
            for(Map.Entry<String, Position> entry : positions.entrySet()){
                //Master can only launch his own intent after launching all the others
                if(entry.getKey().equals("ownpos") == false){
                    if(entry.getKey().equals(winnerString) == true){
                        GlobalResources.getInstance().sendData(winnerString, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(LAUNCH_WIN));
                    }
                    else{
                        GlobalResources.getInstance().sendData(entry.getKey(), DataHandler.DATA_TYPE_DATA_PACKET,new DataPacket(LAUNCH_LOS));
                    }
                }
            }
            if(winnerString.equals("ownpos") == true)
                this.launchWinIntent();
            else this.launchLoserIntent();
        }*/

        //No winner
        //else{
            for(Map.Entry<String, Position> entry : positions.entrySet()){
                if(entry.getKey().equals("ownpos") == false) {
                    ratingCalculation(entry.getKey(), entry.getValue().getXYDistance(randomPosition));
                }
            }
            //For master
            ratingCalculation("ownpos", GlobalResources.getInstance().getDevice().getPosition().getXYDistance(randomPosition));
            button.setEnabled(true);
        //}
    }

    private void ratingCalculation(String phoneId, double distance) {

        if(distance <= 8) {
            if(phoneId.equals("ownpos") == false)
                GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 4));
            else
                setStars((int) distance);
        }

        else if(distance <= 20) {
            if(phoneId.equals("ownpos") == false)
                GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 3));
            else
                setStars((int) distance);
        }

        else if(distance <= 40) {
            if(phoneId.equals("ownpos") == false)
                GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 2));
            else
                setStars((int) distance);
        }

        else if(distance <= 70) {
            if(phoneId.equals("ownpos") == false)
                GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 1));
            else
                setStars((int) distance);
        }

        else {
            if(phoneId.equals("ownpos") == false)
                GlobalResources.getInstance().sendData(phoneId, DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(RATING_CHOOSE, 0));
            else
                setStars((int) distance);
        }
    }

    private void setStars(int stars) {

        button.setClickable(true);

        if(stars == 0) {
            rating.setRating(0);
            ratingFeedback.setText("Nowhere near!");
            Log.d(TAG, "Nowhere near!");
        }

        if(stars == 1) {
            rating.setRating(1);
            ratingFeedback.setText("Better than nothing!");
            Log.d(TAG, "Better than nothing!");
        }

        if(stars == 2) {
            rating.setRating(2);
            ratingFeedback.setText("On the way!");
            Log.d(TAG, "On the way!");
        }

        if(stars == 3) {
            rating.setRating(3);
            ratingFeedback.setText("Almost there!");
            Log.d(TAG, "Almost there!");
        }

        if(stars == 4) {
            rating.setRating(4);
            ratingFeedback.setText("Getting real close!");
            Log.d(TAG, "Getting real close!");
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
        int randomNumberX = (rand.nextInt(LEFT_BOUNDARY)-LEFT_BOUNDARY/2);
        int randomNumberY = (rand.nextInt(TOP_BOUNDARY)-TOP_BOUNDARY/2);

        Log.d(TAG, "RandomX = " + randomNumberX);
        Log.d(TAG, "RandomY = " + randomNumberY);

        //Check if the random location is not too close to the master phone,
        //otherwise the game will end too soon
        while(Math.abs(masterX - randomNumberX) <= 12) {

            randomNumberX = (rand.nextInt(LEFT_BOUNDARY)-LEFT_BOUNDARY/2);
        }
        randomX = randomNumberX;


        //Check if the random location is not too close to the master phone,
        //otherwise the game will end too soon
        while(Math.abs(masterY - randomNumberY) <= 20) {

            randomNumberY = (rand.nextInt(TOP_BOUNDARY)-TOP_BOUNDARY/2);
        }
        randomY = randomNumberY;

        randomLocationMade = true;

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
        //Will continuously call the RunPatternDetector class
        runPatternThread = getThread();
        runPatternThread.run();

        //Coming back from the feedback screen / won game screen?
        if(wonGame == false){
            //Re-enable all buttons
            button.setClickable(true);
            if(randomLocationMade == false) {
                if(GlobalResources.getInstance().getClient() == false) {
                    if(GlobalResources.getInstance().getCalibrated() == false) {
                        makeRandomLocation();
                    }
                }
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
