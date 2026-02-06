package com.tangem.domain.feedback

import android.content.res.Resources
import com.tangem.core.res.getStringSafe
import com.tangem.domain.feedback.models.FeedbackEmail
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.feedback.utils.*
import java.io.File

private const val BINDER_MAX_SIZE_BYTES = 500_000 // Safe limit under Android's 1MB Binder limit

/**
 * Get email with feedback for support
 *
 * @property feedbackRepository feedback repository
 * @property resources          resources for getting strings
 *
[REDACTED_AUTHOR]
 */
class SendFeedbackEmailUseCase(
    private val feedbackRepository: FeedbackRepository,
    private val resources: Resources,
) {

    private val emailSubjectResolver = EmailSubjectResolver(resources)
    private val emailMessageTitleResolver = EmailMessageTitleResolver(resources)
    private val emailMessageBodyResolver = EmailMessageBodyResolver(feedbackRepository)

    suspend operator fun invoke(type: FeedbackEmailType) {
        val email = FeedbackEmail(
            address = getAddress(type),
            subject = emailSubjectResolver.resolve(type),
            message = createMessage(type),
            file = getFile(type),
        )

        feedbackRepository.sendEmail(email)
    }

    private suspend fun getFile(type: FeedbackEmailType): File? {
        return if (type.isVisaEmail()) {
            null
        } else {
            feedbackRepository.getZipLogFile()
        }
    }

    private fun getAddress(type: FeedbackEmailType): String {
        return when {
            type.isVisaEmail() -> TANGEM_VISA_SUPPORT_EMAIL
            type.walletMetaInfo?.isStart2Coin == true -> START2COIN_SUPPORT_EMAIL
            else -> TANGEM_SUPPORT_EMAIL
        }
    }

    private suspend fun createMessage(type: FeedbackEmailType): String {
        val fullMessage = buildString {
            val title = emailMessageTitleResolver.resolve(type)
            append(title)

            skipLine()

            appendDisclaimerIfNeeded(type)

            val body = emailMessageBodyResolver.resolve(type)
            append(body)
        }

        return truncateMessageIfNeeded(fullMessage)
    }

    private fun truncateMessageIfNeeded(message: String): String {
        val messageBytes = message.toByteArray(Charsets.UTF_8)

        return if (messageBytes.size > BINDER_MAX_SIZE_BYTES) {
            // Find a safe truncation point (avoid cutting in middle of UTF-8 chars)
            val truncatedBytes = messageBytes.sliceArray(0 until BINDER_MAX_SIZE_BYTES)
            String(truncatedBytes, Charsets.UTF_8)
        } else {
            message
        }
    }

    private fun FeedbackEmailType.isVisaEmail(): Boolean {
        return this is FeedbackEmailType.Visa || this.walletMetaInfo?.isVisa == true
    }

    private fun StringBuilder.appendDisclaimerIfNeeded(type: FeedbackEmailType): StringBuilder {
        return when (type) {
            is FeedbackEmailType.ScanningProblem,
            is FeedbackEmailType.CurrencyDescriptionError,
            is FeedbackEmailType.PreActivatedWallet,
            is FeedbackEmailType.CardAttestationFailed,
            is FeedbackEmailType.BiometricsAuthenticationFailed,
            is FeedbackEmailType.Visa.Dispute,
            is FeedbackEmailType.Visa.DisputeV2,
            is FeedbackEmailType.Visa.FeatureIsBeta,
            -> this
            is FeedbackEmailType.DirectUserRequest,
            is FeedbackEmailType.RateCanBeBetter,
            is FeedbackEmailType.StakingProblem,
            is FeedbackEmailType.SwapProblem,
            is FeedbackEmailType.TransactionSendingProblem,
            is FeedbackEmailType.Visa.Activation,
            is FeedbackEmailType.Visa.DirectUserRequest,
            is FeedbackEmailType.Visa.FailedIssueCard,
            is FeedbackEmailType.Visa.Withdrawal,
            -> {
                append(resources.getStringSafe(R.string.feedback_data_collection_message))
                skipLine()
            }
        }
    }
}