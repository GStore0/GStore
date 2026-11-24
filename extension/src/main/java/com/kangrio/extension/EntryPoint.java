package com.kangrio.extension;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.kangrio.extension.netflix.NetflixDontKill;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class EntryPoint {
    private static String TAG = "EntryPoint";
    static Handler handler = null;

    public static void init() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("L");
            }
            handler = new Handler(Looper.getMainLooper());
            PackageManagerProxy.init();
            NetflixDontKill.init();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Application app = Utils.getApplication();
                    if (app == null) {
                        handler.postDelayed(this, 2000);
                        return;
                    }
                    registerReceiver(app);
                }
            }, 2000);
        } catch (Throwable ignore) {
        }
    }

    private static void registerReceiver(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(new Receiver(), Receiver.INTENT_FILTER, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(new Receiver(), Receiver.INTENT_FILTER);
            }
        } catch (Throwable e) {
            LogUtils.w(TAG, "registerReceiver: ", e);
        }
    }
}
