package be.groept.emedialab.util;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import be.groept.emedialab.R;
import be.groept.emedialab.communications.DataHandler;
import be.groept.emedialab.communications.DataPacket;
import be.groept.emedialab.server.data.Position;


public class Calibration extends AppCompatActivity {

    private Position firstPosition, secondPosition = null;
    private boolean firstPositionReceived, secondPositionReceived, compensatedXOffset = false;
    private double angle, wantedAngle;
    private TextView text, feedbackText;
    private CircularSeekBar circularSeekBar1;
    private Map<String, Position> confirmedPositions = new HashMap<String, Position>();
    private Button button;

    private static final int CONFIRMED_POS = 0;
    private static final int X_OFFSET = 1;
    private static final int Y_OFFSET = 2;


    private double xOffset = 0;

    private static final String TAG = "ArrowGame";

    //Handler receives information about the device's own position.
    //Has to happen via a handler since other threads cannot write to UI.
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DataHandler.DATA_TYPE_OWN_POS_UPDATED) {
                updatePosition((Position) msg.obj);
                if(secondPositionReceived == true) {
                    circularSeekBar1.setProgress((int)GlobalResources.getInstance().getDevice().getPosition().getRotation());
                }
            }
            //If a data packet arrives (probably confirmation from other device otf its position
            if (msg.what == DataHandler.DATA_TYPE_DATA_PACKET) {
                Serializable data;
                if((data = GlobalResources.getInstance().readData()) != null){
                    Log.d(TAG, "Received data packet in calibration");
                    handleData((DataPacket) data);
                }
            }
        }
    };

    private void handleData(DataPacket dataPacket){
        Log.d(TAG, "Reading datapacket of type " + dataPacket.getDataType());
        //Always read in the string from the sender of the data, to maintain data usage
        switch(dataPacket.getDataType()){
            case CONFIRMED_POS:
                //Confirmed position from other device, get its string from received list + get position from devices list
                String device = GlobalResources.getInstance().getReceivedList().get(GlobalResources.getInstance().getReceivedList().size()-1);
                Position devicePosition = GlobalResources.getInstance().getDevices().get(device);
                //Add this device + its position to the list
                confirmedPositions.put(device, devicePosition);
                Log.d(TAG, "Received confirmed pos");
                calculateOffset();
                break;
            //Client receives X-offset from master
            case X_OFFSET:
                Log.d(TAG, "Received X_OFFSET");
                compensateXOffset((double) dataPacket.getOptionalData());
                break;
            case Y_OFFSET:
                Log.d(TAG, "Received Y_OFFSET");
                compensateYOffset((double) dataPacket.getOptionalData());
                break;
            default:
                Log.d(TAG, "Default case");
                break;
        }
        //Remove address at last index since we do not need it
        //Should be the only one in the list ( list.size() == 1 --> index 0)
        GlobalResources.getInstance().getReceivedList().clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        GlobalResources.getInstance().setCalibrated(false);

        GlobalResources.getInstance().setCali(this);

        text = (TextView) findViewById(R.id.angleView);
        feedbackText = (TextView) findViewById(R.id.feedbackText);
        circularSeekBar1 = (CircularSeekBar) findViewById(R.id.circularSeekBar1);
        circularSeekBar1.setVisibility(View.INVISIBLE);

        GlobalResources.getInstance().setCalibrationHandler(handler);

        //Listener for the button
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (secondPositionReceived == false)
                    calibrate();
                else calibratePartTwo();
            }
        });
    }

    //Update the position that is represented on the screen as user feedback
    public void updatePosition(Position position){
        text.setText(String.format("%s (%.2f, %.2f, %.2f) %.1f°", getText(R.string.CalibrateOwnPosition), position.getX(), position.getY(), position.getZ(), position.getRotation()));

        if(firstPosition != null) {

            wantedAngle = (firstPosition.getRotation() + 180)%360;

            angle = Math.min(Math.abs(wantedAngle - position.getRotation()), Math.abs(360 - Math.abs(wantedAngle - position.getRotation())));
            feedbackText.setText(String.format("%s \n%.1f°", getText(R.string.CalibrateFeedback), angle));
            if (angle < 10)
                feedbackText.setTextColor(Color.parseColor("green"));
            else
                feedbackText.setTextColor(Color.parseColor("red"));
        }
    }

    private void calculateOffset(){
        int nbDevices = GlobalResources.getInstance().getDevices().size() + 1;
        //Not all devices have confirmed yet
        if(confirmedPositions.size() < nbDevices){
            Toast toast = Toast.makeText(this, "Not all devices have confirmed yet", Toast.LENGTH_SHORT);
            toast.show();
            Log.d(TAG, "nbDevices: " + nbDevices + " confirmedPositions size: " + confirmedPositions.size());
            return;
        }

        if(compensatedXOffset == false) {
            //All devices are in --> calculate average (X value should be the same)
            double avgX = 0;
            for (Map.Entry<String, Position> entry : confirmedPositions.entrySet()) {
                avgX += entry.getValue().getX();
            }
            avgX = avgX / nbDevices;
            Log.d(TAG, "AvgX: " + avgX);

            //Send offset of X-values to the phones
            double xOffset = 0;
            for (Map.Entry<String, Position> entry : confirmedPositions.entrySet()) {
                //Do not send it to the master (yourself)
                if (entry.getKey().equals("ownpos") == false) {
                    xOffset = avgX - GlobalResources.getInstance().getDevices().get(entry.getKey()).getX();
                    GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(X_OFFSET, xOffset));
                }
            }

            //Master for its own offset
            xOffset = avgX - GlobalResources.getInstance().getDevice().getPosition().getX();
            compensateXOffset(xOffset);
            confirmedPositions.clear();
            compensatedXOffset = true;
        }
        //Compensate Y offset
        else{
            //All devices are in --> calculate average (X value should be the same)
            double avgY = 0;
            for (Map.Entry<String, Position> entry : confirmedPositions.entrySet()) {
                avgY += entry.getValue().getY();
            }
            avgY = avgY / nbDevices;
            Log.d(TAG, "AvgY: " + avgY);

            //Send offset of X-values to the phones
            double yOffset = 0;
            for (Map.Entry<String, Position> entry : confirmedPositions.entrySet()) {
                //Do not send it to the master (yourself)
                if (entry.getKey().equals("ownpos") == false) {
                    yOffset = avgY - GlobalResources.getInstance().getDevices().get(entry.getKey()).getY();
                    GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(Y_OFFSET, yOffset));
                }
            }

            //Master for its own offset
            yOffset = avgY - GlobalResources.getInstance().getDevice().getPosition().getY();
            confirmedPositions.clear();
            compensateYOffset(yOffset);
        }
    }

    private void compensateXOffset(double xOffset){
        this.xOffset = xOffset;
        //GlobalResources.getInstance().setCamXoffset(GlobalResources.getInstance().getCamXoffset() - xOffset);
        Log.d(TAG, "Calibrated xOfset: " + xOffset);
        compensatedXOffset = true;
        Toast toast = Toast.makeText(this, "Compensated X offset!", Toast.LENGTH_LONG);
        toast.show();
        toast = Toast.makeText(this, "X offset: " + xOffset, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void compensateYOffset(double yOffset){
        GlobalResources.getInstance().setCamXoffset(GlobalResources.getInstance().getCamXoffset() - xOffset);
        GlobalResources.getInstance().setCamYoffset(GlobalResources.getInstance().getCamYoffset() - yOffset);
        Log.d(TAG, "Calibrated yOffset: " + yOffset);

        Toast toast = Toast.makeText(this, "Compensated Y offset!", Toast.LENGTH_LONG);
        toast.show();
        toast = Toast.makeText(this, "Y offset: " + yOffset, Toast.LENGTH_SHORT);
        toast.show();

        //compensating Y offset is the last part in the calibration that happens
        GlobalResources.getInstance().setCalibrated(true);
        GlobalResources.getInstance().setCalibrationHandler(null);
        finish();
    }

    public void calibratePartTwo(){
        //Angle has to be close to zero (<=2°)
        if(GlobalResources.getInstance().getDevice().getPosition().getRotation() < 350 && GlobalResources.getInstance().getDevice().getPosition().getRotation() > 10){
            Toast toast = Toast.makeText(this, "Angle offset too big" ,Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        //If client --> send to master, if master --> check if all positions are in
        if(GlobalResources.getInstance().getClient() == true){
            GlobalResources.getInstance().sendData(DataHandler.DATA_TYPE_DATA_PACKET, new DataPacket(CONFIRMED_POS));
            Log.d(TAG, "Sent confirmed pos signal");
            //button.setEnabled(false);
        }
        else {
            if(confirmedPositions.containsKey("ownpos") == false){
                confirmedPositions.put("ownpos", GlobalResources.getInstance().getDevice().getPosition());
            }
            calculateOffset();
        }
    }

    //Calibration procedure. Button is pressed twice, both time positions are saved to calibrate with
    public void calibrate(){
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
        else if(secondPositionReceived == false){
            Log.d(TAG, "second pos");
            secondPosition = GlobalResources.getInstance().getDevice().getPosition();

            if(!secondPosition.equals(null) && !Double.isNaN(secondPosition.getRotation()) && !Double.isNaN(secondPosition.getX()) && !Double.isNaN(secondPosition.getY()) && !Double.isNaN(secondPosition.getZ())) {

                //Difference in angles should not be greater than 1.5°
                if(Math.min(Math.abs(wantedAngle - secondPosition.getRotation()), Math.abs(360 - Math.abs(wantedAngle - secondPosition.getRotation()))) < 10) {
                    Log.d(TAG, "Second Coordinates in Calibration set: x= " + secondPosition.getX() + " y=" + secondPosition.getY() + " angle= " + secondPosition.getRotation());
                    secondPositionReceived = true;

                    calculateCamOffset();
                    button.setText("Calibrate Part Two");
                }
            }
        }
    }

    private void calculateCamOffset(){
        double xCenter, yCenter;

        Log.d(TAG, "Cali Positions");
        Log.d(TAG, "First: x= " + firstPosition.getX() + " y= " + firstPosition.getY());
        Log.d(TAG, "Second: x= " + secondPosition.getX() + " y= " + secondPosition.getY());

        //Determine if the camera is on the left or right side of the phone
        //Left side: the signs of firstX - secondX and firstY - secondY have to be the opposite of each other
        if( ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            xCenter = -Math.abs((firstPosition.getY()*Math.cos(angle/2) - secondPosition.getY()*Math.cos(angle/2))/2);
        }

        else if( ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            //this means the camera is on the right side of the phone
            xCenter = Math.abs((firstPosition.getY()*Math.cos(angle/2) - secondPosition.getY()*Math.cos(angle/2))/2);
        }

        else{
            //this means the camera is in the center of the phone
            xCenter = 0;
        }

        yCenter = Math.abs((firstPosition.getX()*Math.cos(angle/2) - secondPosition.getX()*Math.cos(angle/2))/2);

        GlobalResources.getInstance().setCamXoffset(xCenter);
        GlobalResources.getInstance().setCamYoffset(yCenter);

        //Set the Calibration to true, so the calibrated offset will be used in PositionCalculation
        GlobalResources.getInstance().setCalibratedCoordinates(true);

        Log.d(TAG, "Calculated camOffset");
        Toast toast = Toast.makeText(this,  "Camoffset: x= " + xCenter + " y= " + yCenter, Toast.LENGTH_LONG);
        toast.show();
        progressBarSetup();
    }

    private void progressBarSetup() {
        //Set visibility of the xml components
        feedbackText.setVisibility(View.INVISIBLE);
        circularSeekBar1.setVisibility(View.VISIBLE);
        circularSeekBar1.setIsTouchEnabled(false);
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
