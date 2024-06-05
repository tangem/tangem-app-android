package com.tangem.features.staking.impl.presentation.state

import com.tangem.core.ui.event.consumedEvent
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.Provider

internal class StakingStateFactory(
    private val clickIntents: StakingClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val currentStateProvider: Provider<StakingUiState>,
) {

    fun getInitialState(): StakingUiState {
        return StakingUiState(
            clickIntents = clickIntents,
            cryptoCurrencyName = cryptoCurrency.name,
            currentScreen = StakingUiStateType.InitialInfo,
            initialInfoState = StakingStates.InitialInfoState.Empty(),
            amountState = StakingStates.AmountState.Empty(),
            validatorState = StakingStates.ValidatorState.Empty(),
            confirmStakingState = StakingStates.ConfirmStakingState.Empty(),
            isBalanceHidden = false,
            event = consumedEvent(),
        )
    }

    fun getOnHideBalanceState(isBalanceHidden: Boolean): StakingUiState {
        return currentStateProvider().copy(isBalanceHidden = isBalanceHidden)
    }
}
