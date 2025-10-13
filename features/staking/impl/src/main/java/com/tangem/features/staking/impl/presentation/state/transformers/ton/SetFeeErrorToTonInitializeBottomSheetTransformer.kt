package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.utils.transformer.Transformer

internal class SetFeeErrorToTonInitializeBottomSheetTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val prevBottomSheetConfig = prevState.bottomSheetConfig ?: return prevState
        val prevBottomSheetConfigContent =
            prevBottomSheetConfig.content as? TonInitializeAccountBottomSheetConfig ?: return prevState

        return prevState.copy(
            bottomSheetConfig = prevBottomSheetConfig.copy(
                content = prevBottomSheetConfigContent.copy(
                    isButtonEnabled = false,
                    feeState = FeeState.Error,
                    isButtonLoading = false,
                ),
            ),
        )
    }
}