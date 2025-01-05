package com.atakmap.android.apolloedge.pp.facerecognizer;

import androidx.fragment.app.Fragment;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.atakmap.android.apolloedge.plugin.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.COMMAND;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.IMAGE_PATH_BUNDLE_KEY;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.IMAGE_PATH_EXTRA_NAME;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.MOBILE;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.ORIGINAL_IMAGE_PATH_BUNDLE_KEY;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.POSITION_BUNDLE_KEY;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.REF_JSON;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.ROOT;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.readJsonObjectFromFile;


public class ImageDetailSlideActivity extends FragmentActivity {
    private static final int NUM_PAGES = 2;
    String imagePath;
    String imgKeyInRefJson;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        // Receive extra sent from the class that opens this activity
        imagePath = getIntent().getStringExtra(IMAGE_PATH_EXTRA_NAME);

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new ImageDetailPagerAdapter(this);
        // Add instances of fragments (one for mobile, one for Command)
        int mobileResultPosition = 0;
        int commandResultPosition = 1;
        pagerAdapter.createFragment(mobileResultPosition);
        pagerAdapter.createFragment(commandResultPosition);
        viewPager.setAdapter(pagerAdapter);

        String[] pathEls = imagePath.split("/");
        String userSelectedBasename = pathEls[pathEls.length-1];
        // Parse the reference json to find out if the user selected a mobile result or a Command result
        File refJson = new File(REF_JSON);
        JSONObject refJsonObj = null;
        try {
            refJsonObj = readJsonObjectFromFile(refJson);
            Iterator<String> keys = refJsonObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object objValue = refJsonObj.get(key);
                String value = objValue.toString();
                JSONObject jsonChildObject = new JSONObject(value);
                String mobileBasename = jsonChildObject.get(MOBILE).toString();
                String cmdBasename;
                try {
                    cmdBasename = jsonChildObject.get(COMMAND).toString();
                } catch (JSONException e) {
                    cmdBasename = null;
                }
                // Note the key for this image; and set the starting Fragment based on which image the user clicked (mobile or Command)
                if (userSelectedBasename.equals(mobileBasename)) {
                    imgKeyInRefJson = key;
                    viewPager.setCurrentItem(mobileResultPosition);
                } else if (userSelectedBasename.equals(cmdBasename)) {
                    imgKeyInRefJson = key;
                    viewPager.setCurrentItem(commandResultPosition);
                } // else we haven't found a match yet, keep iterating
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    /**
     * An adapter which will create the fragments in the ViewPager
     */
    private class ImageDetailPagerAdapter extends FragmentStateAdapter {

        public ImageDetailPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            // Get the full path for the image we'll show in this fragment
            String fullpath = null;
            File refJson = new File(REF_JSON);
            JSONObject refJsonObj = null;
            try {
                refJsonObj = readJsonObjectFromFile(refJson);
                Object objValue = refJsonObj.get(imgKeyInRefJson);
                String value = objValue.toString();
                JSONObject jsonChildObject = new JSONObject(value);
                String basename = null;
                if (position == 0) {
                    basename = jsonChildObject.get(MOBILE).toString();
                } else if (position == 1) {
                    basename = jsonChildObject.get(COMMAND).toString();
                }
                fullpath = ROOT + File.separator + basename;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            // Custom fragments shouldn't override the default constructor, so we'll use a Bundle to pass data to the fragment
            Bundle bundle = new Bundle();
            bundle.putString(ORIGINAL_IMAGE_PATH_BUNDLE_KEY, ROOT + File.separator + imgKeyInRefJson);
            bundle.putString(IMAGE_PATH_BUNDLE_KEY, fullpath);
            bundle.putInt(POSITION_BUNDLE_KEY, position);
            ImageDetailSlideFragment imageDetailSlideFragment = new ImageDetailSlideFragment();
            imageDetailSlideFragment.setArguments(bundle);
            return imageDetailSlideFragment;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
