package be.groept.emedialab.util;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;

import be.groept.emedialab.R;
import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.image_manipulation.PatternCoordinates;
import be.groept.emedialab.server.data.Position;


public class Calibration extends AppCompatActivity {

    private Position firstPosition, secondPosition = null;
    private boolean firstPositionReceived = false;
    private double angle, wantedAngle;
    private TextView text, feedbackText;
    private Button button;

    private static final String TAG = "ArrowGame";

    //Handler receives information about the device's own position.
    //Has to happen via a handler since other threads cannot write to UI.
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            if(msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED){
                //Update the Position & Rotation on the screen
                updatePosition((Position) msg.obj);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        GlobalResources.getInstance().setCalibrated(false);

        GlobalResources.getInstance().setCali(this);

        text = (TextView) findViewById(R.id.angleView);
        button = (Button) findViewById(R.id.button);
        feedbackText = (TextView) findViewById(R.id.feedbackText);

        GlobalResources.getInstance().setCalibrationHandler(handler);
    }

    //Update the position that is represented on the screen as user feedback
    public void updatePosition(Position position){
        text.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        if(firstPosition != null) {

            wantedAngle = (firstPosition.getRotation() + 180)%360;

            angle = Math.min(Math.abs(wantedAngle - position.getRotation()), Math.abs(360 - Math.abs(wantedAngle - position.getRotation())));
            feedbackText.setText(String.format("%s \n%.1f°",getText(R.string.CalibrateFeedback), angle));
            if (angle < 1.5)
                feedbackText.setTextColor(Color.parseColor("green"));
            else
                feedbackText.setTextColor(Color.parseColor("red"));
        }
    }

    //Calibration procedure. Button is pressed twice, both time positions are saved to calibrate with
    public void calibrate(View v){
        Log.d(TAG, "Button pressed");
        if(firstPositionReceived == false){
            Log.d(TAG, "firstPos");
            //Position is in centimeters
            firstPosition = GlobalResources.getInstance().getDevice().getPosition();
            if(!firstPosition.equals(null) && !Double.isNaN(firstPosition.getRotation()) && !Double.isNaN(firstPosition.getX()) && !Double.isNaN(firstPosition.getY()) && !Double.isNaN(firstPosition.getZ())) {

                firstPositionReceived = true;
                Log.d(TAG, "First Coordinates in Calibration set: x= " + firstPosition.getX() + " y=" + firstPosition.getY() + " angle= " + firstPosition.getRotation());
            }
        }
        else{
            Log.d(TAG, "second pos");
            secondPosition = GlobalResources.getInstance().getDevice().getPosition();

            if(!secondPosition.equals(null) && !Double.isNaN(secondPosition.getRotation()) && !Double.isNaN(secondPosition.getX()) && !Double.isNaN(secondPosition.getY()) && !Double.isNaN(secondPosition.getZ())) {

                //Difference in angles should not be greater than 1.5°
                if(Math.min(Math.abs(wantedAngle - secondPosition.getRotation()), Math.abs(360 - Math.abs(wantedAngle - secondPosition.getRotation()))) < 1.5) {
                    Log.d(TAG, "Second Coordinates in Calibration set: x= " + secondPosition.getX() + " y=" + secondPosition.getY() + " angle= " + secondPosition.getRotation());

                    calculateCamOffset();
                }
            }
        }

        /*
        Calibrate the system using the 2 pairs of coordinates received through firstPair & secondPair
        Set the GlobalResources value for calibrated system to true
        Set the GlobalResources setCamXoffset & setCamYoffset to the calculated values
         */
    }

    private void calculateCamOffset(){
        double xCenter, yCenter;

        Log.d(TAG, "Cali Positions");
        Log.d(TAG, "First: x= " + firstPosition.getX() + " y= " + firstPosition.getY());
        Log.d(TAG, "Second: x= " + secondPosition.getX() + " y= " + secondPosition.getY());

        //Determine if the camera is on the left or right side of the phone
        //Right side: the signs of firstX - secondX and firstY - secondY have to be the opposite of each other
        if( ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            yCenter = -Math.abs((firstPosition.getY() - secondPosition.getY())/2);
        }

        else if( ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            //this means the camera is on the left side of the phone
            yCenter = Math.abs((firstPosition.getY() - secondPosition.getY())/2);
        }

        else{
            //this means the camera is in the center of the phone
            yCenter = 0;
        }

        xCenter = Math.abs((firstPosition.getX() - secondPosition.getX())/2);

        GlobalResources.getInstance().setCamXoffset(yCenter);
        GlobalResources.getInstance().setCamYoffset(xCenter);

        //Set the Calibration to true, so the calibrated offset will be used in PositionCalculation
        GlobalResources.getInstance().setCalibrated(true);
        GlobalResources.getInstance().setCalibrationHandler(null);

        finish();
        Log.d(TAG, "Calculated camOffset");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, " Cali onDestroy called");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, " Cali onPause called");
        if(GlobalResources.getInstance().getPatternDetector() != null) {
                GlobalResources.getInstance().getPatternDetector().destroy();
        }
        //If the system is calibrated & onPause is called --> means the Activity is being killed --> Set PatternDetector to Null so the ArrowGame onResume thread can make a new one
        if(GlobalResources.getInstance().getCalibrated())
            GlobalResources.getInstance().getPatternDetector().setPatternNull();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(GlobalResources.getInstance().getPatternDetector() != null && GlobalResources.getInstance().getPatternDetector().isPaused())
            GlobalResources.getInstance().getPatternDetector().setup();
        Log.d(TAG, " Cali onResume called");
    }
}
