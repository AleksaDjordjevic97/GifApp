package com.example.gifappv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;


import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Editor extends AppCompatActivity
{

    ImageView imgUser, gifFrame;
    ImageButton btnEffects,btnSave,btnBack;
    RecyclerView effectsView;
    ViewGroup viewGroup;
    TypedArray effectsArray, effectsIconsArray, frameArArray;
    int selectedFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        btnEffects = findViewById(R.id.btnEffects);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        imgUser = findViewById(R.id.imgSelected);
        gifFrame = findViewById(R.id.gifFrame);
        viewGroup = findViewById(R.id.frmImageLayout);
        effectsView = findViewById(R.id.rcvEffects);
        effectsArray = getResources().obtainTypedArray(R.array.effects_array);
        effectsIconsArray = getResources().obtainTypedArray(R.array.effects_icons_array);
        frameArArray = getResources().obtainTypedArray(R.array.frames_array);

        Intent getImageIntent = getIntent();
        Uri uri = getImageIntent.getParcelableExtra("SELECTED_IMAGE_URI");

        try
        {
            Glide.with(this).load(uri).into(imgUser);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        btnEffects.setOnClickListener(v -> showhideEffects());
        btnSave.setOnClickListener(v ->
        {

            try
            {
                ArrayList<Bitmap> bitmapArray;
                bitmapArray = createBitmapFrameArray();
//                generateGIF(bitmapArray);

                AsyncTask.execute(() ->
                {
                    try
                    {
                        generateGIF(bitmapArray);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });

                Toast.makeText(getApplicationContext(),"GIF saved successfully!",Toast.LENGTH_SHORT).show();

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        });
        btnBack.setOnClickListener(v -> finish());

        setEffects();
    }



    private void showhideEffects()
    {
        if(effectsView.getVisibility() == View.VISIBLE)
            effectsView.setVisibility(View.INVISIBLE);
        else
            effectsView.setVisibility(View.VISIBLE);
    }

    private void setEffects()
    {
        effectsView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(),RecyclerView.HORIZONTAL,false);
        effectsView.setLayoutManager(layoutManager);

        RecyclerView.Adapter effectsAdapter = new RecyclerView.Adapter<EffectsViewHolder>()
        {
            @NonNull
            @Override
            public EffectsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gif_cell_layout,parent,false);
                return new EffectsViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull EffectsViewHolder holder, int position)
            {
                holder.effect.setScaleType(ImageView.ScaleType.FIT_XY);
                holder.effect.setImageResource(effectsIconsArray.getResourceId(position,0));

                holder.effect.setOnClickListener(v ->
                {
                    int gifIcon = effectsArray.getResourceId(position,0);
                    Glide.with(getApplicationContext()).asGif().load(gifIcon).into(gifFrame);
                    selectedFrame = position;
                });
            }

            @Override
            public int getItemCount()
            {
                return effectsArray.length();
            }
        };

        effectsView.setAdapter(effectsAdapter);
        effectsView.setVisibility(View.INVISIBLE);
    }

    public class EffectsViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView effect;

        public EffectsViewHolder(View view)
        {
            super(view);

            effect = view.findViewById(R.id.gif_cell);
        }
    }

    private ArrayList<Bitmap> createBitmapFrameArray()
    {

        TypedArray framesArray = getResources().obtainTypedArray(frameArArray.getResourceId(selectedFrame,0));
        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();

        for(int i = 0; i < framesArray.length(); i++)
        {

            int gifIcon = framesArray.getResourceId(i,0);
            gifFrame.setImageResource(gifIcon);

            DisplayMetrics dm = getApplicationContext().getApplicationContext().getResources().getDisplayMetrics();
            viewGroup.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.EXACTLY));
            viewGroup.layout(0, 0, viewGroup.getMeasuredWidth(), viewGroup.getMeasuredHeight());
            Bitmap returnedBitmap = Bitmap.createBitmap(viewGroup.getMeasuredWidth(),
                    viewGroup.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(returnedBitmap);
            viewGroup.draw(c);


            bitmapArrayList.add(returnedBitmap);

        }
        Glide.with(this).asGif().load(effectsArray.getResourceId(selectedFrame,0)).into(gifFrame);

        return bitmapArrayList;

    }

    public void generateGIF(ArrayList<Bitmap> bitmaps) throws Exception
    {

        AnimatedGIFWriter writer = new AnimatedGIFWriter(false);
        OutputStream os = null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "GIF_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".gif",
                storageDir
        );

        os = new FileOutputStream(image);

        writer.prepareForWrite(os, -1, -1);

        for(Bitmap bitmap : bitmaps)
            writer.writeFrame(os, bitmap);

        writer.finishWrite(os);



        os.close();


        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DATA,"/storage/emulated/0/Pictures/Image." + System.currentTimeMillis() + ".gif");
       // contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image" + System.currentTimeMillis()+".gif");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/gif");
        final Uri gifContentUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        OutputStream outputStream = getApplicationContext().getContentResolver().openOutputStream(gifContentUri, "w");
        IOUtils.copy(new FileInputStream(image), outputStream);


       // Toast.makeText(getApplicationContext(),"GIF saved successfully!",Toast.LENGTH_SHORT).show();

    }

}