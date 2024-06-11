package com.tangem.tap.common.feedback

import com.tangem.core.navigation.feedback.FeedbackManager
import com.tangem.core.navigation.feedback.FeedbackType
import com.tangem.tap.store
import timber.log.Timber

internal class ProxyFeedbackManager : FeedbackManager {

    override fun sendEmail(type: FeedbackType) {
        val manager = store.state.globalState.feedbackManager

        if (manager == null) {
            Timber.e("Feedback manager is not initialized")
            return
        }

        val data = when (type) {
            is FeedbackType.Feedback -> FeedbackEmail()
            is FeedbackType.RateCanBeBetter -> RateCanBeBetterEmail()
            is FeedbackType.ScanFails -> ScanFailsEmail()
            is FeedbackType.SendTransactionFailed -> SendTransactionFailedEmail(type.error)
            is FeedbackType.Support -> FeedbackEmail()
        }

        manager.sendEmail(data)
    }
}