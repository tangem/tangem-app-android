package com.tangem.features.staking.impl.presentation.state.bottomsheet

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.staking.PendingAction

internal data class StakingActionSelectionBottomSheetConfig(
    val title: TextReference,
    val actions: List<PendingAction>,
    val onActionSelect: (PendingAction) -> Unit,
) : TangemBottomSheetConfigContent