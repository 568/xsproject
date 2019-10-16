package com.sid.soundrecorderutils;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
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

public class RecordingService extends Service implements MediaRecorder.OnInfoListener{

    private static final String LOG_TAG = "RecordingService";

    private String mFileName = null;
    private String mFilePath = null;

    private MediaRecorder mRecorder = null;

    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;
    private TimerTask mIncrementTimerTask = null;

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
        startRecording();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mRecorder != null) {
            stopRecording();
        }
        super.onDestroy();
    }

    /*
    * 将当前的时间赋值给 mStartingTimeMills，这里主要是为了记录录音的时长，等到录音结束后再获取一次当前的时间，然后将两个时间进行相减，就能得到录音的具体时长了。
    * */
    public void startRecording() {
        setFileNameAndPath();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(192000);
        mRecorder.setMaxDuration(1*60*1000);
        mRecorder.setOnInfoListener(this);
        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void setFileNameAndPath() {
        int count = 0;
        File f;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String todayDate = sdf.format(new Date());
        do {
            count++;
            mFileName = getString(R.string.default_file_name)
                    + "_" + (System.currentTimeMillis()) + ".mp4";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" +todayDate+"/"+ mFileName;
//            mFilePath += "/SoundRecorder/" + mFileName;
            f = new File(mFilePath);
        } while (f.exists() && !f.isDirectory());
    }

    public void stopRecording() {
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
     * */
    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int i1) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            mRecorder.reset();//reset
            startRecording();

        }
    }
}