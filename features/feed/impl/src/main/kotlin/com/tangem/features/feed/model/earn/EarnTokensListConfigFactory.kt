package com.tangem.features.feed.model.earn

import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM

internal fun createEarnTokensListConfig(
    selectedTypeFilter: EarnFilterTypeUM,
    selectedNetworkFilter: EarnFilterNetworkUM,
    earnNetworks: EarnNetworks,
    isForEarn: Boolean = false,
): EarnTokensListConfig {
    val type = when (selectedTypeFilter) {
        EarnFilterTypeUM.All -> null
        EarnFilterTypeUM.Staking -> "staking"
        EarnFilterTypeUM.YieldMode -> "yield"
    }
    val networks = when (selectedNetworkFilter) {
        is EarnFilterNetworkUM.AllNetworks -> null
        is EarnFilterNetworkUM.MyNetworks -> {
            earnNetworks.fold(
                ifLeft = { null },
                ifRight = { networks ->
                    networks.filter(EarnNetwork::isAdded)
                        .map(EarnNetwork::networkId)
                        .ifEmpty { listOf(NO_ONE_NETWORK) }
                },
            )
        }
        is EarnFilterNetworkUM.Network -> listOf(selectedNetworkFilter.id)
    }
    return EarnTokensListConfig(
        type = type,
        networks = networks,
        isForEarn = isForEarn,
    )
}

/**
 * This id means that backend has to return empty result
 */
private const val NO_ONE_NETWORK = "-1"