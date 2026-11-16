package com.kangrio.extension;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "extension::";

    public static void d(String tag, String msg) {
        int pid = android.os.Process.myPid();
        Log.d(TAG + tag, "[pid:" + pid + "] " + msg);
    }

    public static void e(String tag, String msg) {
        int pid = android.os.Process.myPid();
        Log.e(TAG + tag, "[pid:" + pid + "] " + msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        int pid = android.os.Process.myPid();
        Log.e(TAG + tag, "[pid:" + pid + "] " + msg, e);
    }

    public static void i(String tag, String msg) {
        int pid = android.os.Process.myPid();
        Log.i(TAG + tag, "[pid:" + pid + "] " + msg);
    }

    public static void v(String tag, String msg) {
        int pid = android.os.Process.myPid();
        Log.v(TAG + tag, "[pid:" + pid + "] " + msg);
    }

    public static void w(String tag, String msg) {
        int pid = android.os.Process.myPid();
        Log.w(TAG + tag, "[pid:" + pid + "] " + msg);
    }

    public static void w(String tag, String msg, Throwable e) {
        int pid = android.os.Process.myPid();
        Log.w(TAG + tag, "[pid:" + pid + "] " + msg, e);
    }
}
