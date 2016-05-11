package com.example.robin.pattern_search;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Map;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.DeviceColorPair;
import be.groept.emedialab.util.GlobalResources;


public class GameWindow extends Activity {

    private Button button;
    private TextView feedbackText;

    private static final int END_GAME = 3;
    private static final int POS_CONFIRM = 2;
    private static final int LAUNCH_WIN = 1;


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
            if(msg.what == DataHandler.DATA_TYPE_COORDINATES) {
                //If coordinates are received, the master has to handle these coordinates
                //and store them in a list
                Serializable data;
                if((data = GlobalResources.getInstance().readData()) != null){
                    handleData((DataPacket) data);
                }
            }else if(msg.what == DataHandler.DATA_TYPE_DEVICE_DISCONNECTED){
                //Don't know yet, probalby end the game
                endGame();
            }
            else if(msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED){
               updateText();
            }
        }
    };
    private void updateText(){

        Position position = GlobalResources.getInstance().getDevice().getPosition();
        if(position.getFoundPattern()){
            //feedbackText.setTextColor(Color.parseColor("green"));
            feedbackText.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(be.groept.emedialab.R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        }else{
            feedbackText.setText("still.....");
        }
    }

    private void handleData(DataPacket dataPacket){
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case POS_CONFIRM:
                //Make new DeviceColorPair containing the address of the device from which the ownColor was sent, as well as the ownColor
                if(GlobalResources.getInstance().getReceivedList().size() > 0) {
                    DeviceColorPair pair = new DeviceColorPair(GlobalResources.getInstance().getReceivedList().get(GlobalResources.getInstance().getReceivedList().size() - 1), (int) dataPacket.getOptionalData());
                }
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
        //Log.d(TAG, "received list size: " + GlobalResources.getInstance().getReceivedList().size());
        if(GlobalResources.getInstance().getReceivedList().size() > 0)
            GlobalResources.getInstance().getReceivedList().remove(GlobalResources.getInstance().getReceivedList().size() - 1);
    }

    private void launchWinIntent() {
        Intent intent = new Intent(getBaseContext(), WinnerActivity.class);
        startActivity(intent);
    }

    private void confirmButton() {

        button.setClickable(false);

        //Get feedback text
        feedbackText = (TextView) findViewById(R.id.positionText);

        //In this case you're the client
        if(GlobalResources.getInstance().getClient() == true) {
            //Send data of type DATA_PACKET, DataPacket itself is of type COLOR
            Position position = GlobalResources.getInstance().getDevice().getPosition();
            GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_COORDINATES, new DataPacket(position));
        }

        //In this case you're the master
        else {




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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game_window, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
