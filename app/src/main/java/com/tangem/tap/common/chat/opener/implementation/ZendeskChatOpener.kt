package com.tangem.tap.common.chat.opener.implementation

import android.content.Context
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.LogConfig
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.ZendeskConfig
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R
import com.zendesk.logger.Logger
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.chat.ChatProvidersConfiguration
import zendesk.chat.VisitorInfo
import zendesk.configurations.Configuration
import zendesk.messaging.MessagingActivity

internal class ZendeskChatOpener(
    private val userId: String,
    private val config: ZendeskConfig,
    private val foregroundActivityObserver: ForegroundActivityObserver,
) : ChatOpener {
    private var isInitialized = false

    override fun open(feedbackDataBuilder: (Context) -> String) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            initZendeskIfNeeded(activity.applicationContext)
            setChatVisitorInfo()
            setChatVisitorNote(feedbackDataBuilder(activity))
            showMessagingActivity(activity)
        }
    }

    private fun initZendeskIfNeeded(context: Context) {
        if (isInitialized) return
        isInitialized = true

        Chat.INSTANCE.init(context, config.accountKey, config.appId)
        Logger.setLoggable(LogConfig.zendesk)
    }

    private fun setChatVisitorInfo() {
        val visitorInfo = VisitorInfo.builder()
            .withName("User $userId")
            .build()

        Chat.INSTANCE.chatProvidersConfiguration = ChatProvidersConfiguration.builder()
            .withVisitorInfo(visitorInfo)
            .build()
    }

    private fun setChatVisitorNote(note: String) {
        Chat.INSTANCE.providers()
            ?.profileProvider()
            ?.setVisitorNote(note)
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
