package com.tangem.tap.core.security

import com.tangem.spend.datasource.config.TangemPay

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.security.DeviceSecurityInfoProvider

/** In MOCK env reports a clean device; otherwise delegates (DexProtector RTC flags emulators). */
internal class MockAwareDeviceSecurityInfoProvider(
    private val real: DeviceSecurityInfoProvider,
    private val apiConfigsManager: ApiConfigsManager,
) : DeviceSecurityInfoProvider {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override val isRooted: Boolean
        get() = if (isMockMode) false else real.isRooted

    override val isBootloaderUnlocked: Boolean
        get() = if (isMockMode) false else real.isBootloaderUnlocked

    override val isXposed: Boolean
        get() = if (isMockMode) false else real.isXposed

    override val isVulnerableToMediaTekExploit: Boolean
        get() = if (isMockMode) false else real.isVulnerableToMediaTekExploit
}