package com.duoyi.provider.robufix;

import android.content.Context;

/**
 * Created by duoyi on 2016/11/14.
 */

public class SampleApplicationLike {
    public Context context;

    public void onCreate() {

    }


    public SampleApplicationLike(Context context){
        this.context = context;
    }

    public static SampleApplicationLike instance;

    public static SampleApplicationLike getInstance(Context context){
        if (instance == null){
            instance = new SampleApplicationLike(context);
        }
        return instance;
    }
}
