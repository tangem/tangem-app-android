package com.tangem.tap.data

import android.os.Build
import com.tangem.utils.info.AppInfoProvider
import com.tangem.wallet.BuildConfig
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

internal class DefaultAppInfoProvider @Inject constructor() : AppInfoProvider {
    override val platform: String
        get() = "Android"
    override val device: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val osVersion: String
        get() = Build.VERSION.RELEASE
    override val sdkVersion: Int
        get() = Build.VERSION.SDK_INT
    override val language: String
        get() = Locale.getDefault().toLanguageTag()
    override val timezone: String
        get() = TimeZone.getDefault().id
    override val appVersion: String = BuildConfig.VERSION_NAME
    override val appVersionCode: Int = BuildConfig.VERSION_CODE
    override val isHuaweiDevice: Boolean
        get() = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
            Build.BRAND.equals("HUAWEI", ignoreCase = true)
}