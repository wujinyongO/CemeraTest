package com.example.cier.cameratest;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by user on 2017/11/28.
 */

public class Camera1Preview extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public Camera1Preview(Context context, Camera camera) {
        super(context);
        mCamera=camera;

        mHolder=getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        Log.i(TAG,"mCamera.startPreview");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //if preview holder does not exist,return
        if(mHolder.getSurface()==null){
            return;
        }

        //before restart preview,if must has stoped
        try {
            mCamera.stopPreview();
        }catch(Exception e){

        }

        //restart preview after stop it
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
