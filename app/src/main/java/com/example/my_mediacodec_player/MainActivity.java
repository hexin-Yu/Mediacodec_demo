package com.example.my_mediacodec_player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private TextureView textureView;
    private Audiodecodethread audiodecodethread;
    private Videodecodethread videodecodethread;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(this);
        path ="test.mp4";
    }
    private AssetFileDescriptor getFileDesciptor(){
        try {
            AssetFileDescriptor afd = getAssets().openFd(path);
            return afd;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Surface surface1 =new Surface(surface);
        audiodecodethread =new Audiodecodethread();
        videodecodethread = new Videodecodethread(surface1);
        audiodecodethread.setDatasource(getFileDesciptor());
        videodecodethread.setDatasource(getFileDesciptor());
        audiodecodethread.parpared();
        videodecodethread.parpared();
        audiodecodethread.start();
        videodecodethread.start();

    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }
}