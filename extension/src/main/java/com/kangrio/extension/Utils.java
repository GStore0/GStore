package com.kangrio.extension;

import android.annotation.TargetApi;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class Utils {
    private static String TAG = "Utils";

    private static List<String> KNOWN_GOOGLE_PACKAGES = Arrays.asList(
            "com.google.android.gms",
            "com.android.vending",
            "com.google.android.gsf"
    );

    public static String fakeGoogleSignatureData =
            "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMw\n" +
                    "EQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29n\n" +
                    "bGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEz\n" +
                    "MzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYw\n" +
                    "FAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5k\n" +
                    "cm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtW\n" +
                    "LgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKke\n" +
                    "dxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjA\n" +
                    "sb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/Cx\n" +
                    "URaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJE\n" +
                    "qO4k//0zOHKrUiGYXtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/T\n" +
                    "gt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9jMIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYD\n" +
                    "VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIG\n" +
                    "A1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC\n" +
                    "4IdGZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/y\n" +
                    "zKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCE\n" +
                    "yj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+ho5zEHatRbM/YAnqGcFh5iZBqpknHf1S\n" +
                    "KMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQPD+nqActifK\n" +
                    "Z0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3Wf\n" +
                    "MBEmh/9iFBDAaTCK\n";
    private static final ArrayMap<String, Signature> mSignatureCache = new ArrayMap<>();
    private static final ArrayMap<String, SigningInfo> mSigningInfoCache = new ArrayMap<>();

    public static Signature spoofSignature(PackageInfo packageInfo, String signatureData) {
        Signature cache = mSignatureCache.get(packageInfo.packageName);
        if (cache != null) {
            Log.d(TAG, "[CACHE] Spoofed signature: " + packageInfo.packageName + " " + sigToShar1(cache));
            return cache;
        }

        if (KNOWN_GOOGLE_PACKAGES.contains(packageInfo.packageName)) {
            signatureData = fakeGoogleSignatureData;
        }

        byte[] signatureByte = Base64.decode(signatureData, Base64.DEFAULT);
        Signature signature = new Signature(signatureByte);

        Log.d(TAG, "Spoofed signature: " + packageInfo.packageName + " " + sigToShar1(signature));
        mSignatureCache.put(packageInfo.packageName, signature);
        return signature;
    }

    @TargetApi(android.os.Build.VERSION_CODES.P)
    public static SigningInfo spoofSigningInfo(PackageInfo packageInfo, String signatureData) {
        SigningInfo cache = mSigningInfoCache.get(packageInfo.packageName);
        if (cache != null) {
            return cache;
        }

        SigningInfo signingInfo = packageInfo.signingInfo;
        Signature[] signaturesArray = signingInfo.getApkContentsSigners();
        if (signaturesArray != null && signaturesArray.length > 0) {
            signaturesArray[0] = spoofSignature(packageInfo, signatureData);
        }
        mSigningInfoCache.put(packageInfo.packageName, signingInfo);
        return signingInfo;
    }

    public static String sigToShar1(Signature sig) {
        // to shar1 string
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(sig.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static Field getField(Class<?> cls, String fieldName) {
        Class<?> currentClass = cls;
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    public static Object getFieldValue(Class<?> cls, String fieldName, Object instance) {
        try {
            Field field = getField(cls, fieldName);
            if (field == null) {
                return null;
            }

            return field.get(instance);
        } catch (Throwable e) {
            return null;
        }
    }
}
