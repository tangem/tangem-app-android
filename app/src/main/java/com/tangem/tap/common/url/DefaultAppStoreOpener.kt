package com.tangem.tap.common.url

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.tangem.core.navigation.url.AppStoreOpener
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import com.tangem.utils.buildConfig.AppConfigurationProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultAppStoreOpener @Inject constructor(
    private val appConfigurationProvider: AppConfigurationProvider,
) : AppStoreOpener {

    override fun openStorePage() {
        foregroundActivityObserver.withForegroundActivity { activity ->
            val storeUri: String
            val webUrl: String
            if (appConfigurationProvider.isHuawei()) {
                storeUri = "$HUAWEI_STORE_SCHEME$STORE_PACKAGE_NAME"
                webUrl = "$HUAWEI_WEB_URL$STORE_PACKAGE_NAME"
            } else {
                storeUri = "$GOOGLE_STORE_SCHEME$STORE_PACKAGE_NAME"
                webUrl = "$GOOGLE_WEB_URL$STORE_PACKAGE_NAME"
            }

            openUri(activity, storeUri) || openUri(activity, webUrl)
        }
    }

    private fun openUri(context: Context, uri: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
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