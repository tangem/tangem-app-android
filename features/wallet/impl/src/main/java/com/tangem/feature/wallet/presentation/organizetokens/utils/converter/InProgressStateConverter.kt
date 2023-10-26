package com.tangem.feature.wallet.presentation.organizetokens.utils.converter

import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.utils.converter.TwoWayConverter

internal class InProgressStateConverter : TwoWayConverter<OrganizeTokensState, OrganizeTokensState> {

    override fun convert(value: OrganizeTokensState): OrganizeTokensState {
        return value.copy(
            actions = value.actions.copy(
                showApplyProgress = true,
            ),
            header = value.header.copy(
                isEnabled = false,
            ),
        )
    }

    override fun convertBack(value: OrganizeTokensState): OrganizeTokensState {
        return value.copy(
            actions = value.actions.copy(
                showApplyProgress = false,
            ),
            header = value.header.copy(
                isEnabled = true,
            ),
        )
    }
}
