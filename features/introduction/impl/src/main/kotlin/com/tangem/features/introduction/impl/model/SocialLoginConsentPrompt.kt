package com.tangem.features.introduction.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import kotlinx.coroutines.CompletableDeferred

internal class SocialLoginConsentPrompt(
    private val title: TextReference,
    private val description: TextReference,
    private val acknowledgement: TextReference,
    private val onStateChanged: (SocialLoginConsentUM?) -> Unit,
) {

    private val answer = CompletableDeferred<SocialLoginConsent?>()

    private var choice = Choice()

    suspend fun await(): SocialLoginConsent? {
        publish()

        return try {
            answer.await()
        } finally {
            onStateChanged(null)
        }
    }

    private fun publish() {
        onStateChanged(
            SocialLoginConsentUM(
                title = title,
                description = description,
                acknowledgement = acknowledgement,
                isAcknowledged = choice.isAcknowledged,
                isAcknowledgementMissing = choice.isAcknowledgementMissing,
                isEmailSharingAllowed = choice.isEmailSharingAllowed,
                onAcknowledgedChange = ::onAcknowledgedChange,
                onEmailSharingChange = ::onEmailSharingChange,
                onConfirmClick = ::onConfirmClick,
                onCloseClick = ::onCloseClick,
            ),
        )
    }

    private fun update(transform: (Choice) -> Choice) {
        choice = transform(choice)
        publish()
    }

    private fun onAcknowledgedChange(isAcknowledged: Boolean) = update {
        it.copy(isAcknowledged = isAcknowledged, isAcknowledgementMissing = !isAcknowledged)
    }

    private fun onEmailSharingChange(isAllowed: Boolean) = update { it.copy(isEmailSharingAllowed = isAllowed) }

    private fun onConfirmClick() {
        if (!choice.isAcknowledged) {
            update { it.copy(isAcknowledgementMissing = true) }
            return
        }

        answer.complete(SocialLoginConsent(isEmailSharingAllowed = choice.isEmailSharingAllowed))
    }

    private fun onCloseClick() {
        answer.complete(null)
    }

    private data class Choice(
        val isAcknowledged: Boolean = false,
        val isAcknowledgementMissing: Boolean = false,
        val isEmailSharingAllowed: Boolean = false,
    )
}