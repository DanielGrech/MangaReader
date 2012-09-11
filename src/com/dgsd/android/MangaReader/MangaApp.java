package com.dgsd.android.MangaReader;

import android.app.Application;
import android.content.Context;
import com.dgsd.android.MangaReader.Util.Diagnostics;

public class MangaApp extends Application {

    public static Class getHomeClass(Context context) {
        if(Diagnostics.isTablet(context))
            return null;
        else
            return MainActivity.class;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
