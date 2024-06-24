package com.tangem.core.navigation.feedback

class DummyFeedbackManager : FeedbackManager {

    override fun sendEmail(type: FeedbackType) {
        /* no-op */
    }
}