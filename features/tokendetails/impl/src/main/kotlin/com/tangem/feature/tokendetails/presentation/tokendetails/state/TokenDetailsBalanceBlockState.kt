package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import kotlinx.collections.immutable.ImmutableList

internal sealed class TokenDetailsBalanceBlockState {

    abstract val actionButtons: ImmutableList<TokenDetailsActionButton>

    data class Loading(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
    ) : TokenDetailsBalanceBlockState()

    data class Content(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
        val fiatBalance: String,
        val cryptoBalance: String,
    ) : TokenDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
    ) : TokenDetailsBalanceBlockState()

    fun copyActionButtons(buttons: ImmutableList<TokenDetailsActionButton>): TokenDetailsBalanceBlockState {
        return when (this) {
            is Content -> this.copy(actionButtons = buttons)
            is Error -> this.copy(actionButtons = buttons)
            is Loading -> this.copy(actionButtons = buttons)
        }
    }
}