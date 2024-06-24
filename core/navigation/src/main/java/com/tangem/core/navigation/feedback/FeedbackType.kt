package com.tangem.core.navigation.feedback

sealed class FeedbackType {
    data object RateCanBeBetter : FeedbackType()

    data object ScanFails : FeedbackType()

    data class SendTransactionFailed(val error: String) : FeedbackType()

    data object Feedback : FeedbackType()

    data object Support : FeedbackType()
}