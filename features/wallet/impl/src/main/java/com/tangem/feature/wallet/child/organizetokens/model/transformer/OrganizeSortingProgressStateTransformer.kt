package com.tangem.feature.wallet.child.organizetokens.model.transformer

import com.tangem.core.ui.ds.button.TangemButtonState
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.utils.transformer.Transformer

internal class OrganizeSortingProgressStateTransformer(
    private val isSortingInProgress: Boolean,
) : Transformer<OrganizeTokensUM> {
    override fun transform(prevState: OrganizeTokensUM): OrganizeTokensUM {
        return prevState.copy(
            organizeMenuUM = prevState.organizeMenuUM.copy(
                isEnabled = !isSortingInProgress,
            ),
            cancelButton = prevState.cancelButton.copy(
                state = if (isSortingInProgress) {
                    TangemButtonState.Disabled
                } else {
                    TangemButtonState.Default
                },
            ),
            applyButton = prevState.applyButton.copy(
                state = if (isSortingInProgress) {
                    TangemButtonState.Loading
                } else {
                    TangemButtonState.Default
                },
            ),
        )
    }
}