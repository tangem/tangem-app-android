package com.tangem.tap.common.feedback

import android.content.Context
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.chat.ChatConfig
import com.tangem.tap.common.chat.ChatManager
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.log.TangemLogCollector
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

/**
* [REDACTED_AUTHOR]
 */
class FeedbackManager(
    val infoHolder: AdditionalFeedbackInfo,
    private val logCollector: TangemLogCollector,
    private val chatManager: ChatManager,
) {

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

    fun openChat(config: ChatConfig, feedbackData: FeedbackData) {
        chatManager.open(config) { context ->
            feedbackData.run {
                prepare(infoHolder)
                joinTogether(context, infoHolder)
            }
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
