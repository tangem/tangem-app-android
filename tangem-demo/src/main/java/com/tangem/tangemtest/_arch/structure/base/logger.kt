package com.tangem.tangemtest._arch.structure.base

import ru.dev.gbixahue.eu4d.lib.android.global.log.Logger
import ru.dev.gbixahue.eu4d.lib.android.global.log.TagLogger
import ru.dev.gbixahue.eu4d.lib.android.global.log.profiling.SimpleLogProfiler
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
object ULog {

    private var logger: Logger? = null

    fun setLogger(logger: Logger) {
        ULog.logger = logger
    }

    fun d(from: Any, msg: Any?, value: Any? = null) {
        logger?.d(from, stringOf(msg), value)
    }

    fun w(from: Any, msg: Any?, value: Any? = null) {
        logger?.w(from, stringOf(msg), value)
    }

    fun e(from: Any, msg: Any?, value: Any? = null) {
        logger?.e(from, stringOf(msg), value)
    }
}

class UnitLogger : TagLogger("UNIT") {
    init {
        msProfiler = SimpleLogProfiler()
    }
}