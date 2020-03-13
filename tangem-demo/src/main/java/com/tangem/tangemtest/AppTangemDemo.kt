package com.tangem.tangemtest

import android.app.Application
import com.tangem.tangemtest.commons.TangemLogger
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class AppTangemDemo : Application() {

    override fun onCreate() {
        super.onCreate()

        setLogger()
    }

    private fun setLogger() {
        Log.setLogger(TangemLogger())
    }
}
