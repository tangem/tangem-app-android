package com.tangem.domain.feedback

import android.content.res.Resources
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.SupportFeedbackEmail
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.skipLine
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Get email with feedback for support
 *
 * @property feedbackRepository feedback repository
 * @property resources          resources for getting strings
 *
[REDACTED_AUTHOR]
 */
class GetSupportFeedbackEmailUseCase(
    private val feedbackRepository: FeedbackRepository,
    private val resources: Resources,
) {

    // 00.00 00:00:00.000
    private val dateFormatter = DateTimeFormatterBuilder()
        .appendDayOfMonth(2)
        .appendLiteral('.')
        .appendMonthOfYear(2)
        .appendLiteral(' ')
        .appendHourOfDay(2)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .appendLiteral(':')
        .appendSecondOfMinute(2)
        .appendLiteral('.')
        .appendMillisOfSecond(3)
        .toFormatter()
        .withLocale(Locale.getDefault())

    suspend operator fun invoke(): SupportFeedbackEmail {
        val cardInfo = feedbackRepository.getCardInfo()

        return SupportFeedbackEmail(
            address = getEmail(isStart2Coin = cardInfo.isStart2Coin),
            subject = getSubject(isStart2Coin = cardInfo.isStart2Coin),
            message = createMessage(cardInfo),
            file = feedbackRepository.createLogFile(logs = getLogs()),
        )
    }

    private fun getEmail(isStart2Coin: Boolean): String {
        return if (isStart2Coin) START2COIN_SUPPORT_EMAIL else TANGEM_SUPPORT_EMAIL
    }

    private fun getSubject(isStart2Coin: Boolean): String {
        return resources.getString(
            if (isStart2Coin) {
                R.string.feedback_subject_support
            } else {
                R.string.feedback_subject_support_tangem
            },
        )
    }

    private suspend fun createMessage(cardInfo: CardInfo): String {
        return StringBuilder().apply {
            append(resources.getString(R.string.feedback_preface_support))
            skipLine()
            append(resources.getString(R.string.feedback_data_collection_message))
            skipLine()
            append(
                FeedbackDataBuilder().apply {
                    addUserWalletsInfo(userWalletsInfo = feedbackRepository.getUserWalletsInfo())
                    addDelimiter()
                    addCardInfo(cardInfo)
                    addDelimiter()
                    addBlockchainInfoList(blockchainInfoList = feedbackRepository.getBlockchainInfoList())
                    addDelimiter()
                    addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
                }.build(),
            )
        }.toString()
    }

    private suspend fun getLogs(): String {
        val builder = StringBuilder()

        var sum = 0
        val appLogs = feedbackRepository.getAppLogs()
        for (i in appLogs.lastIndex downTo 0) {
            val log = appLogs[i]
            val date = dateFormatter.print(DateTime(log.timestamp))

            val formattedLog = "$date: ${log.message}\n"

            sum += formattedLog.length
            if (sum < GMAIL_MAX_FILE_SIZE) {
                builder.insert(0, formattedLog)
            } else {
                break
            }
        }

        return builder.toString()
    }

    private companion object {
        const val START2COIN_SUPPORT_EMAIL = "cardsupport@start2coin.com"
        const val TANGEM_SUPPORT_EMAIL = "support@tangem.com"
        const val GMAIL_MAX_FILE_SIZE = 24_900_000 // ≈ 25 MB
    }
}