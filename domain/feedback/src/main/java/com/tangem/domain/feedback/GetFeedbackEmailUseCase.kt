package com.tangem.domain.feedback

import android.content.res.Resources
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmail
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.*

/**
 * Get email with feedback for support
 *
 * @property feedbackRepository feedback repository
 * @property resources          resources for getting strings
 *
* [REDACTED_AUTHOR]
 */
class GetFeedbackEmailUseCase(
    private val feedbackRepository: FeedbackRepository,
    private val resources: Resources,
) {

    private val emailSubjectResolver = EmailSubjectResolver(resources)
    private val emailMessageTitleResolver = EmailMessageTitleResolver(resources)
    private val emailMessageBodyResolver = EmailMessageBodyResolver(feedbackRepository)

    suspend operator fun invoke(type: FeedbackEmailType): FeedbackEmail {
        val formattedLogs = AppLogsFormatter().format(appLogs = feedbackRepository.getAppLogs())

        return FeedbackEmail(
            address = getAddress(type.cardInfo),
            subject = emailSubjectResolver.resolve(type),
            message = createMessage(type),
            file = feedbackRepository.createLogFile(logs = formattedLogs),
        )
    }

    private fun getAddress(cardInfo: CardInfo?): String {
        return if (cardInfo?.isStart2Coin == true) START2COIN_SUPPORT_EMAIL else TANGEM_SUPPORT_EMAIL
    }

    private suspend fun createMessage(type: FeedbackEmailType): String {
        return StringBuilder().apply {
            val title = emailMessageTitleResolver.resolve(type)
            append(title)

            skipLine()

            appendDisclaimerIfNeeded(type)

            val body = emailMessageBodyResolver.resolve(type)
            append(body)
        }.toString()
    }

    private fun StringBuilder.appendDisclaimerIfNeeded(type: FeedbackEmailType): StringBuilder {
        return if (type is FeedbackEmailType.ScanningProblem) {
            this
        } else {
            append(resources.getString(R.string.feedback_data_collection_message))
            skipLine()
        }
    }
}
