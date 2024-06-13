package com.tangem.features.staking.impl.presentation.state

import com.tangem.core.ui.event.consumedEvent
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.state.transformers.StakingScreenStateTransformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StakingStateController @Inject constructor() {

    val value: StakingUiState get() = uiState.value

    private val mutableUiState: MutableStateFlow<StakingUiState> = MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<StakingUiState> get() = mutableUiState.asStateFlow()

    fun update(function: (StakingUiState) -> StakingUiState) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: StakingScreenStateTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    fun clear() {
        mutableUiState.update { getInitialState() }
    }

    private fun getInitialState(): StakingUiState {
        return StakingUiState(
            clickIntents = StakingClickIntentsStub,
            cryptoCurrencyName = "",
            currentScreen = StakingUiStateType.InitialInfo,
            initialInfoState = StakingStates.InitialInfoState.Empty(),
            amountState = StakingStates.AmountState.Empty(),
            validatorState = StakingStates.ValidatorState.Empty(),
            confirmStakingState = StakingStates.ConfirmStakingState.Empty(),
            isBalanceHidden = false,
            event = consumedEvent(),
        )
    }
}