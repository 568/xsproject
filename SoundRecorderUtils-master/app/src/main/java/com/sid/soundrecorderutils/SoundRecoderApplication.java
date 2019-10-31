package com.sid.soundrecorderutils;

import android.app.Application;
import android.provider.Settings;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by lv on 19-10-14.
 */

public class SoundRecoderApplication extends Application {
    static SoundRecoderApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("platform", "android");
        String androidId =Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        try {
            httpHeaders.put( "anroidid", URLEncoder.encode(androidId, "UTF-8"));
            httpHeaders.put("sdkver", (URLEncoder.encode(android.os.Build.VERSION.SDK, "UTF-8")));
            httpHeaders.put("model", (URLEncoder.encode(android.os.Build.MODEL, "UTF-8")));
            httpHeaders.put("releasever", (URLEncoder.encode(android.os.Build.VERSION.RELEASE, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        OkGo.getInstance().init(this).addCommonHeaders(httpHeaders);
    }

    public static SoundRecoderApplication getInstance(){
        return instance;
    }
}
