package com.atakmap.android.apolloedge.pp.facerecognizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.atakmap.android.apolloedge.plugin.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.GeolocUtils.convertDMSToDegree;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.IMAGE_PATH_BUNDLE_KEY;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.ORIGINAL_IMAGE_PATH_BUNDLE_KEY;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.POSITION_BUNDLE_KEY;

public class ImageDetailSlideFragment extends Fragment {
    // TODO Make sure the user will always be able to see the classification (i.e.handle cases where the face is to the far right of the frame, the picture is taken from far away, etc.)
    String imagePath;
    int position;
    String originalImagePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePath = getArguments().getString(IMAGE_PATH_BUNDLE_KEY);
        position = getArguments().getInt(POSITION_BUNDLE_KEY);
        originalImagePath = getArguments().getString(ORIGINAL_IMAGE_PATH_BUNDLE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.image_detail_scroll_view, container, false);

        ImageView imgView = view.findViewById(R.id.imgDetailImg);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imgView.setImageBitmap(bitmap);

        // Read in location data to display in text view
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(originalImagePath); // Get the EXIF data from the original image # TODO Consider writing the EXIF data to the Command download
            String latStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String longRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            if (latStr != null && longStr != null) {
                double latitude = convertDMSToDegree(latStr);
                double longitude = convertDMSToDegree(longStr);
                if (latRef.equals("S")) {
                    latitude *= -1;
                }
                if (longRef.equals("W")) {
                    longitude *= -1;
                }
                latStr = String.valueOf(latitude);
                longStr = String.valueOf(longitude);
            } else {
                latStr = "N/A";
                longStr = "N/A";
            }
            TextView textViewLatValue = view.findViewById(R.id.latValue);
            textViewLatValue.setText(latStr);
            TextView textViewLongValue = view.findViewById(R.id.longValue);
            textViewLongValue.setText(longStr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the date time of when the picture was taken (in our case, lastModified should be the creation date)
        File file = new File(originalImagePath);
        Date lastModDate = new Date(file.lastModified());
        TextView textViewDateValue = view.findViewById(R.id.dateValue);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(lastModDate);
        textViewDateValue.setText(date);
        TextView textViewTimeValue = view.findViewById(R.id.timeValue);
        String time =  new SimpleDateFormat("HH:mm:ssZ").format(lastModDate);
        textViewTimeValue.setText(time);

        TextView textViewHeader = view.findViewById(R.id.imgDetailTextHeader);
        if (position == 0) { // creating view for the mobile result
            ImageButton leftArrow = view.findViewById(R.id.leftArrow);
            leftArrow.setVisibility(View.GONE);
            textViewHeader.setText("Edge Results");
        } else if (position == 1) { // creating view for the Command result
            ImageButton rightArrow = view.findViewById(R.id.rightArrow);
            rightArrow.setVisibility(View.GONE);
            textViewHeader.setText("Command Results");
        }

        return view;
    }
}
