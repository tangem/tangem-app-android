package com.tangem.features.onramp.hottokens.entity

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Hot crypto UM
 *
 * @property items items
 *
 * @author Andrew Khokhlov on 18/01/2025
 */
data class HotCryptoUM(
    val items: ImmutableList<TokensListItemUM>,
)
