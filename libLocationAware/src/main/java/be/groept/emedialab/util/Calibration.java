package be.groept.emedialab.util;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import be.groept.emedialab.R;
import be.groept.emedialab.image_manipulation.PatternCoordinates;


public class Calibration extends AppCompatActivity {

    private int firstX, firstY, secondX, secondY;
    private Point3D firstCoordinates, secondCoordinates = null;
    private boolean firstCoordinatesReceived = false;

    private static final String TAG = "Calibration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        GlobalResources.getInstance().setCalibrating(true);
    }

    public void calibrate(View v){
        if(firstCoordinatesReceived == false){
            Point3D firstCoordinates = GlobalResources.getInstance().getPatternDetector().getCoordinates();
            this.firstCoordinates = firstCoordinates;
            firstCoordinatesReceived = true;
            Log.d(TAG, "First Coordinates in Calibration set");
        }
        else{
            Point3D secondCoordinates = GlobalResources.getInstance().getPatternDetector().getCoordinates();
            this.secondCoordinates = secondCoordinates;
            Log.d(TAG, "Second Coordinates in Calibration set");
            calculateCamOffset();
        }

        /*
        Calibrate the system using the 2 pairs of coordinates received through firstPair & secondPair
        Set the GlobalResources value for calibrated system to true
        Set the GlobalResources setCamXoffset & setCamYoffset to the calculated values
         */

        GlobalResources.getInstance().setCalibrated(true);
    }

    private void calculateCamOffset(){
        Log.d(TAG, "Calculated camOffset");
        GlobalResources.getInstance().setCalibrated(true);
        finish();
    }

}
