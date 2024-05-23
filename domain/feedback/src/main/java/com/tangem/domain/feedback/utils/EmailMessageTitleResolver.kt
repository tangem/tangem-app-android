package com.tangem.domain.feedback.utils

import android.content.res.Resources
import com.tangem.domain.feedback.R
import com.tangem.domain.feedback.models.FeedbackEmailType

/**
 * Email message title resolver
 *
 * @property resources resources
 *
* [REDACTED_AUTHOR]
 */
internal class EmailMessageTitleResolver(private val resources: Resources) {

    /** Resolve email message title by [type] */
    fun resolve(type: FeedbackEmailType): String {
        return when (type) {
            FeedbackEmailType.DirectUserRequest -> R.string.feedback_preface_support
            FeedbackEmailType.RateCanBeBetter -> R.string.feedback_preface_rate_negative
            FeedbackEmailType.ScanningProblem -> R.string.feedback_preface_scan_failed
            FeedbackEmailType.TransactionSendingProblem -> R.string.feedback_preface_tx_failed
        }
            .let(resources::getString)
    }
}
