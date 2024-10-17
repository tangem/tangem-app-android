package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetApprovalBottomSheetTypeChangeTransformer(
    private val approveType: ApproveType,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val approvalBottomSheetConfig = prevState.bottomSheetConfig?.content as? GiveTxPermissionBottomSheetConfig

        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig?.copy(
                content = approvalBottomSheetConfig?.copy(
                    data = approvalBottomSheetConfig.data.copy(approveType = approveType),
                ) as TangemBottomSheetConfigContent,
            ),
        )
    }
}