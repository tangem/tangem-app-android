package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingActionSelectionBottomSheetConfig
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class ShowActionSelectorBottomSheetTransformer(
    private val pendingActions: ImmutableList<PendingAction>,
    private val onActionSelect: (PendingAction) -> Unit,
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                content = StakingActionSelectionBottomSheetConfig(
                    title = resourceReference(R.string.common_choose_action),
                    actions = pendingActions,
                    onActionSelect = onActionSelect,
                ),
                onDismissRequest = onDismiss,
            ),
        )
    }
}