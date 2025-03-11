package com.tangem.domain.feedback.utils

import android.content.res.Resources
import com.tangem.core.res.getStringSafe
import com.tangem.domain.feedback.R
import com.tangem.domain.feedback.models.FeedbackEmailType

/**
 * Email subject resolver
 *
 * @property resources resources
 *
[REDACTED_AUTHOR]
 */
internal class EmailSubjectResolver(private val resources: Resources) {

    /** Resolve email message body by [type] */
    fun resolve(type: FeedbackEmailType): String {
        return when (type) {
            is FeedbackEmailType.DirectUserRequest -> {
                if (type.cardInfo.isStart2Coin) {
                    resources.getStringSafe(R.string.feedback_subject_support)
                } else {
                    resources.getStringSafe(R.string.feedback_subject_support_tangem)
                }
            }
            is FeedbackEmailType.RateCanBeBetter -> resources.getStringSafe(R.string.feedback_subject_rate_negative)
            is FeedbackEmailType.ScanningProblem -> resources.getStringSafe(R.string.feedback_subject_scan_failed)
            is FeedbackEmailType.TransactionSendingProblem,
            is FeedbackEmailType.StakingProblem,
            is FeedbackEmailType.SwapProblem,
            -> resources.getStringSafe(R.string.feedback_subject_tx_failed)
            is FeedbackEmailType.PreActivatedWallet -> {
                resources.getStringSafe(R.string.feedback_subject_pre_activated_wallet)
            }
            is FeedbackEmailType.CurrencyDescriptionError -> {
                resources.getStringSafe(R.string.feedback_token_description_error)
            }
            FeedbackEmailType.CardAttestationFailed -> "Card attestation failed"
        }
    }
}