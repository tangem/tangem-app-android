package com.tangem.tap.common.feedback

import com.tangem.core.navigation.email.EmailSender
import com.tangem.domain.feedback.GetFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 25/02/2021.
 */
class LegacyFeedbackManager(
    private val getFeedbackEmailUseCase: GetFeedbackEmailUseCase,
) {

    // TODO: https://tangem.atlassian.net/browse/AND-8506
    fun sendEmail(type: FeedbackEmailType) {
        scope.launch {
            val email = getFeedbackEmailUseCase(type = type)

            store.inject(DaggerGraphState::emailSender).send(
                email = EmailSender.Email(
                    address = email.address,
                    subject = email.subject,
                    message = email.message,
                    attachment = email.file,
                ),
            )
        }
    }
}
