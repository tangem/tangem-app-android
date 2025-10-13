package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber
import kotlin.reflect.KClass

object ForegroundActivityObserver {

    private val activities = HashMap<KClass<out Activity>, AppCompatActivity>()

    val foregroundActivity: AppCompatActivity?
        get() = activities.entries
            .firstOrNull { entry ->
                Timber.i("foregroundActivity: ${entry.key} | ${entry.value.isDestroyed}")
                entry.value.isDestroyed == false
            }
            ?.value

    internal val callbacks: ActivityLifecycleCallbacks
        get() = Callbacks()

    internal class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityResumed(activity: Activity) {
            Timber.i("onActivityResumed ${activity::class}")
            if (activity is AppCompatActivity) {
                Timber.i("onActivityResumed store activity")
                activities[activity::class] = activity
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            Timber.i("onActivityDestroyed")
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