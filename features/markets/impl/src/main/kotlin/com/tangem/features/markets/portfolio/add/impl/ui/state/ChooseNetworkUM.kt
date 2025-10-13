package com.tangem.features.markets.portfolio.add.impl.ui.state

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import kotlinx.collections.immutable.ImmutableList

data class ChooseNetworkUM(
    val networks: ImmutableList<BlockchainRowUM>,
    val onNetworkClick: (BlockchainRowUM) -> Unit,
)