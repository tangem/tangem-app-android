package com.tangem.tangemtest

import android.app.Application
import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.ItemLogger
import com.tangem.tangemtest.commons.TangemLogger
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class AppTangemDemo : Application() {

    override fun onCreate() {
        super.onCreate()

        AppTangemDemo.appInstance = this
        setupLoggers()
    }

    private fun setupLoggers() {
        Log.setLogger(TangemLogger())
        ILog.setLogger(ItemLogger())
    }

    companion object {
        lateinit var appInstance: AppTangemDemo
    }
}
