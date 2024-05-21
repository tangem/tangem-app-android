package com.tangem.domain.feedback.models

/**
 * Email feedback type
 *
 * @author Andrew Khokhlov on 17/05/2024
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
