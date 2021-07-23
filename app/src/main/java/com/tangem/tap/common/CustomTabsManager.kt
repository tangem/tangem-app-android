package com.tangem.tap.common

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.tangem.tap.common.extensions.getColorCompat
import com.tangem.wallet.R

class CustomTabsManager {
    fun openUrl(url: String, context: Context) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setNavigationBarColor(context.getColorCompat(R.color.toolbarColor))
                    .build()
            )
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}