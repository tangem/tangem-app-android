package com.tangem.domain.yield.supply.usecase

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository

/**
 * Wraps a yield-swap call data with a yield-module upgrade transaction when the user's deployed
 * yield-module contract version is out of date.
 */
class WrapYieldSwapCallDataWithUpgradeUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        callData: SmartContractCallData,
    ): SmartContractCallData = yieldSupplyTransactionRepository.wrapYieldSwapCallDataWithUpgradeIfNeeded(
        userWalletId = userWalletId,
        network = network,
        callData = callData,
    )
}