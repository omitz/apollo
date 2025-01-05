package  com.atakmap.android.apolloedge.pp.facerecognizer;

// https://www.androidauthority.com/how-to-build-an-image-gallery-app-7189a76/

import android.graphics.Bitmap;

public class ImageData {
    private String imageTitle;
    private Integer imageId;
    private String imagePath;
    private Bitmap imageBitmap;

    public String getImageTitle() {
        return imageTitle;
    }

    public String getImagePath() {
        return imagePath;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageID(Integer id) {
        this.imageId = id;
    }

    public void setImageTitle(String title) {
        this.imageTitle = title;
    }

    public void setImagePath(String image_path) {
        this.imagePath = image_path;
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;}
}
