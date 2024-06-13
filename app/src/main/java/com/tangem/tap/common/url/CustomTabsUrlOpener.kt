package com.tangem.tap.common.url

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.getColorCompat
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R

internal class CustomTabsUrlOpener : UrlOpener {

    override fun openUrl(url: String) {
        foregroundActivityObserver.withForegroundActivity {
            openUrl(url, context = it)
        }
    }

    private fun openUrl(url: String, context: Context) {
        if (url.isEmpty()) return
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

        // Open CustomTabsActivity as new task without saving into the stack
        customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_NO_HISTORY)

        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}