package com.tangem.feature.swap.router

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import java.lang.ref.WeakReference

class CustomTabsManager(private val context: WeakReference<Context>) {
    fun openUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().build(),
            )
            .build()
        context.get()?.let { customTabsIntent.launchUrl(it, Uri.parse(url)) }
    }
}
