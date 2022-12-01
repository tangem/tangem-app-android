package com.tangem.tap

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.reflect.KClass

class ForegroundActivityObserver : ActivityResultCaller {
    override var activityResultLauncher: ActivityResultLauncher<Intent>? = null
        private set

    private val activities = WeakHashMap<KClass<out Activity>, Activity>()

    val foregroundActivity: Activity?
        get() = activities.entries
            .filterNot { it.value.isDestroyed }
            .firstOrNull()
            ?.value

    internal val callbacks: ActivityLifecycleCallbacks
        get() = Callbacks()

    internal inner class Callbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activityResultLauncher = (activity as? AppCompatActivity)?.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) {
                /* no-op */
            }
        }

        override fun onActivityResumed(activity: Activity) {
            activities[activity::class] = activity
        }

        override fun onActivityDestroyed(activity: Activity) {
            activities.remove(activity::class)
            if (activities.isEmpty()) {
                activityResultLauncher = null
            }
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
