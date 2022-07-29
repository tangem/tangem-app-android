package com.tangem.tap.common.feedback

import android.content.Context
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.logConfig
import com.tangem.tap.withForegroundActivity
import com.tangem.wallet.R
import com.zendesk.logger.Logger
import timber.log.Timber
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.configurations.Configuration
import zendesk.messaging.MessagingActivity
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

/**
* [REDACTED_AUTHOR]
 */
class FeedbackManager(
    val infoHolder: AdditionalFeedbackInfo,
    private val logCollector: TangemLogCollector,
) {
    fun initChat(
        context: Context,
        zendeskConfig: ZendeskConfig,
    ) {
        Chat.INSTANCE.init(
            context,
            zendeskConfig.accountKey,
            zendeskConfig.appId
        )

        // Zendesk logs
        Logger.setLoggable(logConfig.zendesk)
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
                onFail = onFail
            )
        }
    }

    fun openChat(feedbackData: FeedbackData) {
        feedbackData.prepare(infoHolder)
        foregroundActivityObserver.withForegroundActivity { activity ->
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

    private fun setChatVisitorNote(
        context: Context,
        feedbackData: FeedbackData,
    ) {
        Chat.INSTANCE.providers()
            ?.profileProvider()
            ?.setVisitorNote(feedbackData.joinTogether(context, infoHolder))
    }

    private fun showMessagingActivity(context: Context) {
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
