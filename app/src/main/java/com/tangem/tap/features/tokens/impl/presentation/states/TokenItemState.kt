package com.tangem.tap.features.tokens.impl.presentation.states

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * Token item state.
 * All subclasses is stable, but @Immutable annotation is required to use this sealed class like as
 * field of TokensListStateHolder.
 *
* [REDACTED_AUTHOR]
 */
@Immutable
sealed interface TokenItemState {

    /** Token id */
    val id: String

    /** Token full name (name with symbol) */
    val fullName: String

    /** Token icon url */
    val iconUrl: String

    /** List of networks */
    val networks: ImmutableList<NetworkItemState>

    /** Token composed id that unique for tokens in different networks  */
    val composedId: String

    /**
     * Token item state that is available for read
     *
     * @property id       token id
     * @property fullName token name
     * @property iconUrl  token icon url
     * @property networks list of networks that is available for read
     * @property composedId token composed id to use in lists
     */
    data class ReadContent(
        override val id: String,
        override val fullName: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ReadContent>,
        override val composedId: String,
    ) : TokenItemState

    /**
     * Token item state that is available for read and manage
     *
     * @property id       token id
     * @property fullName     token name
     * @property iconUrl  token icon url
     * @property networks list of networks is available for read and edit
     * @property composedId token composed id to use in lists
     * @property name     token name
     * @property symbol   token brief name
     */
    data class ManageContent(
        override val id: String,
        override val fullName: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ManageContent>,
        override val composedId: String,
        val name: String,
        val symbol: String,
    ) : TokenItemState
}
