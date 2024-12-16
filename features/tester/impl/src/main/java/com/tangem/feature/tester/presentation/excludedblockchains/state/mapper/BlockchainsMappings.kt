package com.tangem.feature.tester.presentation.excludedblockchains.state.mapper

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.feature.tester.presentation.excludedblockchains.state.BlockchainUM

internal fun List<Blockchain>.toUiModels(
    excludedBlockchainsIds: Set<String>,
    onExcludedStateChange: (Blockchain, Boolean) -> Unit,
): List<BlockchainUM> = mapNotNull { blockchain ->
    blockchain
        .takeUnless { it == Blockchain.Unknown || it.isTestnet() }
        ?.toUiModel(
            isExcluded = blockchain.id in excludedBlockchainsIds,
            onExcludedStateChange = { isExcluded ->
                onExcludedStateChange(blockchain, isExcluded)
            },
        )
}
    .sortedBy(BlockchainUM::name)
    .sortedByDescending(BlockchainUM::isExcluded)

private fun Blockchain.toUiModel(isExcluded: Boolean, onExcludedStateChange: (Boolean) -> Unit): BlockchainUM =
    BlockchainUM(
        id = id,
        name = name,
        symbol = currency,
        iconResId = getActiveIconRes(id),
        isExcluded = isExcluded,
        onExcludedStateChange = onExcludedStateChange,
    )