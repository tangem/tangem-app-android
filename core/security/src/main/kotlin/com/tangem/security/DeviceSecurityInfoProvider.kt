package com.tangem.security

interface DeviceSecurityInfoProvider {
    val isRooted: Boolean
    val isBootloaderUnlocked: Boolean
    val isXposed: Boolean
    val isVulnerableToMediaTekExploit: Boolean
}

fun DeviceSecurityInfoProvider.isSecurityExposed(): Boolean = isRooted || isBootloaderUnlocked || isXposed