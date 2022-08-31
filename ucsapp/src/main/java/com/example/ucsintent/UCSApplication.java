package com.example.ucsintent;

import android.app.Application;
import android.content.Context;

/**
 * Class used in order to get applicationContext even outside Activity classes
 */
public class UCSApplication extends Application {
    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
