package com.tangem.features.feed.earn.model

import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnNetworks

internal fun createEarnTokensListConfig(
    filter: EarnFilter?,
    earnNetworks: EarnNetworks?,
    isForEarn: Boolean = false,
): EarnTokensListConfig {
    val type = when (filter?.earnFilterType) {
        null, EarnFilterType.ALL -> null
        EarnFilterType.STAKING -> "staking"
        EarnFilterType.YIELD -> "yield"
    }
    val networks = when (val network = filter?.earnFilterNetwork) {
        null, is EarnFilterNetwork.AllNetworks -> null
        is EarnFilterNetwork.MyNetworks -> when (earnNetworks) {
            null -> listOf(NO_ONE_NETWORK)
            else -> earnNetworks.fold(
                ifLeft = { null },
                ifRight = { networks ->
                    networks.filter(EarnNetwork::isAdded)
                        .map(EarnNetwork::networkId)
                        .ifEmpty { listOf(NO_ONE_NETWORK) }
                },
            )
        }
        is EarnFilterNetwork.Specific -> listOf(network.id)
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