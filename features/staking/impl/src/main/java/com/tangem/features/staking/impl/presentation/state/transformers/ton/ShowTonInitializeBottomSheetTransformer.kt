package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.tangem.common.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.utils.transformer.Transformer

internal class ShowTonInitializeBottomSheetTransformer(
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismiss,
                content = TonInitializeAccountBottomSheetConfig(
                    title = resourceReference(R.string.staking_account_initialization_title),
                    message = resourceReference(R.string.staking_account_initialization_message),
                    footer = resourceReference(R.string.staking_account_initialization_footer),
                    onButtonClick = prevState.clickIntents::onActivateTonAccountClick,
                    isButtonEnabled = false,
                    feeState = FeeState.Loading,
                    isButtonLoading = false,
                ),
            ),
        )
    }
}