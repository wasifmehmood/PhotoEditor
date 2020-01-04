package com.example.photoeditor3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RecyclerAdapterViewHolder> {

    public static ArrayList<String> mFileName;
    public static ArrayList<Integer> imageViewsId;
    private final adapterListener onClickListener;
    final private RecyclerAdapterOnClickHandler mClickHandler;
    private Context context;

    public interface RecyclerAdapterOnClickHandler {
        void onClick(String fileName, View view, int position);
    }

    //region Interface adapter listener
    public interface adapterListener {

        void btnOnClick(View v, int position);
    }

    public RecyclerAdapter(RecyclerAdapterOnClickHandler handler, Context context, adapterListener listener) {
        mClickHandler = handler;
        this.onClickListener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        int layoutId = R.layout.list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutId, parent, shouldAttachToParentImmediately);
        RecyclerAdapterViewHolder viewHolder = new RecyclerAdapterViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapterViewHolder holder, final int position) {

        String fileNameClicked = mFileName.get(position);
        String upperString = fileNameClicked.substring(0,1).toUpperCase() + fileNameClicked.substring(1);
        holder.mFileNameTextView.setText(upperString);

        if(imageViewsId.size()>0)
        {
            holder.imageView.setImageResource(imageViewsId.get(position));
        }

        setTag(holder, fileNameClicked);
        setText(holder, fileNameClicked);


        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onClickListener.btnOnClick(v, position);
            }
        });
    }

    private void setTag(RecyclerAdapterViewHolder holder, String fileNameClicked) {


        if(fileNameClicked.equals("brightness"))
        {
            holder.imageView.setTag("exposure");
        }
        else if(fileNameClicked.equals("smooth"))
        {
            holder.imageView.setTag("luminance_denoise");
        }
        else if(fileNameClicked.equals("structure")){
            holder.imageView.setTag("clarity");
        }
        else {
            holder.imageView.setTag(fileNameClicked);
        }

    }

    private void setText(RecyclerAdapterViewHolder holder, String fileNameClicked) {

        if(fileNameClicked.equals("saturation_red"))
        {
            holder.mFileNameTextView.setText("Saturation red");
        }
        else if(fileNameClicked.equals("saturation_orange"))
        {
            holder.mFileNameTextView.setText("Saturation orange");
        }
        else if(fileNameClicked.equals("saturation_yellow"))
        {
            holder.mFileNameTextView.setText("Saturation yellow");
        }
        else if(fileNameClicked.equals("saturation_green"))
        {
            holder.mFileNameTextView.setText("Saturation green");
        }
        else if(fileNameClicked.equals("saturation_aqua"))
        {
            holder.mFileNameTextView.setText("Saturation aqua");
        }
        else if(fileNameClicked.equals("saturation_blue"))
        {
            holder.mFileNameTextView.setText("Saturation blue");
        }
        else if(fileNameClicked.equals("luminance_red"))
        {
            holder.mFileNameTextView.setText("luminance red");
        }
        else if(fileNameClicked.equals("luminance_orange"))
        {
            holder.mFileNameTextView.setText("Luminance orange");
        }
        else if(fileNameClicked.equals("luminance_yellow"))
        {
            holder.mFileNameTextView.setText("Luminance yellow");
        }
        else if(fileNameClicked.equals("luminance_green"))
        {
            holder.mFileNameTextView.setText("Luminance green");
        }
        else if(fileNameClicked.equals("luminance_aqua"))
        {
            holder.mFileNameTextView.setText("Luminance aqua");
        }
        else if(fileNameClicked.equals("luminance_blue"))
        {
            holder.mFileNameTextView.setText("Luminance blue");
        }
    }

    @Override
    public int getItemCount() {

        if (mFileName == null)
            return 0;
        return mFileName.size();
    }

    public void setFileNames(ArrayList<String> mFileName) {
        RecyclerAdapter.mFileName = mFileName;
        notifyDataSetChanged();
    }

    public void setImageViews(ArrayList<Integer> imageViewsId)
    {
        RecyclerAdapter.imageViewsId = imageViewsId;
    }

    public class RecyclerAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView mFileNameTextView;
        final ImageView imageView;
        final ConstraintLayout constraintLayout;

        RecyclerAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            mFileNameTextView = itemView.findViewById(R.id.list_text_view);
            imageView = itemView.findViewById(R.id.list_image_view);
            constraintLayout = itemView.findViewById(R.id.list_const_layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int adapterPosition = getAdapterPosition();
            String clickedFileName = mFileName.get(adapterPosition);

            mClickHandler.onClick(clickedFileName, v, adapterPosition);
        }
    }
}
