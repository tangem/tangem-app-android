package com.tangem.tangemcard.reader;

/**
 * Created by dvol on 07.03.2018.
 */

public class SettingsMask {
    public static final int IsReusable = 0x0001;
    public static final int UseActivation = 0x0002;
    public static final int ForbidPurgeWallet = 0x0004;
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

    public static final int ProtectIssuerDataAgainstReplay = 0x4000;

    public static final int AllowSelectBlockchain = 0x8000;

    public static final int DisablePrecomputedNDEF = 0x00010000;

    public static String getDescription(int iValue) {
        StringBuilder sb=new StringBuilder();
        sb.append("[");
        if ((iValue & SettingsMask.AllowSwapPIN) != 0) sb.append("AllowSwapPIN, ");
        if ((iValue & SettingsMask.AllowSwapPIN2) != 0)
            sb.append("AllowSwapPIN2, ");
        if ((iValue & SettingsMask.ForbidDefaultPIN) != 0)
            sb.append("ForbidDefaultPIN, ");
        if ((iValue & SettingsMask.IsReusable) != 0) sb.append("IsReusable, ");
        if ((iValue & SettingsMask.Protocol_AllowStaticEncryption) != 0)
            sb.append("Protocol_AllowStaticEncryption, ");
        if ((iValue & SettingsMask.Protocol_AllowUnencrypted) != 0)
            sb.append("Protocol_AllowUnencrypted, ");
        if ((iValue & SettingsMask.SmartSecurityDelay) != 0)
            sb.append("SmartSecurityDelay, ");
        if ((iValue & SettingsMask.UseActivation) != 0)
            sb.append("UseActivation, ");
        if ((iValue & SettingsMask.UseBlock) != 0) sb.append("UseBlock, ");
        if ((iValue & SettingsMask.UseCVC) != 0) sb.append("UseCVC, ");
        if ((iValue & SettingsMask.UseDynamicNDEF) != 0)
            sb.append("UseDynamicNDEF, ");
        if ((iValue & SettingsMask.UseNDEF) != 0) sb.append("UseNDEF, ");
        if ((iValue & SettingsMask.UseOneCommandAtTime) != 0)
            sb.append("UseOneCommandAtTime, ");
        if ((iValue & SettingsMask.ProtectIssuerDataAgainstReplay) != 0)
            sb.append("ProtectIssuerDataAgainstReplay, ");
        if ((iValue & SettingsMask.ForbidPurgeWallet) != 0) sb.append("ForbidPurgeWallet, ");
        if ((iValue & SettingsMask.AllowSelectBlockchain) != 0) sb.append("AllowSelectBlockchain, ");
        if ((iValue & SettingsMask.DisablePrecomputedNDEF) != 0) sb.append("DisablePrecomputedNDEF, ");

        if (sb.length() > 1) sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }

}
