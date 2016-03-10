package be.groept.emedialab.image_manipulation;

import android.content.Context;
import android.content.Intent;
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

    private static final String TAG = "PatternDetectorTag";
    private static final long sampleRate = 200;
    private final static boolean DEBUG = false;

    private PositionCalculation calc;
    //private VideoCapture mCamera;
    private Camera mCamera;
    private PatternDetectorAlgorithmInterface patternDetectorAlgorithm;
    private boolean isPaused = false;
    private ArrayList<Long> averageTime = new ArrayList<>();

    private CameraBridgeViewBase.CvCameraViewFrame cam;

    private Camera.PictureCallback jpegCallBack;

    private Mat rgba, grey, binary;

    /**
     * Indicates which camera should be used.
     * '0' = back facing camera.
     * '1' = front facing camera.
     */
    private int camera = 1;

    //for debugging purposes
    private int picCount = 0;

    private Runnable cameraRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                //if(GlobalResources.getInstance().getCalibrated()) {
                    if (mCamera != null) {
                        long startTime;
                        if (DEBUG) {
                            startTime = System.currentTimeMillis();
                        }
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
    };

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
        Position devicePosition = new Position(deviceCoordinates.getX(), deviceCoordinates.getY(), deviceCoordinates.getZ(), calc.calculateRotation(patternCoordinates));
        devicePosition.setFoundPattern(patternCoordinates.getPatternFound());
        GlobalResources.getInstance().updateOwnPosition(devicePosition);
    }

    /**
     * Load camera with the {@link VideoCapture} class.
     * Code is based on <a href="http://developer.sonymobile.com/knowledge-base/tutorials/android_tutorial/get-started-with-opencv-on-android/">http://developer.sonymobile.com/knowledge-base/tutorials/android_tutorial/get-started-with-opencv-on-android/</a>
     */
    public void setup() {

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        mCamera = Camera.open(camera);

        try{
            //Using a dummy texture --> Results in no camera visible on screen
            mCamera.setPreviewTexture(new SurfaceTexture(10));

            //Getting & Setting Camera parameters
            Camera.Parameters param = mCamera.getParameters();
            param.set("rotation", 270);
            param.set("orientation", "portrait");
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(param);
        } catch (IOException e){
            e.printStackTrace();
            Log.d(TAG, "Error in PatternDetector Setup");
        }

        mCamera.startPreview();

        GlobalResources.getInstance().setPictureWidth(mCamera.getParameters().getPictureSize().width);
        GlobalResources.getInstance().setPictureHeight(mCamera.getParameters().getPictureSize().height);

        //This is called when the camera takes a picture (in the pair() method)
        jpegCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera)
            {

                //For debugging purposes: write picture to SDCard (1 in every 21 pictures)

                if(picCount==20){
                    picCount = 0;
                    FileOutputStream outStream = null;
                    try{
                        outStream = new FileOutputStream(String.format("/sdcard/DCIM/Camera/%d.jpg", System.currentTimeMillis()));
                        outStream.write(data);
                        outStream.close();
                        Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
                    } catch(FileNotFoundException e){
                        Log.d(TAG, "fileNotFoundException");
                        e.printStackTrace();
                    } catch (IOException e){
                        Log.d(TAG, "IOException");
                        e.printStackTrace();
                    } finally{
                        Log.d(TAG, "finally");
                    }

                }
                picCount++;
                Log.d(TAG, "picCount: " + picCount);


                mCamera.stopPreview();
                try {
                    mCamera.setPreviewTexture(new SurfaceTexture(10));
                } catch (Exception e){
                    Log.d(TAG, "Exception setting preview texture)");
                }
                mCamera.startPreview();


                //Create Mat Object from data
                try{
                    rgba = new Mat();
                    rgba = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                    grey = new Mat();
                    Imgproc.cvtColor(rgba, grey, Imgproc.COLOR_RGB2GRAY);
                    binary = new Mat();
                    Imgproc.threshold(grey, binary, 80, 255, Imgproc.THRESH_BINARY);
                } catch (Exception e){
                    Log.d(TAG, "Exception in Mat: " + e.toString() + " message: " + e.getMessage());
                }
            }
        };

/*
        //Check if the system is calibrated
        if (GlobalResources.getInstance().getCalibrated() == false){
            Log.d(TAG, "Not Calibrated");
            Log.d(TAG, "Made calibration class");
            Log.d(TAG, "Context: " + GlobalResources.getInstance().getContext());
            Intent intent = new Intent(GlobalResources.getInstance().getContext(), Calibration.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "Made intent");
            GlobalResources.getInstance().getContext().startActivity(intent);
            Log.d(TAG, "Launched intent");


                        /* Here it should call the Calibration class
                        Giving as parameter the patternAndImagePair.element1
                        The Calibration should define the X & Y offset of the camera to the center of the phone
                        and place these values in GlobalResources so that PositionCalculation can access them.
                        Also a bool var should be uses so the system knows whether or not it is calibrating, also placed in GlobalResources
                         */
        //}



        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(cameraRunnable, 0, sampleRate, TimeUnit.MILLISECONDS);
    }

    public Point3D getCoordinates(){
        Log.d(TAG, "getCoordinates mCamera:" + mCamera);
            Tuple<PatternCoordinates, Mat> patternAndImagePair = pair();
            Point3D devCoordinates = calc.patternToReal(patternAndImagePair.element1);
            return devCoordinates;
    }

    /**
     * Release Camera and cleanup.
     */
    public void destroy(){
        Log.d(TAG, "onDestroy called");
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            isPaused = true;
            // TODO stop executorService
    }

    public boolean isPaused(){
        return isPaused;
    }

    public void setCalc(PositionCalculation calc) {
        this.calc = calc;
    }

    public int getCamera() {
        return camera;
    }

    private void setCamera(int camera) {
        if(camera != 0 && camera != 1){
            throw new IllegalArgumentException("Invalid value: camera should be 0 or 1.");
        }else{
            this.camera = camera;
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
        //Mat rgba = new Mat();
        //Mat gray = new Mat();
        //Mat binary = new Mat();
        //mCamera.retrieve(rgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);

        /*if(camera == 1) {
            //Flip the image around the openCv x-axis (== Calc y-axis) if the front facing camera is used.
            //See: http://answers.opencv.org/question/8804/ipad-camera-input-is-rotated-180-degrees/
            Core.flip(rgba, rgba, 1);
            Core.flip(rgba, rgba, 0);
        }
        */
        //Take a picture with the camera, this also handles the Mat objects
        mCamera.takePicture(null, null, jpegCallBack);

        // Convert to grey-scale.
        //Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGB2GRAY);

        // Threshold the grey-scale to binary
        //Imgproc.threshold(gray, binary, 80, 255, Imgproc.THRESH_BINARY);

        Tuple<PatternCoordinates, Mat> patternAndImagePair = null;
        switch(GlobalResources.getInstance().getImageSettings().getBackgroundMode()){
            case ImageSettings.BACKGROUND_MODE_RGB:
                Log.d(TAG, "rgba mat: " + rgba);
                Log.d(TAG, "grey mat: " + grey);
                Log.d(TAG, "binary mat: " + binary);
                patternAndImagePair = patternDetectorAlgorithm.find(rgba, binary, false);
                //Log.d(TAG, "patternAndImagePair: " + patternAndImagePair);
                //Log.d(TAG, "patternAndImagePair element2: " + patternAndImagePair.element2);
                Log.d(TAG, "patternAndImagePair element1: " + patternAndImagePair.element1);
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
}
