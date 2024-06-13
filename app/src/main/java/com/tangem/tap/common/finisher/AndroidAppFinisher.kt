package com.tangem.tap.common.finisher

import android.content.Intent
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.tap.MainActivity
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity

internal class AndroidAppFinisher : AppFinisher {

    override fun finish() {
        foregroundActivityObserver.withForegroundActivity { activity ->
            activity.finish()
        }
    }

    override fun restart() {
        foregroundActivityObserver.withForegroundActivity { activity ->
            activity.finish()

            val intent = Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            activity.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}