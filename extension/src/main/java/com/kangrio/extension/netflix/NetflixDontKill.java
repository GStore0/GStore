package com.kangrio.extension.netflix;

import com.kangrio.extension.LogUtils;

public class NetflixDontKill {
    private static String TAG = "NetflixDontKill";

    public static void init() {
        try {
            System.loadLibrary("netflix_dont_kill");
        } catch (Throwable e) {
            LogUtils.w(TAG, "init: " + e.getMessage());
        }
    }
}