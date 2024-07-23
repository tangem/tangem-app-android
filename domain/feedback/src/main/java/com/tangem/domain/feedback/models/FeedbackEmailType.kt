package com.tangem.domain.feedback.models

/**
 * Email feedback type
 *
* [REDACTED_AUTHOR]
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
}
