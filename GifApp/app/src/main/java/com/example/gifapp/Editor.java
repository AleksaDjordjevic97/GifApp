package com.example.gifapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Editor extends AppCompatActivity
{

    ImageView imgUser;
    ImageButton btnEffects,btnSave,btnBack;
    RecyclerView effectsView;
    ViewGroup viewGroup;
    TypedArray effectsArray;
    boolean openEffectsButton = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        btnEffects = findViewById(R.id.btnEffects);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        imgUser = findViewById(R.id.imgSelected);
        viewGroup = findViewById(R.id.frmImageLayout);
        effectsView = findViewById(R.id.rcvEffects);
        effectsArray = getResources().obtainTypedArray(R.array.effects_array);

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

        btnEffects.setOnClickListener(v -> showEffectsBar());
        btnSave.setOnClickListener(v -> saveImage());
        btnBack.setOnClickListener(v -> finish());
    }


    private void saveImage()
    {

        viewGroup.setDrawingCacheEnabled(true);
        viewGroup.buildDrawingCache();
        Bitmap bm = viewGroup.getDrawingCache();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "GIFs_" + timeStamp;
        MediaStore.Images.Media.insertImage(getContentResolver(), bm, imageFileName , "This image was made by GIFApp");
        viewGroup.setDrawingCacheEnabled(false);
    }

    private void showEffectsBar()
    {

    }
}