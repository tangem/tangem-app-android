package com.tangem.tap.features.tokens.presentation.states

/** Marker interface for states that have a list of tokens */
sealed interface TokensListVisibility {

    /** Tokens list */
    val tokens: List<TokenItemModel>
}
