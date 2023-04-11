package com.tangem.tap.features.tokens.presentation.states

import kotlinx.collections.immutable.ImmutableList

/**
 * Network item state
 *
[REDACTED_AUTHOR]
 */
sealed interface TokenItemState {

    /** Token name */
    val name: String

    /** Token icon url */
    val iconUrl: String

    /** List of networks */
    val networks: ImmutableList<NetworkItemState>

    /**
     * Token item state that is available for read
     *
     * @property name     token name
     * @property iconUrl  token icon url
     * @property networks list of networks that is available for read
     */
    data class ReadAccess(
        override val name: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ReadAccess>,
    ) : TokenItemState

    /**
     * Token item state that is available for read and edit
     *
     * @property name     token name
     * @property iconUrl  token icon url
     * @property networks list of networks is available for read and edit
     * @property id       token id
     */
    data class ManageAccess(
        override val name: String,
        override val iconUrl: String,
        override val networks: ImmutableList<NetworkItemState.ManageAccess>,
        val id: String,
    ) : TokenItemState
}