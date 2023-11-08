package com.tangem.tap.common.chat.opener.implementation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import com.spr.messengerclient.config.SPRMessenger
import com.spr.messengerclient.config.bean.SPRMessengerConfig
import com.tangem.common.extensions.guard
import com.tangem.datasource.config.models.SprinklrConfig
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import timber.log.Timber
import java.io.File
import java.util.Locale

internal class SprinklrChatOpener(
    private val config: SprinklrConfig,
    private val foregroundActivityObserver: ForegroundActivityObserver,
) : ChatOpener {

    override fun open(createFeedbackFile: (Context) -> File?, createLogsFile: (Context) -> File?) {
        val messenger = SPRMessenger.shared()

        if (messenger.config == null) {
            initSprConfig(messenger)
        }

        messenger.startApplication()
    }

    private fun initSprConfig(messenger: SPRMessenger) {
        val application = foregroundActivityObserver.foregroundActivity?.application.guard {
            Timber.e("The SPR chat cannot be opened because there are no activities in foreground")
            return
        }

        messenger.takeOff(application, createSprConfig(application, config))
    }

    @SuppressLint("HardwareIds")
    private fun createSprConfig(application: Application, config: SprinklrConfig): SPRMessengerConfig {
        return SPRMessengerConfig().apply {
            appId = config.appId
            appKey = CHAT_APP_KEY
            deviceId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
            environment = config.environment
            skin = CHAT_SKIN
            locale = Locale.getDefault().language
        }
    }

    private companion object {
        const val CHAT_APP_KEY = "com.sprinklr.messenger.release"
        const val CHAT_SKIN = "MODERN"
    }
}