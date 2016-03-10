package be.groept.emedialab.math;

import android.util.Log;

import be.groept.emedialab.image_manipulation.PatternCoordinates;
import be.groept.emedialab.util.GlobalResources;
import be.groept.emedialab.util.Point3D;
import be.groept.emedialab.util.Vector;

import org.opencv.core.Point;

/**
 * Computes the x,y,z coordinates of the device using the outline of the detected pattern.
 */
public class PositionCalculation {
    private static final String TAG = "PatternDetectorTag";

    //Defines the corner points of the pattern in pixels
    private double xap, xbp, xcp, xdp;
    private double yap, ybp, ycp, ydp;

    //Defines the angle of view of the camera
    private double phiX;

    //The real life side of the pattern in cm.
    private double patternSide;

    //Canvas X Size
    private double canvasXSize;

    private double fieldOfViewX;

    private double scaleFactor;

    /**
     * @param patternSide The real life patternSide of the pattern in cm (The length of one of the sides of the square)
     * @param width Width in pixels of the full image in portrait.
     * @param height Height in pixels of the full image in portrait.
     * @param phiX Angle of view of the camera, careful this is the vertical angle in phone x-y conventions
     */
     public PositionCalculation(double patternSide, double width, double height, double phiX) {
         this.patternSide = patternSide;
         this.phiX = phiX;
         canvasXSize = width;
         double canvasYSize = height;
    }

    /**
     * Calculate the x,y and z coordinates of the device.
     * Position (0,0,0) is when the pattern is in the center.
     *
     * @param pattern Corner points of the pattern.
     *                Num1 is the point at the white inner square.
     *                Moving clockwise the points should correspond to Num2, Num3 and Num4.
     *                X-axis is 480side, Y-axis is the 640side. Make sure the pattern is in the right coordinates!
     * @return A new point representing the position of the device.
     */
    public Point3D patternToReal(PatternCoordinates pattern){
        initPixelValues(pattern);
        calculateFieldOfView();

        //Calculate z using the field of view of y (higher accuracy than x-axis)
        double zCoordinate = fieldOfViewX / (2 * Math.tan(Math.toRadians(phiX / 2)));

        //Log.d(TAG, "phi: " + phiX);

        Point centerPattern = calculateCenterPattern();

        //Set the origin of the coordinate system of the image in the center of the sensor
        centerPattern.x -= GlobalResources.getInstance().getPictureWidth()/2;
        centerPattern.y -= GlobalResources.getInstance().getPictureHeight()/2;

        //Flip over x-axis
        centerPattern.y *= -1;

        //Factor in screen offset
        /*
        Whether of not this factor has to be taken into account depends on if the device is calibrated or not
        Not Calibrated --> Don't take into account (Values need to be calculated)
        Calibrated --> Take into account (Values have been calculated)

        Using the GlobalResources getCamXoffset & getCamYoffset
         */
        if(GlobalResources.getInstance().getCalibrated()) {
            centerPattern.x -= (5.35 / scaleFactor);
            centerPattern.y -= (2 / scaleFactor);
        }
        else{
            //centerPattern.x = 1;
            //centerPattern.y = 1;
        }

        //Take the translated error of the camera into account

        /*
        Because of the Calibration we will implement, this method will probably not be needed anymore
         */
        /*
        double ex = (CameraConstants.getInstance().getEx() / CameraConstants.getInstance().getHeight()) * zCoordinate;
        double ey = (CameraConstants.getInstance().getEy() / CameraConstants.getInstance().getHeight()) * zCoordinate;
        */
        double ex = 0;
        double ey = 0;
        Point translated = new Point(
                centerPattern.x + ex,
                centerPattern.y + ey
        );
        double rotation = calculateRotation(pattern);
        Point rotated = new Point(
                translated.x * Math.cos(Math.toRadians(rotation)) + translated.y * Math.sin(Math.toRadians(rotation)),
                -translated.x * Math.sin(Math.toRadians(rotation)) + translated.y * Math.cos(Math.toRadians(rotation))
        );

        //Convert pixel values to real values
        double xCoordinate = rotated.x * scaleFactor;
        double yCoordinate = rotated.y * scaleFactor;

        return new Point3D(-xCoordinate, -yCoordinate, zCoordinate);
    }

    private void initPixelValues(PatternCoordinates patternCoordinates){
        xap = patternCoordinates.getNum(1).x;
        yap = patternCoordinates.getNum(1).y;
        xbp = patternCoordinates.getNum(2).x;
        ybp = patternCoordinates.getNum(2).y;
        xcp = patternCoordinates.getNum(3).x;
        ycp = patternCoordinates.getNum(3).y;
        xdp = patternCoordinates.getNum(4).x;
        ydp = patternCoordinates.getNum(4).y;
        //Log.d(TAG, "Xposities: 1: " + xap + " 2: " + xbp + " 3: " + xcp + " 4:" + xdp);
        //Log.d(TAG, "Yposities: 1: " + yap + " 2: " + ybp + " 3: " + ycp + " 4:" + ydp);

    }

    private Point calculateCenterPattern(){
        return new Point(
                (xap + xbp + xcp + xdp)/4,
                (yap + ybp + ycp + ydp)/4
        );
    }

