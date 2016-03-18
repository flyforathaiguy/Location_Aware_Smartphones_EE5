package be.groept.emedialab.util;

import android.bluetooth.BluetoothDevice;
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
    private double angle;
    private TextView text;
    private Button button;

    private static final String TAG = "ArrowGame";

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

        GlobalResources.getInstance().setCalibrationHandler(handler);
    }

    public void updatePosition(Position position){
        text.setText(String.format("Own Location: (%.2f, %.2f, %.2f) %.1f°", position.getX(), position.getY(), position.getZ(), position.getRotation()));
    }

    public void calibrate(View v){
        Log.d(TAG, "Button pressed");
        if(firstPositionReceived == false){
            Log.d(TAG, "firstPos");
            firstPosition = GlobalResources.getInstance().getDevice().getPosition();
            if(!firstPosition.equals(null) && !Double.isNaN(firstPosition.getRotation()) && !Double.isNaN(firstPosition.getX()) && !Double.isNaN(firstPosition.getY()) && !Double.isNaN(firstPosition.getY())) {

                firstPositionReceived = true;
                Log.d(TAG, "First Coordinates in Calibration set: x= " + firstPosition.getX() + " y=" + firstPosition.getY() + " angle= " + firstPosition.getRotation());
            }
        }
        else{
            Log.d(TAG, "second pos");
            secondPosition = GlobalResources.getInstance().getDevice().getPosition();

            if(!secondPosition.equals(null) && !Double.isNaN(secondPosition.getRotation()) && !Double.isNaN(secondPosition.getX()) && !Double.isNaN(secondPosition.getY()) && !Double.isNaN(secondPosition.getY())) {

                Log.d(TAG, "Second Coordinates in Calibration set: x= " + secondPosition.getX() + " y=" + secondPosition.getY() + " angle= " + secondPosition.getRotation());

                calculateCamOffset();
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

        xCenter = (firstPosition.getX() + secondPosition.getX())/2;
        yCenter = (firstPosition.getY() + secondPosition.getY())/2;

        //Determine if the camera is on the left or right side of the phone

        //Left side: the signs of firstX - secondX and firstY - secondY have to be the opposite of each other
        if( ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            xCenter = -Math.abs(firstPosition.getX() - xCenter);
        }

        else if( ( (firstPosition.getX() > secondPosition.getX()) && (firstPosition.getY() > secondPosition.getY()) ) || ( (firstPosition.getX() < secondPosition.getX()) && (firstPosition.getY() < secondPosition.getY()) ) ){
            //this means the camera is on the right side of the phone
            xCenter = Math.abs(firstPosition.getX() - xCenter);
        }

        else{
            //this means the camera is in the center of the phone
            xCenter = 0;
        }

        yCenter = Math.abs(firstPosition.getY() - yCenter);

        GlobalResources.getInstance().setCamXoffset(xCenter);
        GlobalResources.getInstance().setCamYoffset(yCenter);

        GlobalResources.getInstance().setCalibrated(true);

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
