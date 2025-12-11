package com.tangem.domain.feedback.utils

import android.content.res.Resources
import com.tangem.core.res.getStringSafe
import com.tangem.domain.feedback.R
import com.tangem.domain.feedback.models.FeedbackEmailType

/**
 * Email message title resolver
 *
 * @property resources resources
 *
[REDACTED_AUTHOR]
 */
internal class EmailMessageTitleResolver(private val resources: Resources) {

    /** Resolve email message title by [type] */
    fun resolve(type: FeedbackEmailType): String {
        val resId = when (type) {
            is FeedbackEmailType.DirectUserRequest,
            is FeedbackEmailType.CurrencyDescriptionError,
            is FeedbackEmailType.CardAttestationFailed,
            is FeedbackEmailType.Visa.Activation,
            is FeedbackEmailType.Visa.DirectUserRequest,
            is FeedbackEmailType.Visa.Dispute,
            is FeedbackEmailType.Visa.DisputeV2,
            is FeedbackEmailType.Visa.FailedIssueCard,
            is FeedbackEmailType.Visa.Withdrawal,
            is FeedbackEmailType.Visa.FeatureIsBeta,
            is FeedbackEmailType.PreActivatedWallet,
            -> R.string.feedback_preface_support
            is FeedbackEmailType.RateCanBeBetter -> R.string.feedback_preface_rate_negative
            is FeedbackEmailType.ScanningProblem -> R.string.feedback_preface_scan_failed
            is FeedbackEmailType.TransactionSendingProblem,
            is FeedbackEmailType.StakingProblem,
            is FeedbackEmailType.SwapProblem,
            -> R.string.feedback_preface_tx_failed
        }

        return resources.getStringSafe(resId)
    }
}