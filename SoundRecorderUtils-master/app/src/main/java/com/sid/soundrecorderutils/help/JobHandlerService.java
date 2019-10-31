package com.sid.soundrecorderutils.help;

import android.app.ActivityManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sid.soundrecorderutils.RecordingService;
import com.sid.soundrecorderutils.network.APIForegroundService;

import java.util.List;

/**
 * Created by lv on 19-10-21.
 */

public class JobHandlerService extends JobService {
    private JobScheduler mJobScheduler = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startService(this);
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(startId++,
                new ComponentName(getPackageName(), JobHandlerService.class.getName()));
        //builder.setPeriodic(5000); //每隔5秒运行一次--和下面的时间设置方式只能二选一
        //setMinimumLatency和setOverrideDeadline不能同setPeriodic一起使用
        builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
        builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
        builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
        builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
       // builder.setRequiresCharging(true); // 当插入充电器，执行该任务
        builder.setPersisted(true); //重启后是否还要继续执行
        //builder.setRequiresDeviceIdle(true);
       // 重新调度策略setBackoffCriteria无法和设备闲置状态setRequiresDeviceIdle 并存
        mJobScheduler.schedule(builder.build());
        return Service.START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        //启动本地服务
        boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.RecordingService");
        if (!result) {
            Intent localIntent = new Intent(this, RecordingService.class);
            startService(localIntent);
        }
        Log.e("sr_up","onStartJob");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        //启动本地服务
        boolean result = isServiceRunning(this, "com.sid.soundrecorderutils.RecordingService");
        if (!result) {
            Intent localIntent = new Intent(this, RecordingService.class);
            startService(localIntent);
        }
        Log.e("sr_up","onStopJob");

        Intent intent2 = new Intent(this, APIForegroundService.class);
        intent2.putExtra("flags", "233333");
        startService(intent2);

        return false;
    }

    private void startService(Context context) {
        if (!RecordingService.isRecordSuccess) {
            Intent localIntent = new Intent(context, RecordingService.class);
            startService(localIntent);
        }
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
