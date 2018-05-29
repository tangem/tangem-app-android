package com.tangem.domain.cardReader;

/**
 * Created by dvol on 07.03.2018.
 */

public class SettingsMask {
    public static final int IsReusable = 0x0001;
    public static final int UseActivation = 0x0002;
    public static final int UseBlock = 0x0008;

    public static final int AllowSwapPIN = 0x0010;
    public static final int AllowSwapPIN2 = 0x0020;
    public static final int UseCVC = 0x0040;
    public static final int ForbidDefaultPIN = 0x0080;

    public static final int UseOneCommandAtTime = 0x0100;
    public static final int UseNDEF = 0x0200;
    public static final int UseDynamicNDEF = 0x0400;
    public static final int SmartSecurityDelay = 0x0800;

    public static final int Protocol_AllowUnencrypted = 0x1000;
    public static final int Protocol_AllowStaticEncryption = 0x2000;

}
