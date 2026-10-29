package com.tangem.core.analytics.handlers.appsflyer

import android.app.Activity
import android.content.Intent
import com.appsflyer.AppsFlyerLib
import javax.inject.Inject

class AppsFlyerIntentProcessor @Inject internal constructor() {

    /** Lets the SDK resolve a OneLink delivered to an already running [activity]. */
    fun process(intent: Intent, activity: Activity) {
        AppsFlyerLib.getInstance().performOnDeepLinking(intent, activity)
    }
}