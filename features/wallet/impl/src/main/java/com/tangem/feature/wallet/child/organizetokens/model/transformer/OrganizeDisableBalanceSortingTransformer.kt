package com.tangem.feature.wallet.child.organizetokens.model.transformer

import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.utils.transformer.Transformer

internal object OrganizeDisableBalanceSortingTransformer : Transformer<OrganizeTokensUM> {
    override fun transform(prevState: OrganizeTokensUM): OrganizeTokensUM {
        return prevState.copy(
            organizeMenuUM = prevState.organizeMenuUM.copy(
                isSortedByBalance = false,
            ),
        )
    }
}