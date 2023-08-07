package com.tangem.tap.common.chat.opener.implementation

import android.content.Context
import com.tangem.core.analytics.Analytics
import com.tangem.datasource.config.models.ZendeskConfig
import com.tangem.domain.common.LogConfig
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R
import com.zendesk.logger.Logger
import timber.log.Timber
import zendesk.chat.*
import zendesk.configurations.Configuration
import zendesk.messaging.MessagingActivity
import java.io.File

internal class ZendeskChatOpener(
    private val userId: String,
    private val config: ZendeskConfig,
    private val foregroundActivityObserver: ForegroundActivityObserver,
) : ChatOpener {

    private var isInitialized = false
    private var isFilesSent = false

    override fun open(createFeedbackFile: (Context) -> File?, createLogsFile: (Context) -> File?) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            initZendeskIfNeeded(activity.applicationContext)
            setChatVisitorInfo()
            showMessagingActivity(activity)
            sendFeedbackFile(createFeedbackFile(activity))
            sendLogsFile(createLogsFile(activity))
        }
    }

    private fun initZendeskIfNeeded(context: Context) {
        if (isInitialized) return
        isInitialized = true

        Chat.INSTANCE.init(context, config.accountKey, config.appId)
        Logger.setLoggable(LogConfig.zendesk)
    }

    private fun setChatVisitorInfo() {
        val visitorInfo = VisitorInfo.builder().withName("User $userId").build()

        Chat.INSTANCE.chatProvidersConfiguration =
            ChatProvidersConfiguration.builder().withVisitorInfo(visitorInfo).build()
    }

    private fun sendFeedbackFile(feedbackFile: File?) {
        if (isInitialized && feedbackFile != null && !isFilesSent) {
            Chat.INSTANCE.providers()?.chatProvider()?.sendFile(feedbackFile) { _, bytesUploaded, _ ->
                Timber.d("Log file sent", "bytesUploaded: $bytesUploaded")
            }
            isFilesSent = true
        }
    }

    private fun sendLogsFile(logsFile: File?) {
        if (isInitialized && logsFile != null && !isFilesSent) {
            Chat.INSTANCE.providers()?.chatProvider()?.sendFile(logsFile) { _, bytesUploaded, _ ->
                Timber.d("Log file sent", "bytesUploaded: $bytesUploaded")
            }
            isFilesSent = true
        }
    }

    private fun showMessagingActivity(context: Context) {
        Analytics.send(com.tangem.tap.common.analytics.events.Chat.ScreenOpened())
        MessagingActivity.builder()
            .withMultilineResponseOptionsEnabled(false)
            .withBotLabelStringRes(R.string.chat_bot_name)
            .withBotAvatarDrawable(R.mipmap.ic_launcher)
            .withEngines(ChatEngine.engine())
            .show(context, buildChatConfig())
    }

    private fun buildChatConfig(): Configuration {
        return ChatConfiguration.builder()
            .withOfflineFormEnabled(true)
            .withAgentAvailabilityEnabled(true)
            .withPreChatFormEnabled(false)
            .build()
    }
}