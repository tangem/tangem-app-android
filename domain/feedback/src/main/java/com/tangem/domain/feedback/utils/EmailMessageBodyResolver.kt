package com.tangem.domain.feedback.utils

import com.tangem.domain.feedback.FeedbackDataBuilder
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.repository.FeedbackRepository

/**
 * Email message body resolver
 *
 * @property feedbackRepository feedback repository
 *
[REDACTED_AUTHOR]
 */
internal class EmailMessageBodyResolver(
    private val feedbackRepository: FeedbackRepository,
) {

    /** Resolve email message body by [type] using [cardInfo] */
    suspend fun resolve(type: FeedbackEmailType, cardInfo: CardInfo): String = with(FeedbackDataBuilder()) {
        when (type) {
            FeedbackEmailType.DirectUserRequest -> addUserRequestBody(cardInfo)
            FeedbackEmailType.RateCanBeBetter -> addCardAndPhoneInfo(cardInfo)
            FeedbackEmailType.ScanningProblem -> addScanningProblemBody()
            FeedbackEmailType.TransactionSendingProblem -> addTransactionSendingProblemBody(cardInfo)
        }

        return build()
    }

    private suspend fun FeedbackDataBuilder.addUserRequestBody(cardInfo: CardInfo) {
        addUserWalletsInfo(userWalletsInfo = feedbackRepository.getUserWalletsInfo())
        addDelimiter()
        addCardInfo(cardInfo)
        addDelimiter()
        addBlockchainInfoList(blockchainInfoList = feedbackRepository.getBlockchainInfoList())
        addDelimiter()
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addScanningProblemBody() {
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private suspend fun FeedbackDataBuilder.addTransactionSendingProblemBody(cardInfo: CardInfo) {
        addCardInfo(cardInfo)
        addDelimiter()

        val blockchainError = feedbackRepository.getBlockchainErrorInfo()
        val blockchainInfo = blockchainError?.let {
            feedbackRepository.getBlockchainInfo(
                blockchainId = blockchainError.blockchainId,
                derivationPath = blockchainError.derivationPath,
            )
        }

        if (blockchainInfo != null) {
            addBlockchainError(blockchainInfo, blockchainError)
            addDelimiter()
        }

        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addCardAndPhoneInfo(cardInfo: CardInfo) {
        addCardInfo(cardInfo)
        addDelimiter()
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }
}