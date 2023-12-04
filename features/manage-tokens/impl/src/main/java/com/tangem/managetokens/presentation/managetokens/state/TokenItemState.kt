package com.tangem.managetokens.presentation.managetokens.state

import androidx.compose.runtime.MutableState

internal sealed class TokenItemState {

    abstract val id: String

    data class Loading(override val id: String) : TokenItemState()

    data class Loaded(
        override val id: String,
        val name: String,
        val currencySymbol: String,
        val tokenId: String,
        val tokenIcon: TokenIconState,
        val quotes: QuotesState,
        val rate: String?,
        val availableAction: MutableState<TokenButtonType>,
        val chooseNetworkState: ChooseNetworkState,
        val onButtonClick: (Loaded) -> Unit,
    ) : TokenItemState()
}