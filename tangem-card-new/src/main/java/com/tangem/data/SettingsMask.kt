package com.tangem.data

data class SettingsMask(val rawValue: Int) {
    
    companion object{
        const val IsReusable = 0x0001
        const val UseActivation = 0x0002
        const val ForbidPurgeWallet = 0x0004
        const val UseBlock = 0x0008

        const val AllowSwapPIN = 0x0010
        const val AllowSwapPIN2 = 0x0020
        const val UseCVC = 0x0040
        const val ForbidDefaultPIN = 0x0080

        const val UseOneCommandAtTime = 0x0100
        const val UseNDEF = 0x0200
        const val UseDynamicNDEF = 0x0400
        const val SmartSecurityDelay = 0x0800

        const val Protocol_AllowUnencrypted = 0x1000
        const val Protocol_AllowStaticEncryption = 0x2000

        const val ProtectIssuerDataAgainstReplay = 0x4000

        const val AllowSelectBlockchain = 0x8000

        const val DisablePrecomputedNDEF = 0x00010000

        const val SkipSecurityDelayIfValidatedByLinkedTerminal = 0x00080000
    }
}