package be.groept.emedialab.image_manipulation;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import be.groept.emedialab.math.PositionCalculation;
import be.groept.emedialab.util.Calibration;
import be.groept.emedialab.util.GlobalResources;

/**
 * Class to set up the pattern detector when game is started
 */

public class RunPatternDetector {

    private Activity activity;
    private static final String TAG = "ArrowGame";
    PatternDetector patternDetector = null;

    public RunPatternDetector(Activity activity){
        this.activity = activity;
        PatternDetector patternDetector = GlobalResources.getInstance().getPatternDetector();
        if(patternDetector != null)
            patternDetector.setup();
        BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        onOpenCVSuccessLoad();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, activity, baseLoaderCallback);
    }

    private void onOpenCVSuccessLoad(){
            setupPatternDetector();
    }

    private int getFrontCamIndex(){
        //Select camera
        //If there is no front facing camera, use back camera
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for(int i = 0; i < Camera.getNumberOfCameras(); i++){
            Camera.getCameraInfo(i, ci);
            if(ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return i;
        }
        return 1;
    }

    @SuppressWarnings("deprecation")
    private void setupPatternDetector(){
        patternDetector = null;
        try{
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
            double patternWidthCm = Double.parseDouble(sharedPref.getString("pattern_size", "18"));
            boolean newAlgorithm = sharedPref.getBoolean("new_algorithm", true);

            int cameraSelection = getFrontCamIndex();

            Camera camera = Camera.open(cameraSelection);
            Camera.Parameters params = camera.getParameters();
            Camera.Size imageSize = params.getPictureSize();
            camera.release();


            PositionCalculation positionCalculation = new PositionCalculation(patternWidthCm, imageSize.width, imageSize.height, params.getHorizontalViewAngle());

            patternDetector = new PatternDetector(cameraSelection, newAlgorithm);
            //patternDetector = new PatternDetector(cameraSelection, newAlgorithm, activity.getApplicationContext());
            patternDetector.setCalc(positionCalculation);
            GlobalResources.getInstance().setPatternDetector(patternDetector);
        } catch (RuntimeException e){
            e.printStackTrace();
        }
        setupCamera();
    }

    private void setupCamera(){
        Log.d(TAG, "RunPatternDetector setupCamera");
        if(patternDetector != null){
            Log.d(TAG, "RunPatternDetector calling patternDetector setup");
            Thread patternThread = new Thread() {
                public void run() {
                    patternDetector.setup();
                }
            };
            patternThread.start();

            //Check if the system is calibrated
            //If not, launch the calibration class with a new intent (UI thread)
            Log.d(TAG, "calibrated =" + GlobalResources.getInstance().getCalibrated());
            /*
            if (GlobalResources.getInstance().getCalibrated() == false){
                Log.d(TAG, "Not Calibrated");
                Log.d(TAG, "Made calibration class");
                Log.d(TAG, "Context: " + GlobalResources.getInstance().getContext());
                Intent intent = new Intent(GlobalResources.getInstance().getContext(), Calibration.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Made intent");
                GlobalResources.getInstance().getContext().startActivity(intent);
                Log.d(TAG, "Launched intent");
            }
            */
        }
    }
}
