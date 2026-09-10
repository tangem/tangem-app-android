package com.tangem.features.tangempay.eligibility.waitlist

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.usecase.JoinTangemPayWaitlistUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayWaitlistModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val joinWaitlistUseCase: JoinTangemPayWaitlistUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<TangemPayWaitlistComponent.Params>()

    private val idempotencyKey = UUID.randomUUID().toString()

    val uiState: StateFlow<TangemPayWaitlistUM>
        field = MutableStateFlow(getInitialState())

    fun dismiss() {
        params.onDismiss()
    }

    private fun onEmailChange(email: String) {
        uiState.update { state ->
            if (state.isSaveInProgress) return@update state
            state.copy(
                email = email,
                emailError = null,
                isSaveEnabled = isEmailValid(email),
            )
        }
    }

    private fun onEmailClearClick() {
        uiState.update { state ->
            if (state.isSaveInProgress) return@update state
            state.copy(email = "", emailError = null, isSaveEnabled = false)
        }
    }

    private fun onMarketingConsentClick() {
        uiState.update { state ->
            if (state.isSaveInProgress) return@update state
            state.copy(isMarketingConsentGiven = !state.isMarketingConsentGiven)
        }
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    private fun onSaveClick() {
        val state = uiState.value
        if (state.isSaveInProgress) return

        uiState.update { it.copy(isSaveInProgress = true, isSaveEnabled = false, emailError = null) }

        modelScope.launch {
            joinWaitlistUseCase(
                userWalletId = params.userWalletId,
                entry = TangemPayWaitlistEntry(
                    email = state.email.trim(),
                    product = params.product,
                    countryCode = params.countryCode,
                    regionCode = params.regionCode,
                    isMarketingConsentGiven = state.isMarketingConsentGiven,
                ),
                idempotencyKey = idempotencyKey,
            )
                .onRight { dismiss() }
                .onLeft(::onJoinFailed)
        }
    }

    private fun onJoinFailed(error: VisaApiError) {
        uiState.update { state ->
            state.copy(
                isSaveInProgress = false,
                isSaveEnabled = isEmailValid(state.email),
                emailError = when (error) {
                    is VisaApiError.WaitlistInvalidEmail -> {
                        resourceReference(R.string.common_error_email_incorrect_format)
                    }
                    else -> resourceReference(R.string.common_something_went_wrong)
                },
            )
        }
    }

    private fun isEmailValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    private fun getInitialState(): TangemPayWaitlistUM {
        return TangemPayWaitlistUM(
            email = "",
            emailError = null,
            isMarketingConsentGiven = false,
            isSaveEnabled = false,
            isSaveInProgress = false,
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