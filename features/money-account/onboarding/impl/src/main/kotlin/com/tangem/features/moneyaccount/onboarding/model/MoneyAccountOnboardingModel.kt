package com.tangem.features.moneyaccount.onboarding.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val onboardingRepository: OnboardingRepository,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountOnboardingComponent.Params>()

    val uiState: StateFlow<MoneyAccountOnboardingUM>
        field = MutableStateFlow<MoneyAccountOnboardingUM>(MoneyAccountOnboardingUM.Loading(onBack = ::back))

    init {
        when (params) {
            is MoneyAccountOnboardingComponent.Params.Deeplink -> validateDeeplinkAndShow(params.deeplink)
            is MoneyAccountOnboardingComponent.Params.FromMain,
            is MoneyAccountOnboardingComponent.Params.FromDetailsScreen,
            -> showOnboarding()
        }
    }

    private fun validateDeeplinkAndShow(deeplink: String) {
        modelScope.launch {
            onboardingRepository.validateDeeplink(deeplink)
                .onRight { isValid -> if (isValid) showOnboarding() else back() }
                .onLeft { back() }
        }
    }

    private fun showOnboarding() {
        uiState.update {
            MoneyAccountOnboardingUM.Content(
                onBack = ::back,
                isLoading = false,
                onGetCardClick = ::onGetCardClick,
                onTermsClick = ::onTermsClick,
                onPrivacyClick = ::onPrivacyClick,
            )
        }
    }

    private fun onTermsClick() {
        // TODO([REDACTED_TASK_KEY]): open the provider Terms of Use link.
    }

    private fun onPrivacyClick() {
        // TODO([REDACTED_TASK_KEY]): open the provider Privacy Policy link.
    }

    private fun onGetCardClick() {
        modelScope.launch {
            setLoading(isLoading = true)
            delay(STUB_GET_CARD_DELAY_MS)
            // TODO: create order and sign challenge [REDACTED_JIRA]
            setLoading(isLoading = false)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { state ->
            when (state) {
                is MoneyAccountOnboardingUM.Content -> state.copy(isLoading = isLoading)
                is MoneyAccountOnboardingUM.Loading -> state
            }
        }
    }

    private fun back() {
        router.pop()
    }

    private companion object {
        // TODO([REDACTED_TASK_KEY]): remove the stub delay once create-order + sign-challenge is implemented.
        const val STUB_GET_CARD_DELAY_MS = 3000L
    }
}