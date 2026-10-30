package com.kangrio.gstore.patch

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.BuildConfig
import com.kangrio.gstore.extensions.generateX509Certificate
import com.kangrio.gstore.extensions.isPAndAbove
import com.kangrio.gstore.patch.util.ApkSignerHelper
import com.kangrio.gstore.util.PackageUtil.getPackageInfo
import java.security.cert.X509Certificate

object CertUtilPatch {
    fun isSignedByAuroraStore(context: Context, packageName: String): Boolean = try {
        getX509Certificates(context, packageName).any { cert ->
            val auroraStoreCert = ApkSignerHelper.getCertificate(context)

            // equal
            cert.publicKey == auroraStoreCert.publicKey
        }
    } catch (exception: Throwable) {
        Log.e(TAG, "Failed to check signing cert for $packageName", exception)
        false
    }

    private fun getX509Certificates(context: Context, packageName: String): List<X509Certificate> =
        try {
            val packageInfo = getPackageInfoWithSignature(context, packageName)
            if (isPAndAbove) {
                if (packageInfo.signingInfo!!.hasMultipleSigners()) {
                    packageInfo.signingInfo!!.apkContentsSigners.map {
                        it.generateX509Certificate()
                    }
                } else {
                    packageInfo.signingInfo!!.signingCertificateHistory.map {
                        it.generateX509Certificate()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures!!.map { it.generateX509Certificate() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get X509 certificates", exception)
            emptyList()
        }

    private fun getPackageInfoWithSignature(context: Context, packageName: String): PackageInfo =
        if (isPAndAbove) {
            getPackageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
        }
}