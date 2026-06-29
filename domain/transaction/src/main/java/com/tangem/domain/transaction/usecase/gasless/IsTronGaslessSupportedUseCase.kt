package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.lib.crypto.BlockchainUtils.isTron

/**
 * Tells whether the Tron gasless flow is available for the given [network]/[currency].
 *
 * Checks ONLY domain facts: the currency is a token on a Tron network whose contract is in the
 * backend-supported token list. The `AND_16063_TRON_GASLESS_ENABLED` feature toggle is applied in the
 * feature layer (SendModel) — this use case must not depend on `features/send/api`.
 */
class IsTronGaslessSupportedUseCase(
    private val repository: TronGaslessTransactionRepository,
) {
    suspend operator fun invoke(network: Network, currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Token) return false
        if (!isTron(network.rawId)) return false
        val supported = runCatching { repository.getSupportedTokens() }.getOrDefault(emptyList())
        return supported.any { it.contractAddress == currency.contractAddress }
    }
}