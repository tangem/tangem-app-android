package com.tangem.tap.network.auth

import com.tangem.lib.auth.AppVersionProvider
import com.tangem.wallet.BuildConfig

internal class DefaultAppVersionProvider : AppVersionProvider {

    override fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}
