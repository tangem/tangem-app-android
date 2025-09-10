package com.tangem.tap.common.buildconfig

import com.tangem.utils.buildConfig.AppConfigurationProvider
import com.tangem.wallet.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppConfigurationProviderImpl @Inject constructor() : AppConfigurationProvider {

    override fun isDebug(): Boolean = BuildConfig.BUILD_TYPE == "debug"
    override fun isHuawei(): Boolean = BuildConfig.FLAVOR_NAME == "huawei"
}