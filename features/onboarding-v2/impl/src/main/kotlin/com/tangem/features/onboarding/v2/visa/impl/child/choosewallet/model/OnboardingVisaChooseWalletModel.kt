package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent.Params.Event
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state.OnboardingVisaChooseWalletUM
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state.SelectableChainRowUM
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class OnboardingVisaChooseWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())

    val onEvent = MutableSharedFlow<Event>()
    val uiState = _uiState.asStateFlow()

    private fun getInitialState(): OnboardingVisaChooseWalletUM {
        val selectedOption = SelectableChainRowUM(
            event = Event.TangemWallet,
            icon = R.drawable.ic_tangem_24,
            text = resourceReference(R.string.common_tangem_wallet),
        )

        return OnboardingVisaChooseWalletUM(
            options = persistentListOf(
                selectedOption,
                SelectableChainRowUM(
                    event = Event.OtherWallet,
                    icon = R.drawable.ic_wallet_filled_24,
                    text = resourceReference(R.string.visa_onboarding_other_wallet),
                ),
            ),
            selectedOption = selectedOption,
            onOptionSelected = { option -> _uiState.update { it.copy(selectedOption = option) } },
            onContinueClick = ::onContinueClicked,
        )
    }

    private fun onContinueClicked() {
        modelScope.launch {
            val selectedOption = _uiState.value.selectedOption ?: return@launch
            val value = getAnalyticsValue(selectedOption.event)
            analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.ChooseWalletScreen(value))
            onEvent.emit(selectedOption.event)
        }
    }

    private fun getAnalyticsValue(event: Event) = when (event) {
        Event.TangemWallet -> {
            "Tangem"
        }
        Event.OtherWallet -> {
            "Other"
        }
    }
}
