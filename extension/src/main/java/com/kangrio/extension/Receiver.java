package com.kangrio.extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.List;

public class Receiver extends BroadcastReceiver {
    private final List<String> PACKAGE_ACTIONS = List.of(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED
    );
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("Receiver", "onReceive: " + action);
        if (action == null) return;

        if (PACKAGE_ACTIONS.contains(action)) {
            Uri data = intent.getData();
            if (data == null) return;

            String packageName = data.getSchemeSpecificPart();
            if (packageName == null) return;

            Utils.clearSpoofCache(packageName);
        }
    }
}
