package com.tangem.tap.common.url

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.core.net.toUri
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.getColorCompat
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R
import timber.log.Timber

internal class CustomTabsUrlOpener : UrlOpener {

    override fun openUrl(url: String) {
        foregroundActivityObserver.withForegroundActivity {
            openUrl(url, context = it)
        }
    }

    override fun openUrlExternalBrowser(url: String) {
        foregroundActivityObserver.withForegroundActivity { context ->
            if (url.isEmpty()) return@withForegroundActivity
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(browserIntent)
        }
    }

    private fun openUrl(url: String, context: Context) {
        if (url.isEmpty()) return
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        runCatching {
            if (checkCustomTabsAvailability(context, browserIntent)) {
                context.startActivity(browserIntent)
            } else {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams.Builder()
                            .setNavigationBarColor(context.getColorCompat(R.color.toolbarColor))
                            .build(),
                    )
                    .setColorScheme(
                        if (MutableAppThemeModeHolder.isDarkThemeActive) COLOR_SCHEME_DARK else COLOR_SCHEME_LIGHT,
                    )
                    .build()

                customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                customTabsIntent.launchUrl(context, url.toUri())
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Custom Tabs compatibility check. Returns flag whether custom tabs are supported
     * @see "https://developer.chrome.com/docs/android/custom-tabs/howto-custom-tab-check"
     */
    private fun checkCustomTabsAvailability(context: Context, browserIntent: Intent): Boolean {
        // Get all apps that can handle VIEW intents and Custom Tab service connections.
        val resolveInfos = context.packageManager.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)

        // Extract package names from ResolveInfo objects
        val packageNames = mutableListOf<String>()
        for (info in resolveInfos) {
            packageNames.add(info.activityInfo.packageName)
        }

        // Get a package that supports Custom Tabs
        val packageName = CustomTabsClient.getPackageName(context, packageNames, true)

        return packageName == null // Custom Tabs are not supported by any browser on the device
    }
}