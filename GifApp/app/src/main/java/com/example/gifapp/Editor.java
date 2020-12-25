package com.example.gifapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;


import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
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

    LruCache<String, Bitmap> memoryCache;

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

        final int maxMemory = (int)  (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap bitmap)
            {
                return bitmap.getByteCount() / 1024;
            }
        };

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
                saveImage();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        });
        btnBack.setOnClickListener(v -> finish());

        setEffects();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap)
    {
        if (getBitmapFromMemCache(key) == null)
        {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key)
    {
        return memoryCache.get(key);
    }


    private void saveImage() throws Exception
    {
//        viewGroup.setDrawingCacheEnabled(true);
//        viewGroup.buildDrawingCache();
//        Bitmap bm = viewGroup.getDrawingCache();
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "GIFs_" + timeStamp;
//        MediaStore.Images.Media.insertImage(getContentResolver(), bm, imageFileName , "This image was made by GIFApp");
//        viewGroup.setDrawingCacheEnabled(false);

        TypedArray framesArray = getResources().obtainTypedArray(frameArArray.getResourceId(selectedFrame,0));
        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();

        imgUser.setDrawingCacheEnabled(true);
        imgUser.buildDrawingCache();
        Bitmap bmp1 = imgUser.getDrawingCache();

        for(int i = 0; i < framesArray.length(); i++)
        {
            int gifIcon = framesArray.getResourceId(i,0);
            Bitmap bmp2 = BitmapFactory.decodeResource(getResources(), gifIcon);


            Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
            Canvas canvas = new Canvas(bmOverlay);
            canvas.drawBitmap(bmp1, new Matrix(), null);
            canvas.drawBitmap(bmp2, new Matrix(), null);

            bitmapArrayList.add(bmOverlay);

            //bmp2.recycle();

        }
        //bmp1.recycle();
        Glide.with(this).asGif().load(effectsArray.getResourceId(selectedFrame,0)).into(gifFrame);
        saveToFile(bitmapArrayList);


//        TypedArray framesArray = getResources().obtainTypedArray(frameArArray.getResourceId(selectedFrame,0));
//        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
//
//      //  imgUser.setDrawingCacheEnabled(true);
//      //  imgUser.buildDrawingCache();
//      //  Bitmap bmp1 = imgUser.getDrawingCache();
//
//
//
//        for(int i = 0; i < framesArray.length(); i++)
//        {
//            viewGroup.setDrawingCacheEnabled(true);
//            int gifIcon = framesArray.getResourceId(i,0);
//            //Glide.with(this).asGif().load(gifIcon).into(gifFrame);
//            gifFrame.setImageResource(gifIcon);
//            viewGroup.buildDrawingCache();
//            Bitmap bmp = viewGroup.getDrawingCache();
//
//            //addBitmapToMemoryCache(bmp.toString(),bmp);
//
//
//            bitmapArrayList.add(bmp);
//            bmp.recycle();
//          //  viewGroup.destroyDrawingCache();
//          //  viewGroup.setDrawingCacheEnabled(false);
//
//        }
//        Glide.with(this).asGif().load(effectsArray.getResourceId(selectedFrame,0)).into(gifFrame);
//        saveToFile(bitmapArrayList);


    }

    public byte[] generateGIF(ArrayList<Bitmap> bitmaps)
    {
        TypedArray delayArray = getResources().obtainTypedArray(R.array.delay_array);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.setDelay(delayArray.getResourceId(selectedFrame,0));
        encoder.setRepeat(0);
        encoder.start(bos);

        for (Bitmap bitmap : bitmaps)
        {
            encoder.addFrame(bitmap);
        }
        encoder.finish();
        return bos.toByteArray();

    }

//    private void saveToFile(ArrayList<Bitmap> bitmaps)
//    {
//        FileOutputStream outStream = null;
//        try
//        {
//            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//            String imageFileName = "GIF_" + timeStamp + "_";
//
//           File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

//            File image = File.createTempFile(
//                    imageFileName,
//                    ".gif",
//                    storageDir
//            );
//
//
//            outStream = new FileOutputStream(image);
//
//            outStream.write(generateGIF(bitmaps));
//            outStream.close();
//            Toast.makeText(getApplicationContext(),"GIF saved successfully!",Toast.LENGTH_SHORT).show();
//        }catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//    }

    private void saveToFile(ArrayList<Bitmap> bitmaps)
    {
        FileOutputStream outStream = null;
        try
        {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "GIF_" + timeStamp + "_";

            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
           // File storageDir = getFilesDir();
            File image = File.createTempFile(
                    imageFileName,
                    ".gif",
                    storageDir
            );

            outStream = new FileOutputStream(image);
            outStream.write(generateGIF(bitmaps));
            outStream.close();


            final ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image" + System.currentTimeMillis());
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/gif");
            final Uri gifContentUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            OutputStream outputStream = getApplicationContext().getContentResolver().openOutputStream(gifContentUri, "w");
            IOUtils.copy(new FileInputStream(image), outputStream);


            Toast.makeText(getApplicationContext(),"GIF saved successfully!",Toast.LENGTH_SHORT).show();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
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
}