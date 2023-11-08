package com.tangem.managetokens.presentation.managetokens.state

internal sealed class TokenItemState {

    abstract val id: String

    data class Loading(override val id: String) : TokenItemState()

    data class Loaded(
        override val id: String,
        val name: String,
        val currencyId: String,
        val tokenIcon: TokenIconState,
        val quotes: QuotesState,
        val rate: String?,
        val availableAction: TokenButtonType,
        val onButtonClick: (String) -> Unit,
    ) : TokenItemState()
}