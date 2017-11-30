package com.example.cier.cameratest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;

/**
 * Created by Cier on 2017/11/29.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity implements View.OnClickListener,View.OnLongClickListener {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CODE = 0x001;
    private static final String TAG = "Camera2Activity";

    //为了让照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ImageView show;

    private CameraManager mCameraManager;
    private Handler childHandler, mainhandler;
    private String mCameraID;//摄像头ID，0为后，1为前
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;

    private static String savePath;
    private static int photoIndex = 1;

    private static int photoAmount=10;

    private boolean ready=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.i(TAG,"startThread");
//                while(true) {
//                    if (ready) {
//                        try {
//                            Thread.sleep(500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        takePicture();
//                        Log.i(TAG, "subThread->take photo");
//                    }
//                }
//            }
//        }).start();

        checkPermission();

        initView();

    }

    /**
     * 初始化
     */
    private void initView() {
        savePath=Environment.getExternalStorageDirectory()+"/aaa";
        File dir=new File(Environment.getExternalStorageDirectory(),"aaa");
        if(!dir.exists()){
            dir.mkdirs();
        }

        show = (ImageView) findViewById(R.id.show);

        //mSurfaceView
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnClickListener(this);
        mSurfaceView.setOnLongClickListener(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);

        //mSurfaceView添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback2() {

            /**
             * SurfaceView创建，初始化Camera
             */
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            /**
             * SurfaceView销毁，释放Camera资源
             */
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }

            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {

            }
        });
    }

    /**
     * 初始化 Camera2
     */
    private void initCamera() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        /////childHandler mainHandler
        childHandler = new Handler(handlerThread.getLooper());
        mainhandler = new Handler(getMainLooper());
        mCameraID = CameraCharacteristics.LENS_FACING_FRONT + "";
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            /**
             * 在这里处理得到的临时照片，例如写入储存卡
             * @param reader
             */
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "capture a photo");
                /*mCameraDevice.close();
                mSurfaceView.setVisibility(View.GONE);
                show.setVisibility(View.VISIBLE);
                //获取照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    show.setImageBitmap(bitmap);
                }*/
                File mFile=new File(savePath,"pic"+(photoIndex++)+".jpeg");
                mainhandler.post(new ImageSaver(reader.acquireNextImage(),mFile));

            }
        }, mainhandler);

        //获取摄像头管理器
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                Log.i(TAG, "turning on camera");
                //使用摄像头管理器打开摄像头
                //打开后会回调mStateCallback对象，也就是下方的CameraDevice.StateCallback里的onOpened()函数
                mCameraManager.openCamera(mCameraID, mStateCallback, mainhandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            checkPermission();
            Log.e(TAG, "no camera permission");
            Toast.makeText(this, "no camera permission", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 摄像头创建状态监听
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        /**
         * 打开摄像头时调用
         */
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        /**
         * 关闭摄像头时调用
         */
        @Override
        public void onDisconnected(CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(Camera2Activity.this, "turn on camera fail", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 开始预览
     */
    private void takePreview() {
        try {
            //创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将SurfaceView的Surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
            //创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            //createCaptureSession()方法会回调CameraCaptureSession.StateCallback()，所以重写里面的
            // onConfigured()就可以预览拍照页面
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(),
                    mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                /**
                 * 摄像头已经准备好，可以开始预览了
                 */
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        Log.e(TAG, "mCameraDevice = null");
                        return;
                    }

                    //开始准备预览
                    try {
                        mCameraCaptureSession = session;
                        //自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //打开闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //显示预览
                        //调用setRepeatingRequest()就开始预览了
                        //setRepeatingRequest()会调用CameraCaptureSession.CaptureCallback
                        //重写CameraCaptureSession.CaptureCallback中的
                        //onCaptureCompleted方法，result就是未经过处理的元数据了
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        //
                        CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {

                            }
                        };
                        //
                        ready=true;
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, mCaptureCallback, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(Camera2Activity.this, "相机配置失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    private void takePicture() {
        if (mCameraDevice == null) {
            return;
        }
        //创建拍照时所需的CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            //自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //根据设备方向 计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "get permission fail", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                try {
                    Log.i(TAG, "turning on camera");
                    mCameraManager.openCamera(mCameraID, mStateCallback, mainhandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            Toast.makeText(this, "get permission success", Toast.LENGTH_SHORT).show();
            File dir=new File(Environment.getExternalStorageDirectory(),"aaa");
            if(!dir.exists()){
                dir.mkdirs();
            }
        }
    }


    @Override
    public void onClick(View v) {
        takePicture();
    }

    @Override
    public boolean onLongClick(View v) {
        Log.v(TAG,"long click ---> surfaceView");
        return false;
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
