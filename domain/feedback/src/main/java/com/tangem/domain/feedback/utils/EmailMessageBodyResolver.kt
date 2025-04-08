package com.tangem.domain.feedback.utils

import com.tangem.domain.feedback.FeedbackDataBuilder
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.visa.model.VisaTxDetails

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

    /** Resolve email message body by [type] */
    suspend fun resolve(type: FeedbackEmailType): String = with(FeedbackDataBuilder()) {
        when (type) {
            is FeedbackEmailType.DirectUserRequest -> addUserRequestBody(type.cardInfo)
            is FeedbackEmailType.RateCanBeBetter -> addCardAndPhoneInfo(type.cardInfo)
            is FeedbackEmailType.TransactionSendingProblem -> addTransactionSendingProblemBody(type.cardInfo)
            is FeedbackEmailType.StakingProblem -> addStakingProblemBody(type)
            is FeedbackEmailType.SwapProblem -> addSwapProblemBody(type)
            is FeedbackEmailType.CurrencyDescriptionError -> addTokenInfo(type)
            is FeedbackEmailType.PreActivatedWallet -> addUserRequestBody(type.cardInfo)
            is FeedbackEmailType.ScanningProblem,
            is FeedbackEmailType.CardAttestationFailed,
            -> addPhoneInfoBody()
            is FeedbackEmailType.Visa.Activation -> addUserRequestBody(type.cardInfo)
            is FeedbackEmailType.Visa.DirectUserRequest -> addUserRequestBody(type.cardInfo)
            is FeedbackEmailType.Visa.Dispute -> addVisaRequestBody(type.cardInfo, type.visaTxDetails)
        }

        return build()
    }

    private suspend fun FeedbackDataBuilder.addVisaRequestBody(cardInfo: CardInfo, visaTxDetails: VisaTxDetails) {
        addUserRequestBody(cardInfo)
        addDelimiter()
        addVisaTxInfo(visaTxDetails)
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

    private fun FeedbackDataBuilder.addPhoneInfoBody() {
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

    private suspend fun FeedbackDataBuilder.addStakingProblemBody(type: FeedbackEmailType.StakingProblem) {
        addCardInfo(type.cardInfo)
        addDelimiter()

        val userWalletId = requireNotNull(type.cardInfo.userWalletId) { "UserWalletId must be not null" }
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

        addStakingInfo(
            validatorName = type.validatorName,
            transactionTypes = type.transactionTypes,
            unsignedTransactions = type.unsignedTransactions,
        )
        addDelimiter()

        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private suspend fun FeedbackDataBuilder.addSwapProblemBody(type: FeedbackEmailType.SwapProblem) {
        addCardInfo(type.cardInfo)
        addDelimiter()

        val userWalletId = requireNotNull(type.cardInfo.userWalletId) { "UserWalletId must be not null" }
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

        addSwapInfo(providerName = type.providerName, txId = type.txId)
        addDelimiter()

        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addCardAndPhoneInfo(cardInfo: CardInfo) {
        addCardInfo(cardInfo)
        addDelimiter()
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addTokenInfo(type: FeedbackEmailType.CurrencyDescriptionError) {
        addTokenShortInfo(id = type.currencyId, name = type.currencyName)
    }
}