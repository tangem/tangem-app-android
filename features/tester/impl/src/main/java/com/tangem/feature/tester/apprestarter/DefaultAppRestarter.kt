package com.tangem.feature.tester.apprestarter

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.tangem.feature.tester.ActivityClassWrapper
import com.tangem.features.tester.api.AppRestarter

/**
 * Entity that kills the process and restarts the main activity
 * @property context Activity context
 */
internal class DefaultAppRestarter(
    private val context: Context,
    private val activityClassWrapper: ActivityClassWrapper,
) : AppRestarter {

    override fun restart() {
        if (context !is Activity) return

        context.finish()
        context.startActivity(
            Intent(context, activityClassWrapper.clazz).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
        )
        Runtime.getRuntime().exit(0)
    }
}
