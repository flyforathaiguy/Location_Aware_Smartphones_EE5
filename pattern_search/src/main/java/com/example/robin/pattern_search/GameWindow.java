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
import java.util.Map;

import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternDetector;
import be.groept.emedialab.image_manipulation.RunPatternDetector;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.GlobalResources;


public class GameWindow extends Activity {

    private Button button;
    private static final int END_GAME = 3;
    private TextView feedbackText;

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
                    //handleData((DataPacket) data);
                }
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
                //updateText();
            }
        }
    };

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
