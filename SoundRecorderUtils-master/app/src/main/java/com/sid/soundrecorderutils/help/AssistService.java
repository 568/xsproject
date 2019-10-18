package com.sid.soundrecorderutils.help;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by lv on 19-10-18.
 */

public class AssistService  extends Service {
    public AssistService() {
    }

    public class LocalBinder extends Binder
    {
        public AssistService getService()
        {
            return AssistService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }
}
