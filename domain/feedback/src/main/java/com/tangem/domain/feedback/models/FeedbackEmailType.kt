package com.tangem.domain.feedback.models

/**
 * Email feedback type
 *
[REDACTED_AUTHOR]
 */
sealed interface FeedbackEmailType {

    val cardInfo: CardInfo?

    /** User initiate request yourself. Example, button on DetailsScreen or OnboardingScreen */
    data class DirectUserRequest(override val cardInfo: CardInfo) : FeedbackEmailType

    /** User rate the app as "can be better" */
    data class RateCanBeBetter(override val cardInfo: CardInfo) : FeedbackEmailType

    /** User has problem with scanning */
    data object ScanningProblem : FeedbackEmailType {
        override val cardInfo: CardInfo? = null
    }

    /** User has problem with sending transaction */
    data class TransactionSendingProblem(override val cardInfo: CardInfo) : FeedbackEmailType

    /** User has problem with staking */
    data class StakingProblem(
        override val cardInfo: CardInfo,
        val validatorName: String?,
        val transactionTypes: List<String>,
        val unsignedTransactions: List<String?>,
    ) : FeedbackEmailType

    data class SwapProblem(
        override val cardInfo: CardInfo,
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
        override val cardInfo: CardInfo? = null
    }

    data class PreActivatedWallet(override val cardInfo: CardInfo) : FeedbackEmailType

    data object CardAttestationFailed : FeedbackEmailType {
        override val cardInfo: CardInfo? = null
    }
}