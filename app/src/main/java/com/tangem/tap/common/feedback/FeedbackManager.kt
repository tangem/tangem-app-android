package com.tangem.tap.common.feedback

import android.app.Activity
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.log.TangemLogCollector
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import timber.log.Timber


/**
[REDACTED_AUTHOR]
 */
class FeedbackManager(
    val infoHolder: AdditionalFeedbackInfo,
    private val logCollector: TangemLogCollector,
) {

    private lateinit var activity: Activity

    fun updateActivity(activity: Activity) {
        this.activity = activity
    }

    fun sendEmail(feedbackData: FeedbackData, onFail: ((Exception) -> Unit)? = null) {
        if (!this::activity.isInitialized) return

        feedbackData.prepare(infoHolder)
        val fileLog = if (feedbackData is ScanFailsEmail) createLogFile() else null
        activity.sendEmail(
            email = getSupportEmail(),
            subject = activity.getString(feedbackData.subjectResId),
            message = feedbackData.joinTogether(activity, infoHolder),
            file = fileLog,
            onFail = onFail
        )
    }

    private fun getSupportEmail(): String {
        return if (TapWorkarounds.isStart2CoinIssuer(infoHolder.cardIssuer)) {
            S2C_SUPPORT_EMAIL
        } else {
            DEFAULT_SUPPORT_EMAIL
        }
    }

    private fun createLogFile(): File? {
        return try {
            val file = File(activity.filesDir, "logs.txt")
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
            Timber.e(ex, "Can't create a file for email attachment")
            null
        }
    }

    companion object {
        const val DEFAULT_SUPPORT_EMAIL = "support@tangem.com"
        const val S2C_SUPPORT_EMAIL = "cardsupport@start2coin.com"
    }
}