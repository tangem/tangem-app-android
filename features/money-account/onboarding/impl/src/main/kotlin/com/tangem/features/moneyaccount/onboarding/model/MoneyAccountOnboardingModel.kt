package com.tangem.features.moneyaccount.onboarding.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.impl.R
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
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
            MoneyAccountOnboardingComponent.Params.FromMain,
            MoneyAccountOnboardingComponent.Params.FromDetailsScreen,
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
                fees = null,
                onGetStartedClick = ::onGetStartedClick,
                onTermsAndFeesClick = ::showFees,
                onTermsClick = ::onTermsClick,
                onFeesDismiss = ::hideFees,
            )
        }
    }

    // TODO v_rodionov: #[REDACTED_TASK_KEY] get fee stuff from backend
    private fun showFees() {
        updateContent { it.copy(fees = MoneyAccountOnboardingUM.FeesUM.Content(STUB_FEE_ROWS)) }
    }

    private fun hideFees() {
        updateContent { it.copy(fees = null) }
    }

    private fun onTermsClick() {
        // TODO v_rodionov: #[REDACTED_TASK_KEY] open the provider Terms of Use link.
    }

    private fun onGetStartedClick() {
        router.push(
            AppRoute.TangemPayEligibility(
                product = TangemPayProduct.BANKING_ACCOUNT,
                userWalletId = null,
            ),
        )
    }

    private fun updateContent(block: (MoneyAccountOnboardingUM.Content) -> MoneyAccountOnboardingUM.Content) {
        uiState.update { state ->
            when (state) {
                is MoneyAccountOnboardingUM.Content -> block(state)
                is MoneyAccountOnboardingUM.Loading -> state
            }
        }
    }

    private fun back() {
        router.pop()
    }

    private companion object {
        val STUB_FEE_ROWS = persistentListOf(
            MoneyAccountOnboardingUM.FeeRowUM(
                icon = TangemIconUM.Image(R.drawable.img_flag_usa_44),
                provider = "FedWire",
                receive = "$0",
                send = "$0",
            ),
            MoneyAccountOnboardingUM.FeeRowUM(
                icon = TangemIconUM.Image(R.drawable.img_flag_usa_44),
                provider = "ACH",
                receive = "$0",
                send = "$0",
            ),
        )
    }
}