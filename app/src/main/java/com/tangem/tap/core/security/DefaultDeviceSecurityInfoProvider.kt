package com.tangem.tap.core.security

import com.dexprotector.rtc.RtcStatus
import com.tangem.security.DeviceSecurityInfoProvider
import timber.log.Timber

internal class DefaultDeviceSecurityInfoProvider : DeviceSecurityInfoProvider {
    override val isRooted: Boolean
        get() = getRtcStatusSafely()?.root == true
    override val isBootloaderUnlocked: Boolean
        get() = getRtcStatusSafely()?.unlockedBootloader == true
    override val isXposed: Boolean
        get() = getRtcStatusSafely()?.xposed == true

    private fun getRtcStatusSafely(): RtcStatus? {
        return try {
            RtcStatus.getRtcStatus()
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
    }
}