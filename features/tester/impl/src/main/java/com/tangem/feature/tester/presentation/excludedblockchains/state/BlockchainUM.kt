package com.tangem.feature.tester.presentation.excludedblockchains.state

internal data class BlockchainUM(
    val id: String,
    val name: String,
    val symbol: String,
    val iconResId: Int,
    val isExcluded: Boolean,
    val onExcludedStateChange: (isExcluded: Boolean) -> Unit,
)