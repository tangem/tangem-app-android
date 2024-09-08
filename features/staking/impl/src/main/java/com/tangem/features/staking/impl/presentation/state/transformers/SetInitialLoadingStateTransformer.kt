package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetInitialLoadingStateTransformer(
    private val isRefreshing: Boolean,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val initialState = prevState.initialInfoState as? StakingStates.InitialInfoState.Data
        return prevState.copy(
            initialInfoState = initialState?.copy(
                pullToRefreshConfig = prevState.initialInfoState.pullToRefreshConfig.copy(
                    isRefreshing = isRefreshing,
                ),
            ) ?: prevState.initialInfoState,
        )
    }
}