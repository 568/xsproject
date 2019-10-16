package com.sid.soundrecorderutils.network;

import java.io.Serializable;

/**
 * Created by lv on 19-10-15.
 */

public class VideoItem implements Serializable {
    public String dateTime;
    public String path;
    public String sign;
    public String vcode;
    public VideoItem(String str1,String str2){
        path=str1;
        dateTime=str2;
    }
}
