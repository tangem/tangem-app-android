package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
internal class ForegroundActivityObserver @Inject constructor() : ForegroundActivityProvider {

    private val activities = HashMap<KClass<out Activity>, AppCompatActivity>()

    override val foregroundActivity: AppCompatActivity?
        get() = activities.entries
            .firstOrNull { entry ->
                TangemLogger.i("foregroundActivity: ${entry.key} | ${entry.value.isDestroyed}")
                !entry.value.isDestroyed
            }
            ?.value

    val callbacks: ActivityLifecycleCallbacks = Callbacks()

    private inner class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityResumed(activity: Activity) {
            TangemLogger.i("onActivityResumed ${activity::class}")
            if (activity is AppCompatActivity) {
                TangemLogger.i("onActivityResumed store activity")
                activities[activity::class] = activity
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            TangemLogger.i("onActivityDestroyed")
            activities.remove(activity::class)
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }
    }
}