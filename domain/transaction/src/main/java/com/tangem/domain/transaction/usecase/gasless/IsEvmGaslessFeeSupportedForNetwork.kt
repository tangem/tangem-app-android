package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.repository.CurrencyChecksRepository

/**
 * Gates the fee-token selector and the gasless notifications with the same predicate the fee path uses,
 * so the selector cannot offer a fee token on a network [GetFeeForTokenUseCase] then rejects.
 *
 * Tron has its own gate — [IsTronGaslessSupportedUseCase].
 */
class IsEvmGaslessFeeSupportedForNetwork(
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    operator fun invoke(network: Network): Boolean = currencyChecksRepository.isNetworkSupportedForGaslessTx(network)
}