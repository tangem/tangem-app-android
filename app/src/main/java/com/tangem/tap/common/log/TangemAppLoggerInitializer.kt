package com.tangem.tap.common.log

import android.util.Log
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
 * Tangem app logger
 *
 * @property settingsRepository repository for saving logs
 *
 * @author Andrew Khokhlov on 12/08/2024
 */
class TangemAppLoggerInitializer(
    private val settingsRepository: SettingsRepository,
) {

    /** Initialize */
    fun initialize() {
        if (BuildConfig.LOG_ENABLED) {
            Logger.addLogAdapter(AndroidLogAdapter(TimberFormatStrategy()))
            Timber.plant(tree = createTimberTree(isDebug = true))
        } else {
            Timber.plant(tree = createTimberTree(isDebug = false))
        }
    }

    private fun createTimberTree(isDebug: Boolean): Timber.Tree {
        return object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (isDebug) {
                    Logger.log(priority, tag, message, t)
                }

                if (priority == Log.ERROR || priority == Log.INFO) {
                    settingsRepository.saveLogMessage(message)
                }
            }
        }
    }
}
