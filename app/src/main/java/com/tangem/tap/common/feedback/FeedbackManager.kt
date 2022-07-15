package com.tangem.tap.common.feedback

import android.content.Context
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import timber.log.Timber
import zendesk.configurations.Configuration
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.Support
import zendesk.support.request.RequestConfiguration
import zendesk.support.requestlist.RequestListActivity
import zendesk.support.requestlist.RequestListConfiguration

/**
[REDACTED_AUTHOR]
 */
class FeedbackManager(
    val infoHolder: AdditionalFeedbackInfo,
    private val logCollector: TangemLogCollector,
) {
    fun initChat(
        context: Context,
        zendeskConfig: ZendeskConfig,
    ) {
        Zendesk.INSTANCE.init(
            /* context = */ context,
            /* zendeskUrl = */ zendeskConfig.url,
            /* applicationId = */ zendeskConfig.appId,
            /* oauthClientId = */ zendeskConfig.clientId,
        )
        Support.INSTANCE.init(Zendesk.INSTANCE)
        Zendesk.INSTANCE.setIdentity(AnonymousIdentity())
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
            RequestListActivity.builder()
                .show(
                    /* context = */ activity,
                    /* configurations = */ buildConfigs(activity, feedbackData)
                )
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

    private fun buildConfigs(
        context: Context,
        feedbackData: FeedbackData,
    ): List<Configuration> {
        return listOf(
            // Request configuration
            RequestConfiguration.Builder()
                .withRequestSubject(context.getString(feedbackData.subjectResId))
                .config(),
            // Request list configuration
            RequestListConfiguration.Builder()
                .withContactUsButtonVisible(true)
                .config(),
        )
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