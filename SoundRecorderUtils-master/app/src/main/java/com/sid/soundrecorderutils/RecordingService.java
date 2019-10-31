package com.sid.soundrecorderutils;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.carmelo.library.KeepLiveManager;
import com.sid.soundrecorderutils.help.AssistService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

/**
 * 录音的 Service
 * <p>
 * Created by lv on 2019/09/26.
 * 1.利用 MediaRecoder 这个类，进行声音的记录
 * 2.在 onStartCommand() 里面有一个 startRecording() 方法，在外部启动这个 RecordingService 的时候，便会调用这个 startRecording() 方法开始录音。
 * -2.1:初始化录音文件的名字和保存的路径
 * -2.2:对 mRecorder 进行一系列参数的设置，这个 mRecorder 是 MediaRecorder 的一个实例，专门用于录音的存储。
 * 3.等到录音结束，停止服务后，便会回调 RecordingService 的 onDestroy() 方法，这时候便会调用 stopRecording() 方法，
 * 关闭 mRecorder，并用 SharedPreferences 保存录音文件的信息，最后将 mRecorder 置空，防止内存泄露
 */

public class RecordingService extends Service implements MediaRecorder.OnInfoListener {

    private String mFileName = null;
    private String mFilePath = null;

    private MediaRecorder mRecorder = null;

    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;
    private TimerTask mIncrementTimerTask = null;

    public static volatile boolean isRecordSuccess = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KeepLiveManager.getInstance().setServiceForeground(this);
        startRecording();
        setForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("sr_up", "录音服务中断了!!!!!!!");
//        if (mRecorder != null) {
//            stopRecording();
//        }
        Intent intent = new Intent(this, RecordingService.class);
        startService(intent);
        super.onDestroy();
    }

    /*
    * 将当前的时间赋值给 mStartingTimeMills，这里主要是为了记录录音的时长，等到录音结束后再获取一次当前的时间，然后将两个时间进行相减，就能得到录音的具体时长了。
    * */
    public void startRecording() {
        setFileNameAndPath();
        if (mRecorder == null)
            mRecorder = new MediaRecorder();
        if (isRecordSuccess) {
            mRecorder.reset();
        }
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(192000);
        mRecorder.setMaxDuration(8 * 60 * 1000);
        mRecorder.setOnInfoListener(this);
        isRecordSuccess = true;
        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();
            Log.e("sr_up", "recording start......");
        } catch (Exception e) {
            e.printStackTrace();
            isRecordSuccess = false;
            Log.e("sr_up", "prepare() failed" + e.toString());
            startRecording();
        }

    }

    public void setFileNameAndPath() {
        int count = 0;
        File f;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String todayDate = sdf.format(new Date());
        do {
            count++;
            mFileName = (System.currentTimeMillis()) + ".mp4";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" + todayDate + "/" + mFileName;
//            mFilePath += "/SoundRecorder/" + mFileName;
            f = new File(mFilePath);
        } while (f.exists() && !f.isDirectory());
    }

    public void stopRecording() {
        if (null != mRecorder)
            mRecorder.stop();
        mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
        mRecorder.release();

        getSharedPreferences("sp_name_audio", MODE_PRIVATE)
                .edit()
                .putString("audio_path", mFilePath)
                .putLong("elpased", mElapsedMillis)
                .apply();
        if (mIncrementTimerTask != null) {
            mIncrementTimerTask.cancel();
            mIncrementTimerTask = null;
        }

        mRecorder = null;
    }

    /**
     * MediaRecorder的reset() 方法，可以重置参数，继续录音，结合setMaxDuration和 MediaRecorder.OnInfoListener
     * 来监听当时间间隔达到时捕获事件变可实现音频文件每隔一段时间存储一次
     */
    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int i1) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            mRecorder.reset();//reset
            startRecording();
            Log.e("sr_up", "新的一段....");
        }
    }


    // 要注意的是android4.3之后Service.startForeground() 会强制弹出通知栏，解决办法是再
    // 启动一个service和推送共用一个通知栏，然后stop这个service使得通知栏消失。
    private void setForeground() {

        if (mServiceConnection == null) {
            mServiceConnection = new AssistServiceConnection();
        }
        // 绑定另外一条Service，目的是再启动一个通知，然后马上关闭。以达到通知栏没有相关通知
        // 的效果
        bindService(new Intent(this, AssistService.class), mServiceConnection,
                Service.BIND_AUTO_CREATE);
    }

    // 启动notification的id，两次启动应是同一个id
    private final static int NOTIFICATION_ID = android.os.Process.myPid();
    private AssistServiceConnection mServiceConnection;


    private class AssistServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Service assistService = ((AssistService.LocalBinder) service)
                    .getService();
            RecordingService.this.startForeground(NOTIFICATION_ID, getNotification());
            assistService.startForeground(NOTIFICATION_ID, getNotification());
            assistService.stopForeground(true);

            RecordingService.this.unbindService(mServiceConnection);
            mServiceConnection = null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "")
                .setContentTitle("服务运行于前台")
                .setContentText("service被设为前台进程")
                .setTicker("service正在后台运行...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(System.currentTimeMillis())
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        return notification;
    }


}