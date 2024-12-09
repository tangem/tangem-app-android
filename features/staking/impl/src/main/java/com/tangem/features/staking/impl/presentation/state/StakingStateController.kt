package com.tangem.features.staking.impl.presentation.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.events.StakingEvent
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.state.transformers.SetButtonsStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.SetTitleTransformer
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StakingStateController @Inject constructor(
    urlOpener: UrlOpener,
) {

    val value: StakingUiState get() = uiState.value

    private val mutableUiState: MutableStateFlow<StakingUiState> = MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<StakingUiState> get() = mutableUiState.asStateFlow()

    private val buttonsTransformer = SetButtonsStateTransformer(urlOpener)
    private val titleTransformer = SetTitleTransformer

    fun update(function: (StakingUiState) -> StakingUiState) {
        mutableUiState.update(function = function)
        mutableUiState.update(function = buttonsTransformer::transform)
        mutableUiState.update(function = titleTransformer::transform)
    }

    fun update(transformer: Transformer<StakingUiState>) {
        mutableUiState.update(function = transformer::transform)
        mutableUiState.update(function = buttonsTransformer::transform)
        mutableUiState.update(function = titleTransformer::transform)
    }

    fun updateAll(vararg transformer: Transformer<StakingUiState>) {
        transformer.forEach { mutableUiState.update(function = it::transform) }
        mutableUiState.update(function = buttonsTransformer::transform)
        mutableUiState.update(function = titleTransformer::transform)
    }

    fun clear() {
        mutableUiState.update { getInitialState() }
        mutableUiState.update(function = buttonsTransformer::transform)
        mutableUiState.update(function = titleTransformer::transform)
    }

    fun updateEvent(event: StakingEvent?) {
        mutableUiState.update {
            it.copy(event = event?.let { triggeredEvent(event, ::dismissAlert) } ?: consumedEvent())
        }
    }

    fun dismissAlert() {
        mutableUiState.update { it.copy(event = consumedEvent()) }
    }

    private fun getInitialState(): StakingUiState {
        return StakingUiState(
            title = TextReference.EMPTY,
            subtitle = null,
            clickIntents = StakingClickIntentsStub,
            walletName = "",
            cryptoCurrencyName = "",
            cryptoCurrencySymbol = "",
            cryptoCurrencyBlockchainId = "",
            currentStep = StakingStep.InitialInfo,
            initialInfoState = StakingStates.InitialInfoState.Empty(),
            amountState = AmountState.Empty(),
            validatorState = StakingStates.ValidatorState.Empty(),
            rewardsValidatorsState = StakingStates.RewardsValidatorsState.Empty(),
            confirmationState = StakingStates.ConfirmationState.Empty(),
            isBalanceHidden = false,
            event = consumedEvent(),
            bottomSheetConfig = null,
            actionType = StakingActionCommonType.Enter,
            buttonsState = NavigationButtonsState.Empty,
            balanceState = null,
        )
    }
}