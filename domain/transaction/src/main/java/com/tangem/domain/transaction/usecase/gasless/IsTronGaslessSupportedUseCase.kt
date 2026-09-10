package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Tells whether the Tron gasless flow is available for the given [network]/[currency].
 *
 * Checks ONLY domain facts. The `TWI_1259_TRON_GASLESS_ENABLED` feature toggle is applied in the
 * feature layer (SendModel) — this use case must not depend on `features/send/api`.
 */
class IsTronGaslessSupportedUseCase(
    private val repository: TronGaslessTransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, currency: CryptoCurrency): Boolean {
        if (!isTokenSupported(network = network, currency = currency)) return false
        return walletManagersFacade.isTronAccountActivated(userWalletId = userWalletId, network = network)
    }

    suspend fun isTokenSupported(network: Network, currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Token) return false
        if (!isTron(network.rawId)) return false
        val supported = runSuspendCatching { repository.getSupportedTokens() }.getOrDefault(emptyList())
        return supported.any { it.contractAddress == currency.contractAddress }
    }
}