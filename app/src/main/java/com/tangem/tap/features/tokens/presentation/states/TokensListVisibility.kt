package com.tangem.tap.features.tokens.presentation.states

import kotlinx.collections.immutable.ImmutableList

/** Marker interface for states that have a list of tokens */
sealed interface TokensListVisibility {

    /** Tokens list */
    val tokens: ImmutableList<TokenItemState>
}