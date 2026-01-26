package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.repository.CurrencyChecksRepository

/**
 * Use case to check if gasless fee is supported for a given network.
 */
class IsGaslessFeeSupportedForNetwork(
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    operator fun invoke(network: Network): Boolean {
        return currencyChecksRepository.isNetworkSupportedForGaslessTx(network)
    }
}