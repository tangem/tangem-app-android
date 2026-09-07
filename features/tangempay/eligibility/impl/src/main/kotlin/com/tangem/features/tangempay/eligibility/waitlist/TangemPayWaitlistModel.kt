package com.tangem.features.tangempay.eligibility.waitlist

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayWaitlistModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val uiMessageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<TangemPayWaitlistComponent.Params>()

    val uiState: StateFlow<TangemPayWaitlistUM>
        field = MutableStateFlow(getInitialState())

    fun dismiss() {
        params.onDismiss()
    }

    private fun onEmailChange(email: String) {
        uiState.update { state ->
            state.copy(email = email, isSaveEnabled = isEmailValid(email))
        }
    }

    private fun onEmailClearClick() {
        uiState.update { state -> state.copy(email = "", isSaveEnabled = false) }
    }

    private fun onMarketingConsentClick() {
        uiState.update { state -> state.copy(isMarketingConsentGiven = !state.isMarketingConsentGiven) }
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    // TODO v_rodionov
    private fun onSaveClick() {
        uiMessageSender.send(ToastMessage(stringReference("Join waitlist: ${uiState.value.email}")))
    }

    private fun isEmailValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    private fun getInitialState(): TangemPayWaitlistUM {
        return TangemPayWaitlistUM(
            email = "",
            isMarketingConsentGiven = false,
            isSaveEnabled = false,
            onEmailChange = ::onEmailChange,
            onEmailClearClick = ::onEmailClearClick,
            onMarketingConsentClick = ::onMarketingConsentClick,
            onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            onSaveClick = ::onSaveClick,
            onDismiss = ::dismiss,
        )
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
        val EMAIL_REGEX = "[^@\\s]+@[^@\\s.]+\\.[^@\\s]{2,}".toRegex()
    }
}