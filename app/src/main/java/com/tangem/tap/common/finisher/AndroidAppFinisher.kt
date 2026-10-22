package com.tangem.tap.common.finisher

import android.content.Context
import android.content.Intent
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.core.decompose.utils.withForegroundActivity
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.tap.MainActivity

internal class AndroidAppFinisher(
    private val appContext: Context,
    private val foregroundActivityProvider: ForegroundActivityProvider,
) : AppFinisher {

    override fun finish() {
        foregroundActivityProvider.withForegroundActivity { activity ->
            activity.finish()
        }
    }

    override fun restart() {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        appContext.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}