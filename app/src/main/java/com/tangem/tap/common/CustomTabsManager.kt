package com.tangem.tap.common

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.getColorCompat
import com.tangem.wallet.R

class CustomTabsManager {
    fun openUrl(url: String, context: Context) {
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

        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}