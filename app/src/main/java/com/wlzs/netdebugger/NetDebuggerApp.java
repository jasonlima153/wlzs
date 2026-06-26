package com.wlzs.netdebugger;

import android.app.Application;

public class NetDebuggerApp extends Application {

    private static NetDebuggerApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static NetDebuggerApp getInstance() {
        return instance;
    }
}
