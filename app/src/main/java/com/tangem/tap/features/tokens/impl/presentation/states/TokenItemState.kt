package com.tangem.tap.features.tokens.impl.presentation.states

import kotlinx.collections.immutable.ImmutableList

/**
 * Network item state
 *
[REDACTED_AUTHOR]
 */
sealed interface TokenItemState {

    /** Token full name (name with symbol) */
    val fullName: String

    /** Token icon url */
    val iconUrl: String

    /** List of networks */
    val networks: ImmutableList<NetworkItemState>

    /**
     * Token item state that is available for read
     *
     * @property fullName     token name
     * @property iconUrl  token icon url
     * @property networks list of networks that is available for read
     */
    data class ReadContent(
        override val fullName: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ReadContent>,
    ) : TokenItemState

    /**
     * Token item state that is available for read and manage
     *
     * @property fullName     token name
     * @property iconUrl  token icon url
     * @property networks list of networks is available for read and edit
     * @property id       token id
     * @property name     token name
     * @property symbol   token brief name
     */
    data class ManageContent(
        override val fullName: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ManageContent>,
        val id: String,
        val name: String,
        val symbol: String,
    ) : TokenItemState
}