package com.example.cier.cameratest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cier on 2017/11/30.
 */

public class AudioActivity extends AppCompatActivity {

    private ToggleButton recordBt;
    private Button start;
    private MediaRecorder mRecorder;
    private File dir, mFile;
    public static final String permissions[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    public static final int REQUEST_CODE = 0x001;
    private static boolean isPlaying = false;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_activity);

        getPermission();
        init();
    }

    private void init() {
        recordBt = (ToggleButton) findViewById(R.id.toggleButton);
        start = (Button) findViewById(R.id.button4);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            /**
             *  播放完成时回调
             */
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPlaying = false;
                start.setText("播放");
                mediaPlayer.reset();

            }
        });
        recordBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //录音
                    try {
                        mFile = new File(dir, "sound.amr");
                        if (!mFile.exists()) {
                            mFile.createNewFile();
                        }
                        mRecorder = new MediaRecorder();

                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                        mRecorder.setOutputFile(mFile.getAbsolutePath());

                        mRecorder.prepare();
                        mRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else {
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                }
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //播放音频
                if (!isPlaying) {
                    isPlaying = true;
                    start.setText("停止");
                    try {
                        mediaPlayer.setDataSource(mFile.getAbsolutePath());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    isPlaying = false;
                    start.setText("播放");
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
            }
        });
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> per = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    per.add(permissions[i]);
                }
            }
            if (per.size() != 0) {
                String request[] = new String[per.size()];
                for (int i = 0; i < per.size(); i++)
                    request[i] = per.get(i);
                ActivityCompat.requestPermissions(this, request, REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "未取得相应权限: " + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
        dirInit();
    }

    private void dirInit() {
        dir = new File(Environment.getExternalStorageDirectory(), "111");
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
}
