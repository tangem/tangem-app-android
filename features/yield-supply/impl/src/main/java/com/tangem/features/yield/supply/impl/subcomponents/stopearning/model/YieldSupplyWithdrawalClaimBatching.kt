package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import com.tangem.domain.models.network.Network
import com.tangem.lib.crypto.BlockchainUtils

internal object YieldSupplyWithdrawalClaimBatching {

    fun isSupported(network: Network): Boolean = !BlockchainUtils.isAvalanche(network.rawId)
}