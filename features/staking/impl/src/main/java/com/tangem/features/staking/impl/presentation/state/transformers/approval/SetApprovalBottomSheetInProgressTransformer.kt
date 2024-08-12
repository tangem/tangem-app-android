package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetApprovalBottomSheetInProgressTransformer(
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val approvalBottomSheetConfig = prevState.bottomSheetConfig?.content as? GiveTxPermissionBottomSheetConfig
        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig?.copy(
                onDismissRequest = onDismiss,
                isShow = true,
                content = approvalBottomSheetConfig?.let { config ->
                    config.copy(
                        data = config.data.copy(
                            approveButton = config.data.approveButton.copy(
                                enabled = false,
                                loading = true,
                            ),
                            cancelButton = config.data.cancelButton.copy(
                                enabled = false,
                            ),
                        ),
                        onCancel = onDismiss,
                    )
                } as TangemBottomSheetConfigContent,
            ),
        )
    }
}
