package com.tangem.tap.core.security

import com.dexprotector.rtc.RtcStatus
import com.tangem.security.DeviceSecurityInfoProvider

internal class DefaultDeviceSecurityInfoProvider : DeviceSecurityInfoProvider {
    override val isRooted: Boolean
        get() = RtcStatus.getRtcStatus().root
    override val isBootloaderUnlocked: Boolean
        get() = RtcStatus.getRtcStatus().unlockedBootloader
    override val isXposed: Boolean
        get() = RtcStatus.getRtcStatus().xposed
}