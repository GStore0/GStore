package com.kangrio.extension.creator;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.kangrio.extension.LogUtils;
import com.kangrio.extension.Utils;

public class PackageInfoCreator implements Parcelable.Creator<PackageInfo> {
    private static final String TAG = "PackageInfoCreator";
    Parcelable.Creator<PackageInfo> originalCreator = PackageInfo.CREATOR;

    @Override
    public PackageInfo createFromParcel(Parcel source) {
        PackageInfo packageInfo = originalCreator.createFromParcel(source);

        try {
            Application application = Utils.getApplication();
            if (application == null) {
                LogUtils.e(TAG, "application is null");
                return packageInfo;
            }

            Bundle metaData = null;
            try {
                metaData = application.getPackageManager().getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA).metaData;
            } catch (PackageManager.NameNotFoundException ignore) {
            }
            if (metaData == null) {
                return packageInfo;
            }
            String signatureData = metaData.getString("org.microg.gms.spoofed_certificates");
            if (signatureData == null) {
                return packageInfo;
            }

            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                packageInfo.signatures = new Signature[]{Utils.spoofSignature(packageInfo, signatureData)};
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageInfo.signingInfo != null) {
                Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
                if (signatures != null && signatures.length > 0) {
                    packageInfo.signingInfo = Utils.spoofSigningInfo(packageInfo, signatureData);
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "createFromParcel", e);
        }

        return packageInfo;
    }

    @Override
    public PackageInfo[] newArray(int size) {
        return originalCreator.newArray(size);
    }
}
