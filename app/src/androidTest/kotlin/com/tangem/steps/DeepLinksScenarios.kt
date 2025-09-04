package com.tangem.steps

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider

fun openAppByDeepLink(deepLinkUri: String?) {
    val deeplinkScheme = "tangem://wc?uri="
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplinkScheme + deepLinkUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}