package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import com.tangem.datasource.local.logs.AppLogsStore

/**
 * CardSDK logger implementation.
 *
 * @property appLogsStore app logs store
 *
[REDACTED_AUTHOR]
 */
internal class TangemCardSDKLogger(
    private val appLogsStore: AppLogsStore,
) : TangemSdkLogger {

    private val messageFormatter: LogFormat = LogFormat.StairsFormatter()

    override fun log(message: () -> String, level: Log.Level) {
        if (!LEVELS.contains(level)) return

        appLogsStore.saveLogMessage(
            tag = "CardSDK_${level.name}",
            message = messageFormatter.format(message = message, level = level),
        )
    }

    private companion object {
        val LEVELS = listOf(
            Log.Level.ApduCommand,
            Log.Level.Apdu,
            Log.Level.Tlv,
            Log.Level.Nfc,
            Log.Level.Command,
            Log.Level.Session,
            Log.Level.View,
            Log.Level.Network,
            Log.Level.Error,
            Log.Level.Biometric,
            Log.Level.Info,
        )
    }
}