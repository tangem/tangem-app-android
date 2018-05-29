package com.tangem.domain.wallet;

import android.os.Build;

public class DeviceNFCAntennaLocation {

    public float X;
    public float Y;
    public boolean OnBackSide;
    public float Strength;

    public void getAntennaLocation() {
        String device = DeviceName.getDeviceName();
        String model = Build.DEVICE;
        this.X = 0.5f;
        this.Y = 0.33f;
        this.OnBackSide = true;
        this.Strength = 1.0f;
        // Samsung
//        if (model.contains("Samsung")) {this.Y = 0.33; this.Strength = 0.5; }
//        if (model.contains("Sony")) {this.Y = 0.33; this.Strength = 0.5; }
//        if (device == "Galaxy J5") {this.Y = 0.4; this.Strength = 0.5; }
        if (device == "P10 lite") {this.Y = 0.03f; this.Strength = 0.8f; }
    }
}
