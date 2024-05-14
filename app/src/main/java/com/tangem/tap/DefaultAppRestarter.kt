package com.tangem.tap

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.tangem.utils.AppRestarter
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

/**
 * Entity that kills the process and restarts the main activity
 * @property context activity context
 */
class DefaultAppRestarter @Inject constructor(
    @ActivityContext private val context: Context,
) : AppRestarter {

    override fun restart() {
        if (context !is Activity) return

        context.finish()
        context.startActivity(
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
        )
        Runtime.getRuntime().exit(0)
    }
}
