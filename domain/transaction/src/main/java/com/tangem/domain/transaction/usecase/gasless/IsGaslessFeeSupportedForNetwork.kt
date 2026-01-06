package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.GaslessTransactionRepository

/**
 * Use case to check if gasless fee is supported for a given network.
 */
class IsGaslessFeeSupportedForNetwork(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
) {

    operator fun invoke(network: Network): Boolean {
        return gaslessTransactionRepository.isNetworkSupported(network)
    }
}