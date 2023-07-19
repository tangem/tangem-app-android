package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import kotlinx.collections.immutable.ImmutableList

sealed class TokenDetailsBalanceBlockState(open val actionButtons: ImmutableList<ActionButtonConfig>) {

    data class Loading(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
    ) : TokenDetailsBalanceBlockState(actionButtons = actionButtons)

    data class Content(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        val fiatBalance: String,
        val cryptoBalance: String,
    ) : TokenDetailsBalanceBlockState(actionButtons = actionButtons)

    data class Error(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
    ) : TokenDetailsBalanceBlockState(actionButtons = actionButtons)
}
