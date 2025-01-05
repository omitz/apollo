package pp.facerecognizer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import pp.facerecognizer.utils.FileUtils;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.ImgViewHolder> {

    private ArrayList<CreateList> galleryList;
    private Context context;

    /**
     * Constructor for RVAdapter.
     */
    public RVAdapter(Context context, ArrayList<CreateList> galleryList) {
        this.galleryList = galleryList;
        this.context = context;
    }

    /**
     *
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param i  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. (<- now following new tut - not sure if this still applies)
     * @return A new NumberViewHolder that holds the View for each list item
     */
    @Override
    public ImgViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.img_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        ImgViewHolder viewHolder = new ImgViewHolder(view);

        return viewHolder;
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param i The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(ImgViewHolder holder, int i) {
        holder.title.setText(galleryList.get(i).getImageTitle());
        holder.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.img.setImageBitmap(galleryList.get(i).getImageBitmap());
        holder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                String file = galleryList.get(i).getImagePath();
                // Must provide the Uri via a FileProvider in order to use the default gallery app
                try {
                    intent.setDataAndType(FileProvider.getUriForFile(context, "pp.facerecognizer.fileprovider", new File(file)), "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } catch (IllegalArgumentException e) {
                    // Handle case where the "image" is an empty bitmap, ie do nothing
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    /**
     * Cache of the children views for a list item.
     */
    class ImgViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView img;
        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         * @param view The View that you inflated in
         *                 {@link RVAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        public ImgViewHolder(View view) {
            super(view);
            context = view.getContext();

            title = (TextView) view.findViewById(R.id.title);
            img = (ImageView) view.findViewById(R.id.img);
        }
    }
}