    private void calculateFieldOfView(){
        //Take average of all pattern sides to stabilize the readings
        double patternSideAB = Math.sqrt(Math.pow(xap - xbp, 2) + Math.pow(yap - ybp, 2));
        double patternSideBC = Math.sqrt(Math.pow(xbp - xcp, 2) + Math.pow(ybp - ycp, 2));
        double patternSideCD = Math.sqrt(Math.pow(xcp - xdp, 2) + Math.pow(ycp - ydp, 2));
        double patternSideDA = Math.sqrt(Math.pow(xdp - xap, 2) + Math.pow(ydp - yap, 2));
        double patternSidePx = (patternSideAB + patternSideBC + patternSideCD + patternSideDA) / 4.0;

        scaleFactor = patternSide/patternSidePx;

        fieldOfViewX = (patternSide / patternSidePx) * canvasXSize;

        //Log.d(TAG, "Scalefactor: " + scaleFactor);
        //Log.d(TAG, "canvasXsize: " + canvasXSize);
        //Log.d(TAG, "fieldOfViewX= " + fieldOfViewX);
    }

    //TODO: this is called twice every run (once by PositionCalculation.patternToReal, once by PatternDetector.calculateCoordinates)
    public double calculateRotation(PatternCoordinates pixelPatternCoordinates){

        Point corner1 = pixelPatternCoordinates.getNum(1);
        Point corner2 = pixelPatternCoordinates.getNum(2);
        Point corner3 = pixelPatternCoordinates.getNum(3);
        Point corner4 = pixelPatternCoordinates.getNum(4);

        //Calculate rotation with the x-axis of the image coordinate system with the horizontal sides of the pattern
        Vector xAxis = new Vector(GlobalResources.getInstance().getPictureWidth(), 0);
        Vector side14 = new Vector(corner4.x - corner1.x, corner4.y - corner1.y);
        Vector side23 = new Vector(corner3.x - corner2.x, corner3.y - corner2.y);
        side14.normalize();
        side23.normalize();
        xAxis.normalize();

        double cos14 = side14.dotProduct(xAxis);
        double cos23 = side23.dotProduct(xAxis);

        double sin14 = side14.crossProduct(xAxis);
        double sin23 = side23.crossProduct(xAxis);


        //Calculate rotation with the y-axis of the image coordinate system with the vertical sides of the pattern
        //Needs to be -480 because we flipped the y-axis
        Vector yAxis = new Vector(0, -GlobalResources.getInstance().getPictureHeight());
        Vector side12 = new Vector(corner2.x - corner1.x, corner2.y - corner1.y);
        Vector side43 = new Vector(corner3.x - corner4.x, corner3.y - corner4.y);
        yAxis.normalize();
        side12.normalize();
        side43.normalize();

        double cos12 = side12.dotProduct(yAxis);
        double cos43 = side43.dotProduct(yAxis);

        double sin12 = side12.crossProduct(yAxis);
        double sin43 = side43.crossProduct(yAxis);

        double cos = (cos14 + cos23 + cos12 + cos43)/4;
        double sin = (sin14 + sin23 + sin12 + sin43)/4;

        double rotationAngle= Math.toDegrees(Math.atan2(sin, cos));

        /*
        if(corner4.y > corner1.y)
            rotationAngle = 360 - Math.toDegrees(Math.acos(cos));
        else
            rotationAngle = Math.toDegrees(Math.acos(cos));
         */

        rotationAngle = (rotationAngle + 180 + 360)%360;




        return rotationAngle;


        /*
        Point corner1 = pixelPatternCoordinates.getNum(1);
        Point corner2 = pixelPatternCoordinates.getNum(2);
        Point corner3 = pixelPatternCoordinates.getNum(3);
        Point corner4 = pixelPatternCoordinates.getNum(4);
        Log.d(TAG, "4 points calc rotation: 1: " + corner1 + " 2: " + corner2 + " 3: " + corner3 + " 4: " + corner4);

        //Calculate rotation with the x-axis of the image coordinate system with the horizontal sides of the pattern
        Vector xAxis = new Vector(GlobalResources.getInstance().getPictureWidth(), 0);
        Vector side14 = new Vector(corner4.x - corner1.x, corner4.y - corner1.y);
        Vector side23 = new Vector(corner3.x - corner2.x, corner3.y - corner2.y);

        double cos14 = side14.dotProduct(xAxis)/(side14.getLength() * xAxis.getLength());
        double cos23 = side23.dotProduct(xAxis)/(side23.getLength() * xAxis.getLength());
        Log.d(TAG, "cos14: " + cos14);
        Log.d(TAG, "cos23: " + cos23);

        //Calculate rotation with the y-axis of the image coordinate system with the vertical sides of the pattern
        //Needs to be -480 because we flipped the y-axis
        Vector yAxis = new Vector(0, -GlobalResources.getInstance().getPictureHeight());
        Vector side12 = new Vector(corner2.x - corner1.x, corner2.y - corner1.y);
        Vector side43 = new Vector(corner3.x - corner4.x, corner3.y - corner4.y);

        double cos12 = side12.dotProduct(yAxis)/(side12.getLength() * yAxis.getLength());
        double cos43 = side43.dotProduct(yAxis)/(side43.getLength() * yAxis.getLength());

        Log.d(TAG, "cos12: " + cos12);
        Log.d(TAG, "cos43: " + cos43);

        double cos = (cos14 + cos23 + cos12 + cos43)/4;

        Log.d(TAG, "cos: " + cos);

        double rotationAngle;
        if(corner4.y > corner1.y)
            rotationAngle = 360 - Math.toDegrees(Math.acos(cos));
        else
            rotationAngle = Math.toDegrees(Math.acos(cos));

        Log.d(TAG, "Rotation " + rotationAngle);

        return rotationAngle;
        */
    }
}
