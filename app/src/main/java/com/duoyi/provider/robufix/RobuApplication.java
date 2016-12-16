package com.duoyi.provider.robufix;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.duoyi.provider.library.RobuFix;

public class RobuApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        SampleApplicationLike.getInstance(this).onCreate();

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        RobuFix.loadPatch(base, Environment.getExternalStorageDirectory().getAbsolutePath().concat("/patch.jar"));
    }
}
