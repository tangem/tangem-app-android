package com.tangem.tap.common.feedback

import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.tap.scope
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 25/02/2021.
 */
class LegacyFeedbackManager(
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) {

    fun sendEmail(type: FeedbackEmailType) {
        scope.launch {
            sendFeedbackEmailUseCase(type = type)
        }
    }
}
