package com.tangem.tap.common.feedback

import com.tangem.domain.feedback.FeedbackManager
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.tap.store
import timber.log.Timber

internal class ProxyFeedbackManager : FeedbackManager {

    override fun sendEmail(type: FeedbackEmailType) {
        val manager = store.state.globalState.feedbackManager

        if (manager == null) {
            Timber.e("Feedback manager is not initialized")
            return
        }

        manager.sendEmail(type)
    }
}
