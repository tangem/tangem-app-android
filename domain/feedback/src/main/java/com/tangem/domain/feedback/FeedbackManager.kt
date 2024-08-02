package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.FeedbackEmailType

interface FeedbackManager {

    fun sendEmail(type: FeedbackEmailType)
}