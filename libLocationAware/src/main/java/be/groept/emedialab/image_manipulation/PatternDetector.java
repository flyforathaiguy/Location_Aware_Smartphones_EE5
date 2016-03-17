package be.groept.emedialab.image_manipulation;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import be.groept.emedialab.R;
import be.groept.emedialab.math.PositionCalculation;
import be.groept.emedialab.movement.MovementAccelerometer;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.Calibration;
import be.groept.emedialab.util.GlobalResources;
import be.groept.emedialab.util.Point3D;
import be.groept.emedialab.util.Tuple;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
//import org.opencv.highgui.Highgui;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Grabs frames from the camera and gives them to the Pattern Detection Algorithm.
 * The results are stored in the {@link GlobalResources} Singleton.
 *
 * The constructor can be called at any time. The method {@link #setup()} has to be called after the OpenCV library has loaded.
 * Use {@link #destroy()} to clean up.
 *
 *
 * @see PatternDetectorAlgorithmInterface
 */
public class PatternDetector{

    private static final String TAG = "ArrowGame";
    private static final long sampleRate = 500;
    private final static boolean DEBUG = false;

    private PositionCalculation calc;
    private Camera mCamera;
    private PatternDetectorAlgorithmInterface patternDetectorAlgorithm;
    //private Calibration cali;
    private boolean isPaused = false;
    private ArrayList<Long> averageTime = new ArrayList<>();
    //private boolean handledPicture = true;
    private boolean runRunnable = true;
    private int sleepTime = 300;

    //ScheduledExecutorService executor;
    //ExecutorService executor;

    private Camera.PictureCallback jpegCallBack;
    private Camera.AutoFocusCallback autoFocusCallback;

    private Mat rgba, grey, binary;

    /**
     * Indicates which camera should be used.
     * '0' = back facing camera.
     * '1' = front facing camera.
     */
    private int cameraNum = 1;

    //for debugging purposes
    private int picCount = 0;

    //private Thread cameraThread;

   /* private Runnable cameraRunnable = new Runnable() {
        @Override
        public void run() {
            while (runRunnable) {
                //first check if the last picture has been fully handled, to prevent overloading the camera
                if (handledPicture == true) {
                    try {
                        //if(GlobalResources.getInstance().getCalibrated()) {
                        if (mCamera != null) {
                            long startTime;
                            if (DEBUG) {
                                startTime = System.currentTimeMillis();
                            }
                            handledPicture = false;
                            Tuple<PatternCoordinates, Mat> patternAndImagePair = null;
                            patternAndImagePair = pair();

                            calculateCoordinates(patternAndImagePair.element1);
                            if (DEBUG) {
                                timeMeasure(startTime);
                            }
                            System.gc();
                        }
                        //}
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    */

    public PatternDetector(int camera, boolean newAlgorithm) {
        if(newAlgorithm){
            patternDetectorAlgorithm = new PatternDetectorAlgorithm(5);
        }else{
            patternDetectorAlgorithm = new PatternDetectorAlgorithmOld();
        }
        setCamera(camera);
    }

    public PatternDetector(int camera, boolean newAlgorithm, Context mContext) {
        this(camera, newAlgorithm);
        // TODO: currently this object lives here, but it shouldn't. It should be in a runnable? Not a runnable, runnable gets executed at fixed time intervals
        new MovementAccelerometer(mContext);
    }

    private void calculateCoordinates(PatternCoordinates patternCoordinates) {
        Point3D deviceCoordinates = calc.patternToReal(patternCoordinates); //Calculate the position of this device.
        double angle = calc.calculateRotation(patternCoordinates);
        Position devicePosition = new Position(deviceCoordinates.getX(), deviceCoordinates.getY(), deviceCoordinates.getZ(), angle);
        devicePosition.setFoundPattern(patternCoordinates.getPatternFound());
        GlobalResources.getInstance().updateOwnPosition(devicePosition);

        /*
        if(GlobalResources.getInstance().getCalibrated() == false){
            cali.getAngleView().setText(String.format("Angle: %.1f°", angle));
        }
        */
    }

    /**
     * Load camera with the {@link VideoCapture} class.
     * Code is based on <a href="http://developer.sonymobile.com/knowledge-base/tutorials/android_tutorial/get-started-with-opencv-on-android/">http://developer.sonymobile.com/knowledge-base/tutorials/android_tutorial/get-started-with-opencv-on-android/</a>
     */
    public void setup() {

        //Release any possible previous cameras
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        Log.d(TAG, "runRunnable = true");

        mCamera = Camera.open(cameraNum);

        rgba = new Mat();
        grey = new Mat();
        binary = new Mat();

        try{
            //Using a dummy texture --> Results in no camera visible on screen
            mCamera.setPreviewTexture(new SurfaceTexture(10));

            //Getting & Setting Camera parameters
            Camera.Parameters param = mCamera.getParameters();
             param.set("orientation", "landscape");
             param.set("rotation", 270);

            //param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            //param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //mCamera.autoFocus(null);
            mCamera.setParameters(param);
        } catch (IOException e){
            e.printStackTrace();
            Log.d(TAG, "Error in PatternDetector Setup");
        }

        GlobalResources.getInstance().setPictureWidth(mCamera.getParameters().getPictureSize().width);
        GlobalResources.getInstance().setPictureHeight(mCamera.getParameters().getPictureSize().height);

        //This is called when the camera has autofocussed
        /*
        autoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "autofocus called");
                mCamera.takePicture(null, null, jpegCallBack);
            }
        };
        */

        //This is called when the camera takes a picture (in the pair() method)
        jpegCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                List<String> supportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
                Log.d(TAG, "Focus mode: \n");
                for(int i = 0; i < supportedFocusModes.size();i ++){
                    Log.d(TAG, supportedFocusModes.get(i) + "\n");

                }
               // Log.d(TAG, "Current focus mode: " + mCamera.getParameters().getFocusMode());

               // Log.d(TAG, "onPictureTaken called");
                mCamera.stopPreview();


                /*
                if(picCount >=19) {
                    picCount = 0;
                    FileOutputStream outStream = null;
                    try {
                        String dir_path = "";// set your directory path here
                        outStream = new FileOutputStream(String.format("/sdcard/DCIM/Camera/%d.jpg", System.currentTimeMillis()));
                        outStream.write(data);
                        outStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                picCount++;
                */


                //Create Mat Object from data
                try{
                    rgba = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                    if(cameraNum ==1){
                        Core.flip(rgba, rgba, 1);
                    }

                    // Convert to grey-scale.
                    Imgproc.cvtColor(rgba, grey, Imgproc.COLOR_RGB2GRAY);
                    // Threshold the grey-scale to binary
                    Imgproc.threshold(grey, binary, 80, 255, Imgproc.THRESH_BINARY);
                } catch (Exception e){
                    Log.d(TAG, "Exception in Mat: " + e.toString() + " message: " + e.getMessage());
                }
                //handledPicture = true;

                //Handle the picture
                Tuple<PatternCoordinates, Mat> patternAndImagePair = null;
                patternAndImagePair = pair();
                calculateCoordinates(patternAndImagePair.element1);

                try {
                    mCamera.setPreviewTexture(new SurfaceTexture(10));
                } catch (Exception e){
                    Log.d(TAG, "Exception setting preview texture)");
                }

                mCamera.startPreview();
                //camera.autoFocus(autoFocusCallback);


            }
        };


        mCamera.startPreview();
        //mCamera.autoFocus(autoFocusCallback);

        Thread takePic = new Thread(){
            public void run(){
                while(runRunnable) {
                    mCamera.takePicture(null, null, jpegCallBack);
                    //refreshCamera();
                    updateSleepTime();
                    try {
                        this.sleep(sleepTime);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };

        takePic.start();

        //this.cali = GlobalResources.getInstance().getCalibration();
    }

    private void updateSleepTime(){
        //Decrement
        if(GlobalResources.getInstance().getDevice().getPosition().getFoundPattern() == true){
            sleepTime = Math.max(200, sleepTime - 50);
        }
        else{
            //Increment
            sleepTime = Math.min(1000, sleepTime + 50);
        }
        Log.d(TAG, "SleepTime: " + sleepTime);
    }

    /**
     * Release Camera and cleanup.
     */
    public void destroy(){
        Log.d(TAG, "onDestroy called");

        runRunnable = false;

            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            isPaused = true;
            // TODO stop executorService


        Log.d(TAG, "runRunnable = false");
        GlobalResources.getInstance().setPatternDetector(null);
        System.gc();

    }

    public boolean isPaused(){
        return isPaused;
    }

    public void setCalc(PositionCalculation calc) {
        this.calc = calc;
    }

    public int getCamera() {
        return cameraNum;
    }

    private void setCamera(int camera) {
        if(camera != 0 && camera != 1){
            throw new IllegalArgumentException("Invalid value: camera should be 0 or 1.");
        }else{
            this.cameraNum = camera;
        }
    }

    private void timeMeasure(long startTime){
        long thisTime = (System.currentTimeMillis() - startTime);
        if(thisTime < 300){
            averageTime.add(thisTime);
            long average = 0;
            for(long time : averageTime){
                average += time;
            }
            average /= averageTime.size();
            Log.d(TAG, "Average pattern recognition time: " + average + ", current interation time: " + thisTime);
        }else{
            Log.e(TAG, "Too long thisTime! " + thisTime);
        }
    }

    private Tuple<PatternCoordinates, Mat> pair(){

        //Take a picture with the camera, this also handles the Mat objects
        //mCamera.takePicture(null, null, jpegCallBack);

        Tuple<PatternCoordinates, Mat> patternAndImagePair = null;
        switch(GlobalResources.getInstance().getImageSettings().getBackgroundMode()){
            case ImageSettings.BACKGROUND_MODE_RGB:
                patternAndImagePair = patternDetectorAlgorithm.find(rgba, binary, false);
                break;
            case ImageSettings.BACKGROUND_MODE_GRAYSCALE:
                patternAndImagePair = patternDetectorAlgorithm.find(grey, binary, true);
                break;
            case ImageSettings.BACKGROUND_MODE_BINARY:
                patternAndImagePair = patternDetectorAlgorithm.find(binary, binary, true);
        }
        GlobalResources.getInstance().updateImage(patternAndImagePair.element2);
        return patternAndImagePair;
    }

    /*
    public void setCalibration(Calibration cali){
        this.cali = cali;
    }
    */

}
