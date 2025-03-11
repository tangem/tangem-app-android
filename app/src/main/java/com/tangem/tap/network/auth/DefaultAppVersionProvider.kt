package com.tangem.tap.network.auth

import com.tangem.utils.version.AppVersionProvider
import com.tangem.wallet.BuildConfig

internal class DefaultAppVersionProvider : AppVersionProvider {

    override val versionName: String = BuildConfig.VERSION_NAME

    override val versionCode: Int = BuildConfig.VERSION_CODE
}