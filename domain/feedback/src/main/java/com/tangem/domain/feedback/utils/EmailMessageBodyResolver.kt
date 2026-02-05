package com.tangem.domain.feedback.utils

import com.tangem.domain.feedback.FeedbackDataBuilder
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.visa.model.VisaTxDetails

/**
 * Email message body resolver
 *
 * @property feedbackRepository feedback repository
 *
[REDACTED_AUTHOR]
 */
class EmailMessageBodyResolver(
    private val feedbackRepository: FeedbackRepository,
) {

    /** Resolve email message body by [type] */
    @Suppress("CyclomaticComplexMethod")
    suspend fun resolve(type: FeedbackEmailType): String = with(FeedbackDataBuilder()) {
        when (type) {
            is FeedbackEmailType.DirectUserRequest -> addUserRequestBody(type.walletMetaInfo)
            is FeedbackEmailType.RateCanBeBetter -> addCardAndPhoneInfo(type.walletMetaInfo)
            is FeedbackEmailType.TransactionSendingProblem -> addTransactionSendingProblemBody(type.walletMetaInfo)
            is FeedbackEmailType.StakingProblem -> addStakingProblemBody(type)
            is FeedbackEmailType.SwapProblem -> addSwapProblemBody(type)
            is FeedbackEmailType.CurrencyDescriptionError -> addTokenInfo(type)
            is FeedbackEmailType.PreActivatedWallet -> addUserRequestBody(type.walletMetaInfo)
            is FeedbackEmailType.BackupProblem -> addUserRequestBody(type.walletMetaInfo)
            is FeedbackEmailType.ScanningProblem,
            is FeedbackEmailType.CardAttestationFailed,
            -> addPhoneInfoBody()
            is FeedbackEmailType.Visa.Activation -> addUserRequestBody(type.walletMetaInfo)
            is FeedbackEmailType.Visa.DirectUserRequest -> addUserRequestBody(type.walletMetaInfo)
            is FeedbackEmailType.Visa.Dispute -> addVisaRequestBody(
                walletMetaInfo = type.walletMetaInfo,
                customerId = type.customerId,
                visaTxDetails = type.visaTxDetails,
            )
            is FeedbackEmailType.Visa.FailedIssueCard -> addTangemPayFailedIssuingCardBody(type)
            is FeedbackEmailType.Visa.DisputeV2 -> addTangemPayDisputeRequestBody(type)
            is FeedbackEmailType.Visa.Withdrawal -> addTangemPayWithdrawalRequestBody(type)
            is FeedbackEmailType.Visa.FeatureIsBeta -> addTangemPayBetaRequestBody(type)
            is FeedbackEmailType.Visa.KycRejected -> addTangemPayKycRejectedRequestBody(type)
        }

        return build()
    }

    private fun FeedbackDataBuilder.addTangemPayFailedIssuingCardBody(type: FeedbackEmailType.Visa.FailedIssueCard) {
        addTangemPayPhoneInfoBody(type)
        addDelimiter()
        addCustomerId(customerId = type.customerId)
        type.walletMetaInfo.userWalletId?.let { userWalletId ->
            addUserWalletId(userWalletId = userWalletId.stringValue)
        }
    }

    private fun FeedbackDataBuilder.addTangemPayDisputeRequestBody(type: FeedbackEmailType.Visa.DisputeV2) {
        addTangemPayPhoneInfoBody(type = type)
        addDelimiter()
        addTangemPayTxInfo(type.item)
        addDelimiter()
        addCustomerId(customerId = type.customerId)
        type.walletMetaInfo.userWalletId?.let { userWalletId ->
            addUserWalletId(userWalletId = userWalletId.stringValue)
        }
    }

    private fun FeedbackDataBuilder.addTangemPayBetaRequestBody(type: FeedbackEmailType.Visa) {
        addTangemPayPhoneInfoBody(type)
        addDelimiter()
        addCustomerId(customerId = type.customerId)
        type.walletMetaInfo?.userWalletId?.let { userWalletId ->
            addUserWalletId(userWalletId = userWalletId.stringValue)
        }
    }

    private fun FeedbackDataBuilder.addTangemPayKycRejectedRequestBody(type: FeedbackEmailType.Visa.KycRejected) {
        addTangemPayPhoneInfoBody(type)
        addDelimiter()
        addCustomerId(customerId = type.customerId)
    }

    private suspend fun FeedbackDataBuilder.addTangemPayWithdrawalRequestBody(
        type: FeedbackEmailType.Visa.Withdrawal,
    ) {
        addUserWalletMetaInfo(type.walletMetaInfo)
        addDelimiter()
        addCustomerId(customerId = type.customerId)

        val userWalletId = requireNotNull(type.walletMetaInfo.userWalletId) { "UserWalletId must be not null" }
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

    private suspend fun FeedbackDataBuilder.addVisaRequestBody(
        walletMetaInfo: WalletMetaInfo,
        visaTxDetails: VisaTxDetails,
        customerId: String,
    ) {
        addUserRequestBody(walletMetaInfo)
        addDelimiter()
        addCustomerId(customerId = customerId)
        addVisaTxInfo(visaTxDetails)
    }

    private suspend fun FeedbackDataBuilder.addUserRequestBody(walletMetaInfo: WalletMetaInfo) {
        addUserWalletsInfo(userWalletsInfo = feedbackRepository.getUserWalletsInfo(walletMetaInfo.userWalletId))
        addDelimiter()
        addUserWalletMetaInfo(walletMetaInfo)
        addDelimiter()

        val userWalletId = walletMetaInfo.userWalletId

        if (userWalletId != null) {
            val blockchainInfoList = feedbackRepository.getBlockchainInfoList(userWalletId)

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

    private fun FeedbackDataBuilder.addTangemPayPhoneInfoBody(type: FeedbackEmailType.Visa) {
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
        addTangemPayIssueType(type)
    }

    private suspend fun FeedbackDataBuilder.addTransactionSendingProblemBody(walletMetaInfo: WalletMetaInfo) {
        addUserWalletMetaInfo(walletMetaInfo)
        addDelimiter()

        val userWalletId = requireNotNull(walletMetaInfo.userWalletId) { "UserWalletId must be not null" }
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
        addUserWalletMetaInfo(type.walletMetaInfo)
        addDelimiter()

        val userWalletId = requireNotNull(type.walletMetaInfo.userWalletId) { "UserWalletId must be not null" }
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
        addUserWalletMetaInfo(type.walletMetaInfo)
        addDelimiter()

        val userWalletId = requireNotNull(type.walletMetaInfo.userWalletId) { "UserWalletId must be not null" }
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

    private fun FeedbackDataBuilder.addCardAndPhoneInfo(walletMetaInfo: WalletMetaInfo) {
        addUserWalletMetaInfo(walletMetaInfo)
        addDelimiter()
        addPhoneInfo(phoneInfo = feedbackRepository.getPhoneInfo())
    }

    private fun FeedbackDataBuilder.addTokenInfo(type: FeedbackEmailType.CurrencyDescriptionError) {
        addTokenShortInfo(id = type.currencyId, name = type.currencyName)
    }
}