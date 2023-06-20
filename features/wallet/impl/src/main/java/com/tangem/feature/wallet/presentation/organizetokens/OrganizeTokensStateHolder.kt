package com.tangem.feature.wallet.presentation.organizetokens

import com.tangem.feature.wallet.presentation.common.state.TokenListState

internal data class OrganizeTokensStateHolder(
    val tokens: TokenListState,
    val header: HeaderConfig,
    val actions: ActionsConfig,
) {

    data class HeaderConfig(
        val onSortByBalanceClick: () -> Unit,
        val onGroupByNetworkClick: () -> Unit,
    )

    data class ActionsConfig(
        val onApplyClick: () -> Unit,
        val onCancelClick: () -> Unit,
    )
}