package com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import kotlinx.collections.immutable.ImmutableList

data class ChooseNetworkUM(
    val networks: ImmutableList<BlockchainRowUM>,
    val onNetworkClick: (BlockchainRowUM) -> Unit,
)