package be.groept.emedialab.image_manipulation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import be.groept.emedialab.math.PositionCalculation;
import be.groept.emedialab.movement.MovementAccelerometer;
import be.groept.emedialab.server.data.Position;
import be.groept.emedialab.util.GlobalResources;
import be.groept.emedialab.util.Point3D;
import be.groept.emedialab.util.Tuple;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
    private boolean isPaused = false;
    private ArrayList<Long> averageTime = new ArrayList<>();
    private boolean handledPicture = true;
    private boolean runThread = true;
    private int sleepTime = 300;
    private int noPicCount = 0;
    private Camera.PictureCallback jpegCallBack;

    private Mat rgba, grey, binary;
    private int cameraNum = 1;
    private boolean debug = false;

    //for debugging purposes
    private int picCount = 0;

    public PatternDetector(int camera, boolean newAlgorithm) {
        if(newAlgorithm){
            Log.d(TAG, "Old algorithm");
            patternDetectorAlgorithm = new PatternDetectorAlgorithm(5);
        }else{
            Log.d(TAG, "New algorithm");
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

        Log.d(TAG, "runThread = true");

        mCamera = Camera.open(cameraNum);

        //Initialize the Mat objects
        rgba = new Mat();
        grey = new Mat();
        binary = new Mat();

        try{
            //Using a dummy texture --> Results in no camera visible on screen
            mCamera.setPreviewTexture(new SurfaceTexture(10));
            setParameters();
        } catch (IOException e){
            e.printStackTrace();
            Log.d(TAG, "Error in PatternDetector Setup");
        }

        //These variables are used in the PositionCalculation class
        GlobalResources.getInstance().setPictureWidth(mCamera.getParameters().getPictureSize().width);
        GlobalResources.getInstance().setPictureHeight(mCamera.getParameters().getPictureSize().height);

        //This is called when the camera takes a picture (in the pair() method)
        jpegCallBack = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                mCamera.stopPreview();

                //Create Mat Object from data
                try{
                    //Convert the picture to a Mat object
                    rgba = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                    Core.flip(rgba, rgba,1);

                    // Convert to grey-scale.
                    Imgproc.cvtColor(rgba, grey, Imgproc.COLOR_RGB2GRAY);
                    // Threshold the grey-scale to binary
                    Imgproc.threshold(grey, binary, 80, 255, Imgproc.THRESH_BINARY);
                } catch (Exception e){
                    Log.d(TAG, "Exception in Mat: " + e.toString() + " message: " + e.getMessage());
                }

                //For debugging: write the taken picture to the SD card (1 out of every 21 pics)
                if(debug) {
                    if (picCount >= 20) {
                        picCount = 0;
                        FileOutputStream outStream = null;
                        long time = System.currentTimeMillis();
                        try {
                            String dir_path = "";// set your directory path here
                            outStream = new FileOutputStream(String.format("/sdcard/DCIM/Camera/%d.jpg", time));
                            outStream.write(data);
                            outStream.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Convert Mat's into Bitmap and then save them to the SD card
                        Bitmap scale = Bitmap.createBitmap(rgba.width(), rgba.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(rgba, scale);
                        saveBitmap(scale, time, 1);

                        scale = Bitmap.createBitmap(rgba.width(), rgba.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(rgba, scale);
                        saveBitmap(scale, time, 2);

                        scale = Bitmap.createBitmap(rgba.width(), rgba.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(rgba, scale);
                        saveBitmap(scale, time, 3);

                    }
                    picCount++;
                }

                //Handle the picture
                Tuple<PatternCoordinates, Mat> patternAndImagePair = null;
                patternAndImagePair = pair();
                calculateCoordinates(patternAndImagePair.element1);

                try {
                    mCamera.setPreviewTexture(new SurfaceTexture(10));
                    setParameters();
                } catch (Exception e){
                    Log.d(TAG, "Exception setting preview texture)");
                }

                mCamera.startPreview();
                handledPicture = true;
            }
        };

        mCamera.startPreview();

        //Different thread, takes pictures & runs the pattern recognition, reruns for as long as runThread is true
        //To not overload the camera, use the handledPicture variable
        Thread takePic = new Thread(){
            public void run(){
                while(runThread) {
                    if(handledPicture) {
                        handledPicture = false;
                        noPicCount = 0;
                        try {
                            mCamera.takePicture(null, null, jpegCallBack);
                        } catch (Exception e){
                            e.printStackTrace();
                            Log.d(TAG, "Error when trying to take a picture");

                            //If error, restart the camera
                            mCamera.stopPreview();
                            try {
                                mCamera.setPreviewTexture(new SurfaceTexture(10));
                                setParameters();
                            } catch (Exception f){
                                Log.d(TAG, "Exception setting preview texture after picture error");
                            }

                            mCamera.startPreview();
                            handledPicture = true;
                        }
                        //Update the time that the thread has to sleep, depends on the status of the last taken picture
                        updateSleepTime();
                        System.gc();
                    }
                    else noPicCount++;

                    /*
                    If there has not been taken a picture in a long time, it is possible that the camera has encountered and has to be reset.
                    This happens here
                     */
                    if(noPicCount > 40){
                        mCamera.stopPreview();
                        mCamera.release();

                        mCamera.open(cameraNum);

                        try {
                            mCamera.setPreviewTexture(new SurfaceTexture(10));
                        } catch (Exception e){
                            Log.d(TAG, "Exception setting preview texture");
                        }
                        mCamera.startPreview();
                        noPicCount = 0;

                    }
                    try {
                        this.sleep(sleepTime);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        //Start the thread
        takePic.start();
    }


    //Save the bitmap image to the SD card
    public void saveBitmap(Bitmap bm, float time, int num)
    {
        try
        {
            FileOutputStream stream = new FileOutputStream((String.format("/sdcard/DCIM/Camera/%f.jpg", (time + num))));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
        }
        catch(Exception e)
        {
            Log.d(TAG, "Could not save" + e.toString());
        }
    }

    private void setParameters(){
        //Getting & Setting Camera parameters
        Camera.Parameters param = mCamera.getParameters();
        param.set("orientation", "landscape");
        //param.set("rotation", 90);
        param.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);

        Display display = ((WindowManager)GlobalResources.getInstance().getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if(display.getRotation() == Surface.ROTATION_0){
            mCamera.setDisplayOrientation(90);
        }
        if(display.getRotation() == Surface.ROTATION_270){
            mCamera.setDisplayOrientation(270);
        }

        mCamera.setParameters(param);
    }

    //Update the sleep time for the takePic thread, increment is no picture is detected, otherwise decrement
    private void updateSleepTime(){
        //Decrement
        if(GlobalResources.getInstance().getDevice().getPosition().getFoundPattern() == true){
            sleepTime = Math.max(200, sleepTime - 50);
        }
        else{
            //Increment
            sleepTime = Math.min(1000, sleepTime + 50);
        }
    }

    /**
     * Release Camera and cleanup.
     */
    public void destroy(){
        Log.d(TAG, "onDestroy called");
        runThread = false;

            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        isPaused = true;

        Log.d(TAG, "runThread = false");
        System.gc();
    }

    //Gets called when the system is calibrated & destroyed by Calibration class
    public void setPatternNull(){
        GlobalResources.getInstance().setPatternDetector(null);
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

}
