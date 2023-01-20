package com.tangem.tap.common.feedback

import android.content.Context
import android.os.Build
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.persistence.PreferencesStorage
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R
import timber.log.Timber
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.chat.ChatProvidersConfiguration
import zendesk.chat.VisitorInfo
import zendesk.configurations.Configuration
import zendesk.messaging.MessagingActivity
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

/**
 * Created by Anton Zhilenkov on 25/02/2021.
 */
class FeedbackManager(
    val infoHolder: AdditionalFeedbackInfo,
    private val logCollector: TangemLogCollector,
    private val preferencesStorage: PreferencesStorage,
) {
    private var lastUsedConfigForInitialization: ZendeskConfig? = null

    var chatInitializer: ((ZendeskConfig) -> Unit)? = null

    fun initChat(zendeskConfig: ZendeskConfig) {
        // prevent double initialization with the same config
        if (lastUsedConfigForInitialization == zendeskConfig) return

        lastUsedConfigForInitialization = zendeskConfig
        chatInitializer?.invoke(zendeskConfig)
    }

    fun sendEmail(feedbackData: FeedbackData, onFail: ((Exception) -> Unit)? = null) {
        feedbackData.prepare(infoHolder)
        foregroundActivityObserver.withForegroundActivity { activity ->
            val fileLog = if (feedbackData is ScanFailsEmail) createLogFile(activity) else null
            activity.sendEmail(
                email = getSupportEmail(),
                subject = activity.getString(feedbackData.subjectResId),
                message = feedbackData.joinTogether(activity, infoHolder),
                file = fileLog,
                onFail = onFail,
            )
        }
    }

    fun openChat(feedbackData: FeedbackData) {
        feedbackData.prepare(infoHolder)
        foregroundActivityObserver.withForegroundActivity { activity ->
            setChatVisitorInfo()
            setChatVisitorNote(activity, feedbackData)
            showMessagingActivity(activity)
        }
    }

    private fun createLogFile(context: Context): File? {
        return try {
            val file = File(context.filesDir, "logs.txt")
            file.delete()
            file.createNewFile()

            val stringWriter = StringWriter()
            logCollector.getLogs().forEach { stringWriter.append(it) }
            val fileWriter = FileWriter(file)
            fileWriter.write(stringWriter.toString())
            fileWriter.close()
            logCollector.clearLogs()
            file
        } catch (ex: Exception) {
            Timber.e(ex, "Can't create the logs file")
            null
        }
    }

    private fun setChatVisitorInfo() {
        if (preferencesStorage.chatFirstLaunchTime == null) {
            preferencesStorage.chatFirstLaunchTime = System.currentTimeMillis()
        }
        val chatUserId = (preferencesStorage.chatFirstLaunchTime.toString() + Build.MODEL).hashCode()
        val visitorInfo = VisitorInfo.builder()
            .withName("User $chatUserId")
            .build()

        Chat.INSTANCE.chatProvidersConfiguration = ChatProvidersConfiguration.builder()
            .withVisitorInfo(visitorInfo)
            .build()
    }

    private fun setChatVisitorNote(
        context: Context,
        feedbackData: FeedbackData,
    ) {
        Chat.INSTANCE.providers()
            ?.profileProvider()
            ?.setVisitorNote(feedbackData.joinTogether(context, infoHolder))
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

    private fun getSupportEmail(): String {
        return if (TapWorkarounds.isStart2CoinIssuer(infoHolder.cardIssuer)) {
            S2C_SUPPORT_EMAIL
        } else {
            DEFAULT_SUPPORT_EMAIL
        }
    }

    companion object {
        const val DEFAULT_SUPPORT_EMAIL = "support@tangem.com"
        const val S2C_SUPPORT_EMAIL = "cardsupport@start2coin.com"
    }
}
