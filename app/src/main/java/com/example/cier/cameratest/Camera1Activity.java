package com.example.cier.cameratest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class Camera1Activity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "Camera1Activity";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private ImageView show;
    private int viewWidth,viewHeight;//width and height of mSurfaceView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        if(!checkCameraHardware(this)){
            Toast.makeText(this, "不支持相机", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            Toast.makeText(this, "摄像头个数 "+ Camera.getNumberOfCameras(), Toast.LENGTH_SHORT).show();
        }

        initView();
    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_DENIED){
            if(Build.VERSION.SDK_INT>=23){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
            }
        }
    }

    private void initView(){
        show= (ImageView) findViewById(R.id.show);
        mSurfaceView= (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder=mSurfaceView.getHolder();
        //mSurfView不需要自己的缓冲区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //mSurfaceView添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback2(){

            /**
             * surfaceView 创建
             */
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //初始化Camera
                initCamera();
            }

            /**
             * surfaceView 方向发生变化
             */
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            /**
             * surfaceView 销毁
             */
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //释放Camera资源
                if(mCamera!=null){
                    mCamera.stopPreview();
                    mCamera.release();
                }
            }

            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {

            }
        });

        mSurfaceView.setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mSurfaceView!=null){
            viewWidth=mSurfaceView.getWidth();
            viewHeight=mSurfaceView.getHeight();
        }
    }

    private void initCamera(){
        mCamera=Camera.open();//默认开启后置摄像头
        mCamera.setDisplayOrientation(90);
        if(mCamera!=null){
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewFpsRange(viewWidth, viewHeight);
                //设置相机预览照片帧数
                parameters.setPreviewFpsRange(4, 10);
                //设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);
                //设置图片的质量
                parameters.set("jpeg-quality", 90);
                //设置照片的大小
                parameters.setPictureSize(viewWidth, viewHeight);
                //通过SurfaceView显示预览
                mCamera.setPreviewDisplay(mSurfaceHolder);
                //开始预览
                mCamera.startPreview();
            }catch (Exception e){

            }
        }
    }

    @Override
    public void onClick(View v) {
        if(mCamera==null){
            Log.e(TAG,"mCamera=null");
            return;
        }
        Log.i(TAG,"onClick");
        mCamera.autoFocus(autoFocusCallback);
    }

    /**
     * 自动对焦，对焦成功后，就拍照
     */
    Camera.AutoFocusCallback autoFocusCallback=new Camera.AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success){
                camera.takePicture(new Camera.ShutterCallback(){
                    /**
                     * 按下快门瞬间的操作
                     */
                    @Override
                    public void onShutter() {

                    }
                },new Camera.PictureCallback(){
                    /**
                     * 是否保存原始图片的信息
                     * @param data
                     * @param camera
                     */
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                },pictureCallback);
            }
        }
    };

    /**
     * 获取图片
     */
    Camera.PictureCallback pictureCallback=new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap resource= BitmapFactory.decodeByteArray(data,0,data.length);
            if(resource==null){
            }
            final Matrix matrix=new Matrix();
            matrix.setRotate(90);
            final Bitmap bitmap=Bitmap.createBitmap(resource,0,0,resource.getWidth(),
                    resource.getHeight(),matrix,true);
            if(bitmap!=null&&show!=null&&show.getVisibility()==View.GONE){
                mCamera.stopPreview();
                show.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
                Toast.makeText(Camera1Activity.this, "capture success", Toast.LENGTH_SHORT).show();
                show.setImageBitmap(bitmap);
            }
        }
    };

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
