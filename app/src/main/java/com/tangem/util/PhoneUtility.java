package com.tangem.util;

import android.os.Build;

import com.tangem.domain.wallet.DeviceName;

/**
 * Created by Ilia on 20.04.2018.
 */

public class PhoneUtility {
    public static String GetPhoneName()
    {
        return DeviceName.getDeviceName();

    }

    public static String getDeviceInfo() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("----------------------------------------\n");
        stringBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
        stringBuilder.append("ID: ").append(Build.ID).append("\n");
        stringBuilder.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        stringBuilder.append("Brand: ").append(Build.BRAND).append("\n");
        stringBuilder.append("Hardware: ").append(Build.HARDWARE).append("\n");
        stringBuilder.append("Version: ").append(Build.VERSION.RELEASE).append(", ").append(Build.VERSION.INCREMENTAL).append("\n");
        stringBuilder.append("OS: ").append(Build.VERSION.BASE_OS).append("\n");
        stringBuilder.append("SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        stringBuilder.append("BOARD: ").append(Build.BOARD).append("\n");
        stringBuilder.append("FINGERPRINT: ").append(Build.FINGERPRINT).append("\n");
        stringBuilder.append("----------------------------------------\n");

        return stringBuilder.toString();

    }
}
