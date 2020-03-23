package com.tangem.tangemtest

import android.app.Application
import com.tangem.tangemtest._arch.structure.base.ULog
import com.tangem.tangemtest._arch.structure.base.UnitLogger
import com.tangem.tangemtest.commons.TangemLogger
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class AppTangemDemo : Application() {

    override fun onCreate() {
        super.onCreate()

        setupLoggers()
    }

    private fun setupLoggers() {
        Log.setLogger(TangemLogger())
        ULog.setLogger(UnitLogger())
    }
}
