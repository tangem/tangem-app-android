package com.tangem.domain.feedback.utils

import android.content.res.Resources
import com.tangem.domain.feedback.R
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmailType

/**
 * Email subject resolver
 *
 * @property resources resources
 *
* [REDACTED_AUTHOR]
 */
internal class EmailSubjectResolver(private val resources: Resources) {

    /** Resolve email message body by [type] using [cardInfo] */
    fun resolve(type: FeedbackEmailType, cardInfo: CardInfo): String {
        return when (type) {
            FeedbackEmailType.DirectUserRequest -> {
                if (cardInfo.isStart2Coin) {
                    R.string.feedback_subject_support
                } else {
                    R.string.feedback_subject_support_tangem
                }
            }
            FeedbackEmailType.RateCanBeBetter -> R.string.feedback_subject_rate_negative
            FeedbackEmailType.ScanningProblem -> R.string.feedback_subject_scan_failed
            FeedbackEmailType.TransactionSendingProblem -> R.string.feedback_subject_tx_failed
        }
            .let(resources::getString)
    }
}
