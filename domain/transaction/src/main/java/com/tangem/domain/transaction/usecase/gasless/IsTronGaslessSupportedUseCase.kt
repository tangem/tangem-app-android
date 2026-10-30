package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Tells whether the Tron gasless flow is available for the given [network]/[currency].
 *
 * The gasless service sponsors the activation of a non-activated account and decides its eligibility when
 * quoting (see [GetTronGaslessFeeUseCase]), so the account state is not checked here.
 */
class IsTronGaslessSupportedUseCase(
    private val repository: TronGaslessTransactionRepository,
) {

    suspend operator fun invoke(network: Network, currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Token) return false
        if (!isTron(network.rawId)) return false
        val supported = runSuspendCatching { repository.getSupportedTokens() }.getOrDefault(emptyList())
        return supported.any { it.contractAddress == currency.contractAddress }
    }
}