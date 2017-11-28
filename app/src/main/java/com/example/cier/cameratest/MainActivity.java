package com.example.cier.cameratest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkCameraHardware(this)){
            Toast.makeText(this, "不支持相机", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            Toast.makeText(this, "摄像头个数 "+ Camera.getNumberOfCameras(), Toast.LENGTH_SHORT).show();
        }
    }

    public static Camera getCameraInstance(){
        Camera camera=null;
        try {
            camera=Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    /**
     * check if the phone support camera
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        }else{
            return false;
        }
    }
}
