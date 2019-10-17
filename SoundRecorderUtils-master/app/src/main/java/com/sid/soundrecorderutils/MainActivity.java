package com.sid.soundrecorderutils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.sid.soundrecorderutils.help.ScreenBroadcastListener;
import com.sid.soundrecorderutils.help.ScreenManager;
import com.sid.soundrecorderutils.network.APIForegroundService;

import java.util.List;

/**
 * Created by lv on 2019/09/26.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ScreenManager screenManager = ScreenManager.getInstance(MainActivity.this);
        ScreenBroadcastListener listener = new ScreenBroadcastListener(this);
        listener.registerListener(new ScreenBroadcastListener.ScreenStateListener() {
            @Override
            public void onScreenOn() {
                screenManager.finishActivity();
            }

            @Override
            public void onScreenOff() {
                screenManager.startActivity();
            }
        });
        initView();
    }

    private void initView() {
        findViewById(R.id.main_btn_record_sound).setOnClickListener(this);
        findViewById(R.id.main_btn_play_sound).setOnClickListener(this);
        findViewById(R.id.main_btn_stop_service).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.main_btn_record_sound) {
            final RecordAudioDialogFragment fragment = RecordAudioDialogFragment.newInstance();
            fragment.show(getSupportFragmentManager(), RecordAudioDialogFragment.class.getSimpleName());
            fragment.setOnCancelListener(new RecordAudioDialogFragment.OnAudioCancelListener() {
                @Override
                public void onCancel() {
                    fragment.dismiss();
                }
            });
        } else if (id == R.id.main_btn_play_sound) {
            Intent intent2 = new Intent(this, APIForegroundService.class);
            intent2.putExtra("flags", "233333");
            startService(intent2);
        } else if (id == R.id.main_btn_stop_service) {
            Intent intent2 = new Intent(this, APIForegroundService.class);
            stopService(intent2);
            boolean result = isServiceRunning(this,"com.sid.soundrecorderutils.network.APIForegroundService");
            Log.e("sr_up","result:"+result);
        }
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
}
