package com.tangem.core.navigation.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.tangem.utils.buildConfig.AppConfigurationProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DefaultAppStoreOpener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfigurationProvider: AppConfigurationProvider,
) : AppStoreOpener {

    override fun openStorePage() {
        val storeUri: String
        val webUrl: String
        if (appConfigurationProvider.isHuawei()) {
            storeUri = "$HUAWEI_STORE_SCHEME$STORE_PACKAGE_NAME"
            webUrl = "$HUAWEI_WEB_URL$STORE_PACKAGE_NAME"
        } else {
            storeUri = "$GOOGLE_STORE_SCHEME$STORE_PACKAGE_NAME"
            webUrl = "$GOOGLE_WEB_URL$STORE_PACKAGE_NAME"
        }

        openUri(storeUri) || openUri(webUrl)
    }

    private fun openUri(uri: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: ActivityNotFoundException) {
            TangemLogger.e("Unable to open store uri: $uri", e)
            false
        }
    }

    private companion object {
        const val STORE_PACKAGE_NAME = "com.tangem.wallet"
        const val GOOGLE_STORE_SCHEME = "market://details?id="
        const val GOOGLE_WEB_URL = "https://play.google.com/store/apps/details?id="
        const val HUAWEI_STORE_SCHEME = "appmarket://details?id="
        const val HUAWEI_WEB_URL = "https://appgallery.huawei.com/app/"
    }
}