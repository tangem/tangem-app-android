package com.tangem.domain.feedback.models

import com.tangem.domain.visa.model.VisaTxDetails

/**
 * Email feedback type
 *
[REDACTED_AUTHOR]
 */
sealed interface FeedbackEmailType {

    val walletMetaInfo: WalletMetaInfo?

    /** User initiate request yourself. Example, button on DetailsScreen or OnboardingScreen */
    data class DirectUserRequest(override val walletMetaInfo: WalletMetaInfo) : FeedbackEmailType

    /** User rate the app as "can be better" */
    data class RateCanBeBetter(override val walletMetaInfo: WalletMetaInfo) : FeedbackEmailType

    /** User has problem with scanning */
    data object ScanningProblem : FeedbackEmailType {
        override val walletMetaInfo: WalletMetaInfo? = null
    }

    /** User has problem with sending transaction */
    data class TransactionSendingProblem(override val walletMetaInfo: WalletMetaInfo) : FeedbackEmailType

    /** User has problem with staking */
    data class StakingProblem(
        override val walletMetaInfo: WalletMetaInfo,
        val validatorName: String?,
        val transactionTypes: List<String>,
        val unsignedTransactions: List<String?>,
    ) : FeedbackEmailType

    data class SwapProblem(
        override val walletMetaInfo: WalletMetaInfo,
        val providerName: String,
        val txId: String,
    ) : FeedbackEmailType

    /**
     * Error in currency description
     *
     * @property currencyId   currency id
     * @property currencyName currency name
     */
    data class CurrencyDescriptionError(val currencyId: String, val currencyName: String) : FeedbackEmailType {
        override val walletMetaInfo: WalletMetaInfo? = null
    }

    data class PreActivatedWallet(override val walletMetaInfo: WalletMetaInfo) : FeedbackEmailType

    data object CardAttestationFailed : FeedbackEmailType {
        override val walletMetaInfo: WalletMetaInfo? = null
    }

    sealed class Visa : FeedbackEmailType {
        data class DirectUserRequest(override val walletMetaInfo: WalletMetaInfo) : Visa()

        data class Activation(override val walletMetaInfo: WalletMetaInfo) : Visa()

        data class Dispute(
            val visaTxDetails: VisaTxDetails,
            override val walletMetaInfo: WalletMetaInfo,
        ) : Visa()
    }
}