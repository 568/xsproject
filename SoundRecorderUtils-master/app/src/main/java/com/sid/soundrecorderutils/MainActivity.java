package com.sid.soundrecorderutils;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sid.soundrecorderutils.help.JobHandlerService;
import com.sid.soundrecorderutils.help.ScreenBroadcastListener;
import com.sid.soundrecorderutils.help.ScreenManager;
import com.sid.soundrecorderutils.network.APIForegroundService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lv on 2019/09/26.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    View gobackbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        1像素保活，暂时关闭
//        final ScreenManager screenManager = ScreenManager.getInstance(MainActivity.this);
//        ScreenBroadcastListener listener = new ScreenBroadcastListener(this);
//        listener.registerListener(new ScreenBroadcastListener.ScreenStateListener() {
//            @Override
//            public void onScreenOn() {
//                screenManager.finishActivity();
//            }
//
//            @Override
//            public void onScreenOff() {
//                screenManager.startActivity();
//            }
//        });
        initView();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String todayDate = sdf.format(new Date());
        File dfolder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
        if (!dfolder.exists()) {
            //folder /SoundRecorder doesn't exist, create the folder
            dfolder.mkdir();
        }

        File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder/" + todayDate);
        if (!folder.exists()) {
            //folder /SoundRecorder doesn't exist, create the folder
            folder.mkdir();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this
                    , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            openJobService();
        }
    }

    private TextView timeTextView;

    private void initView() {
        timeTextView = findViewById(R.id.record_audio_text_time);
        findViewById(R.id.main_btn_record_sound).setOnClickListener(this);
        findViewById(R.id.main_btn_play_sound).setOnClickListener(this);
        findViewById(R.id.main_btn_stop_service).setOnClickListener(this);
        gobackbtn = findViewById(R.id.gobackbtn);
        gobackbtn.setOnClickListener(this);
        //页面时间的显示
        SharedPreferences sharePreferences = getSharedPreferences("sp_name_audio", MODE_PRIVATE);
        long start_time = sharePreferences.getLong("start_record_time", 0);
        long duration = System.currentTimeMillis() - start_time;
        if (start_time > 0 && duration > 0) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
                    - TimeUnit.MINUTES.toSeconds(minutes);
           // timeTextView.setText(String.format("录音时长　%02d:%02d", minutes,seconds));
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.main_btn_record_sound) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this
                        , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_PHONE_STATE}, 1);
            } else {
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
            }
        } else if (id == R.id.main_btn_play_sound) {
            Intent intent2 = new Intent(this, APIForegroundService.class);
            intent2.putExtra("flags", "233333");
            startService(intent2);
        } else if (id == R.id.main_btn_stop_service) {
            Intent intent2 = new Intent(this, APIForegroundService.class);
            stopService(intent2);
            boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.network.APIForegroundService");
            Log.e("sr_up", "result:" + result);
        } else if (id == R.id.gobackbtn) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    private void openRecordDialog() {
        final RecordAudioDialogFragment fragment = RecordAudioDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), RecordAudioDialogFragment.class.getSimpleName());
        fragment.setOnCancelListener(new RecordAudioDialogFragment.OnAudioCancelListener() {
            @Override
            public void onCancel() {
                fragment.dismiss();
            }
        });
    }

    private void testPlaySound() {
        RecordingItem recordingItem = new RecordingItem();
        SharedPreferences sharePreferences = getSharedPreferences("sp_name_audio", MODE_PRIVATE);
        final String filePath = sharePreferences.getString("audio_path", "");
        long elpased = sharePreferences.getLong("elpased", 0);
        recordingItem.setFilePath(filePath);
        recordingItem.setLength((int) elpased);
        PlaybackDialogFragment fragmentPlay = PlaybackDialogFragment.newInstance(recordingItem);
        fragmentPlay.show(getSupportFragmentManager(), PlaybackDialogFragment.class.getSimpleName());
    }

    /**
     * 校验某个服务是否还存在
     */
    public boolean isServiceRunning(Context context, String serviceName) {
        // 校验服务是否还存在
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo info : services) {
            // 得到所有正在运行的服务的名称
            String name = info.service.getClassName();
            if (serviceName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean mStartRecording = true;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    onRecord(mStartRecording);
                }
                break;
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            Intent intent = new Intent(this, RecordingService.class);
            // start recording
            //mPauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "开始录音...", Toast.LENGTH_SHORT).show();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String todayDate = sdf.format(new Date());
            File dfolder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!dfolder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                dfolder.mkdir();
            }

            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder/" + todayDate);
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }
            //start RecordingService
            boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.RecordingService");
//            if (!RecordingService.isRecordSuccess || !result)
            startService(intent);
            //记录一下开始时间
            getSharedPreferences("sp_name_audio", MODE_PRIVATE)
                    .edit()
                    .putLong("start_record_time", System.currentTimeMillis())
                    .apply();

            //keep screen on while recording
//            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            //开启文件上传服务
            Intent intent2 = new Intent(this, APIForegroundService.class);
            intent2.putExtra("flags", "233333");
            startService(intent2);
            //
            openJobService();
        }
    }

    private void openJobService() {

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, JobHandlerService.class);
        startService(intent);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //重写返回键
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.RecordingService");
        if (!result) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this
                        , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_PHONE_STATE}, 1);
            } else {
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
            }
            //gobackbtn.performClick();
        }
        //页面时间的显示
        SharedPreferences sharePreferences = getSharedPreferences("sp_name_audio", MODE_PRIVATE);
        long start_time = sharePreferences.getLong("start_record_time", 0);
        long duration = System.currentTimeMillis() - start_time;
        if (start_time > 0 && duration > 0) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
                    - TimeUnit.MINUTES.toSeconds(minutes);
            //timeTextView.setText(String.format("录音时长　%02d:%02d", minutes,seconds));
        }
    }

    @Override
    protected void onDestroy() {
        Log.e("sr_up", "--------录音宿主MainActivity被销毁了-----------");
        boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.RecordingService");
        if (!result) {
            Intent localIntent = new Intent(this, RecordingService.class);
            startService(localIntent);
        }
        super.onDestroy();
    }
}
