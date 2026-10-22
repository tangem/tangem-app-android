package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ForegroundActivityObserver @Inject constructor() : ForegroundActivityProvider {

    // Read from background threads too (push handling), so the list is only ever replaced as a whole
    private val resumedActivities = AtomicReference<List<AppCompatActivity>>(emptyList())

    override val foregroundActivity: AppCompatActivity?
        get() = resumedActivities.get().lastOrNull { activity ->
            TangemLogger.i("foregroundActivity: ${activity::class} | ${activity.isDestroyed}")
            !activity.isDestroyed
        }

    val callbacks: ActivityLifecycleCallbacks = Callbacks()

    private inner class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            TangemLogger.i("onActivityResumed ${activity::class}")
            if (activity is AppCompatActivity) {
                TangemLogger.i("onActivityResumed store activity")
                resumedActivities.updateAndGet { it.without(activity) + activity }
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            TangemLogger.i("onActivityDestroyed")
            resumedActivities.updateAndGet { it.without(activity) }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    private fun List<AppCompatActivity>.without(activity: Activity): List<AppCompatActivity> {
        return filterNot { it === activity }
    }
}