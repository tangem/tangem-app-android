package com.tangem.feature.wallet.child.organizetokens.model.transformer

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
                isEnabled = !isSortingInProgress,
            ),
            applyButton = prevState.applyButton.copy(
                isLoading = isSortingInProgress,
            ),
        )
    }
}