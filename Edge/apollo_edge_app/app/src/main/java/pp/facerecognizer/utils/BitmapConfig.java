package pp.facerecognizer.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Size;

/**
 * This class is to help manage some of the information that is essentially specific to one fullsize bitmap. The functionality in the constructor is required both when the fullsize bitmap comes from the camera and when the fullsize bitmap comes from an uploaded image.
 */
public class BitmapConfig {

    private static final int CROP_SIZE = 300;

    Bitmap fullsizeBm;
    Bitmap croppedBm;
    Matrix frameToCropTransform;
    Matrix cropToFrameTransform;

    public BitmapConfig(Size size, int sensorOrientation) {
        fullsizeBm = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBm = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888);
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        size.getWidth(), size.getHeight(),
                        CROP_SIZE, CROP_SIZE,
                        sensorOrientation, false);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    public Bitmap getCroppedBm() {
        return croppedBm;
    }

    public Matrix getFrameToCropTransform() {
        return frameToCropTransform;
    }

    public Matrix getCropToFrameTransform() {
        return cropToFrameTransform;
    }

    public Bitmap getFullsizeBm() {
        return fullsizeBm;
    }
}