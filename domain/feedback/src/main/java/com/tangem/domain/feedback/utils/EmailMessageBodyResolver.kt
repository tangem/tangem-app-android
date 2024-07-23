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
* [REDACTED_AUTHOR]
 */
internal class EmailMessageBodyResolver(
    private val feedbackRepository: FeedbackRepository,
) {

    /** Resolve email message body by [type] */
    suspend fun resolve(type: FeedbackEmailType): String = with(FeedbackDataBuilder()) {
        when (type) {
            is FeedbackEmailType.DirectUserRequest -> addUserRequestBody(type.cardInfo)
            is FeedbackEmailType.RateCanBeBetter -> addCardAndPhoneInfo(type.cardInfo)
            is FeedbackEmailType.ScanningProblem -> addScanningProblemBody()
            is FeedbackEmailType.TransactionSendingProblem -> addTransactionSendingProblemBody(type.cardInfo)
        }

        return build()
    }

    private suspend fun FeedbackDataBuilder.addUserRequestBody(cardInfo: CardInfo) {
        addUserWalletsInfo(userWalletsInfo = feedbackRepository.getUserWalletsInfo(cardInfo.userWalletId))
        addDelimiter()
        addCardInfo(cardInfo)
        addDelimiter()

        if (cardInfo.userWalletId != null) {
            val blockchainInfoList = feedbackRepository.getBlockchainInfoList(cardInfo.userWalletId)

            if (blockchainInfoList.isNotEmpty()) {
                addBlockchainInfoList(blockchainInfoList = blockchainInfoList)
                addDelimiter()
            }
        }

        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addScanningProblemBody() {
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private suspend fun FeedbackDataBuilder.addTransactionSendingProblemBody(cardInfo: CardInfo) {
        addCardInfo(cardInfo)
        addDelimiter()

        val userWalletId = requireNotNull(cardInfo.userWalletId) { "UserWalletId must be not null" }
        val blockchainError = feedbackRepository.getBlockchainErrorInfo(userWalletId = userWalletId)
        val blockchainInfo = blockchainError?.let {
            feedbackRepository.getBlockchainInfo(
                userWalletId = userWalletId,
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
