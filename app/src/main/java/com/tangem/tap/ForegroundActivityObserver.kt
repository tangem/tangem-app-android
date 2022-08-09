package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import java.util.WeakHashMap
import kotlin.reflect.KClass

class ForegroundActivityObserver {
    private val activities = WeakHashMap<KClass<out Activity>, Activity>()

    val foregroundActivity: Activity?
        get() = activities.entries
            .filterNot { it.value.isDestroyed }
            .firstOrNull()
            ?.value

    val callbacks get() = Callbacks()

    inner class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            activities[activity::class] = activity
        }

        override fun onActivityDestroyed(activity: Activity) {
            activities.remove(activity::class)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
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

fun ForegroundActivityObserver.withForegroundActivity(
    block: (Activity) -> Unit
) {
    foregroundActivity?.let { block(it) }
}