package com.tangem.tap.common.log

import android.util.Log
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
 * Tangem app logger
 *
 * @property appLogsStore app logs store
 *
[REDACTED_AUTHOR]
 */
class TangemAppLoggerInitializer(
    private val appLogsStore: AppLogsStore,
) {

    /** Initialize */
    fun initialize() {
        if (IS_LOG_ENABLED) {
            Logger.addLogAdapter(AndroidLogAdapter(TimberFormatStrategy()))
        }

        Timber.plant(tree = createTimberTree())
    }

    private fun createTimberTree(): Timber.Tree {
        return object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (IS_LOG_ENABLED) {
                    Logger.log(priority, tag, message, t)
                }

                if (PERMITTED_PRIORITY.contains(priority)) {
                    appLogsStore.saveLogMessage(
                        tag = tag ?: "TangemAppLogger",
                        message = message,
                    )
                }
            }
        }
    }

    private companion object {
        val IS_LOG_ENABLED: Boolean = BuildConfig.LOG_ENABLED
        val PERMITTED_PRIORITY = listOf(Log.ERROR, Log.INFO)
    }
}