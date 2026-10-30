package com.tangem.core.analytics.handlers.appsflyer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.app.OnNewIntentProvider
import com.appsflyer.AppsFlyerLib
import javax.inject.Inject

/** Lets the SDK resolve a OneLink delivered to an already running activity (warm start). */
internal class AppsFlyerNewIntentObserver @Inject constructor() : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is OnNewIntentProvider) return

        activity.addOnNewIntentListener { intent ->
            AppsFlyerLib.getInstance().performOnDeepLinking(intent, activity)
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}