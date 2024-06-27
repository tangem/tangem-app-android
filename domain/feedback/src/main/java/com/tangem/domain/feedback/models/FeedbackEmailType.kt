package com.tangem.domain.feedback.models

/**
 * Email feedback type
 *
[REDACTED_AUTHOR]
 */
sealed interface FeedbackEmailType {

    /** User initiate request yourself. Example, button on DetailsScreen or OnboardingScreen */
    data object DirectUserRequest : FeedbackEmailType

    /** User rate the app as "can be better" */
    data object RateCanBeBetter : FeedbackEmailType

    /** User has problem with scanning */
    data object ScanningProblem : FeedbackEmailType

    /** User has problem with sending transaction */
    data object TransactionSendingProblem : FeedbackEmailType
}