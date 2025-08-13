package com.tangem.datasource.info

import android.os.Build
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import java.util.*
import javax.inject.Inject

internal class AndroidAppInfoProvider @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
) : AppInfoProvider {
    override val platform: String
        get() = "Android"
    override val device: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val osVersion: String
        get() = Build.VERSION.RELEASE
    override val language: String
        get() = Locale.getDefault().language
    override val timezone: String
        get() = TimeZone.getDefault().id
    override val appVersion: String
        get() = appVersionProvider.versionName
    override val isHuaweiDevice: Boolean
        get() = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
            Build.BRAND.equals("HUAWEI", ignoreCase = true)
}