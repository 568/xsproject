package com.sid.soundrecorderutils.help;

import android.app.Activity;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Created by lv on 19-10-15.
 */

public class ScreenManager {
    private Context mContext;

    private WeakReference<Activity> mActivityWref;

    public static ScreenManager gDefualt;

    public static ScreenManager getInstance(Context context) {
        if (gDefualt == null) {
            gDefualt = new ScreenManager(context.getApplicationContext());
        }
        return gDefualt;
    }
    private ScreenManager(Context context) {
        this.mContext = context;
    }

    public void setActivity(Activity pActivity) {
        mActivityWref = new WeakReference<Activity>(pActivity);
    }

    public void startActivity() {
        SinglePixelActivity.actionToSinglePixelActivity(mContext);
    }

    public void finishActivity() {
        //结束掉SinglePixelActivity
        if (mActivityWref != null) {
            Activity activity = mActivityWref.get();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
