package com.tangem.feature.swap.router

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import java.lang.ref.WeakReference

@Deprecated("Replace with CustomTabsUrlOpener")
class CustomTabsManager(private val context: WeakReference<Context>) {

    fun openUrl(url: String) {
        val mContext = context.get() ?: return
        val browserIntent = Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setDataAndType(Uri.fromParts("http", "", null), "text/plain")
        var possibleBrowsers =
            mContext.packageManager.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (possibleBrowsers.isEmpty()) {
            possibleBrowsers =
                mContext.packageManager.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
        }
        if (possibleBrowsers.isNotEmpty()) {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().build(),
                )
                .build()
            customTabsIntent.intent.setPackage(possibleBrowsers[0].activityInfo.packageName)
            context.get()?.let { customTabsIntent.launchUrl(it, Uri.parse(url)) }
        } else {
            val browserIntent2 = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            mContext.startActivity(browserIntent2)
        }
    }
}