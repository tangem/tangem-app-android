package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.WeakHashMap
import kotlin.reflect.KClass

class ForegroundActivityObserver {

    private val activities = WeakHashMap<KClass<out Activity>, AppCompatActivity>()

    val foregroundActivity: AppCompatActivity?
        get() = activities.entries
            .firstOrNull { it.value?.isDestroyed == false }
            ?.value

    internal val callbacks: ActivityLifecycleCallbacks
        get() = Callbacks()

    internal inner class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityResumed(activity: Activity) {
            activities[activity::class] = activity as? AppCompatActivity
        }

        override fun onActivityDestroyed(activity: Activity) {
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

fun ForegroundActivityObserver.withForegroundActivity(block: (AppCompatActivity) -> Unit) {
    foregroundActivity?.let { block(it) }
}