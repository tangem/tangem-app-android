package com.tangem.features.markets.portfolio.impl.ui.state

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import kotlinx.collections.immutable.ImmutableList

internal data class SelectNetworkUM(
    val tokenId: String,
    val iconUrl: String?,
    val tokenName: String,
    val tokenCurrencySymbol: String,
    val networks: ImmutableList<BlockchainRowUM>,
    val onNetworkSwitchClick: (BlockchainRowUM, Boolean) -> Unit,
)