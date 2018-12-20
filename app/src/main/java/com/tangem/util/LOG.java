package com.tangem.util;

import android.util.Log;

import com.tangem.wallet.BuildConfig;

public final class LOG {
    public static String TAG = "com.tangem.wallet";

    public static void d(String msg) {
        d(null, msg);
    }

    public static void e(String msg) {
        e(null, msg);
    }

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG)
            Log.d(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), msg);
    }

    public static void d(String tag, int msg) {
        if (BuildConfig.DEBUG)
            Log.d(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), String.valueOf(msg));
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.DEBUG)
            Log.i(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), msg);
    }

    public static void i(String tag, int msg) {
        if (BuildConfig.DEBUG)
            Log.i(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), String.valueOf(msg));
    }

    public static void w(String tag, String msg) {
        if (BuildConfig.DEBUG)
            Log.w(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), msg);
    }

    public static void w(String tag, int msg) {
        if (BuildConfig.DEBUG)
            Log.w(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), String.valueOf(msg));
    }

    public static void e(String tag, String msg) {
        if (BuildConfig.DEBUG)
            Log.e(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), msg);
    }

    public static void e(String tag, int msg) {
        if (BuildConfig.DEBUG)
            Log.d(TAG + (tag != null && !tag.equals("") ? "." + tag : ""), String.valueOf(msg));
    }

}