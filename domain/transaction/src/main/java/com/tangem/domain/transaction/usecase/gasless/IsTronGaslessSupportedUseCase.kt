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
 * Checks ONLY domain facts: the currency is a token on a Tron network whose contract is in the
 * backend-supported token list. The `TWI_1259_TRON_GASLESS_ENABLED` feature toggle is applied in the
 * feature layer (SendModel) — this use case must not depend on `features/send/api`.
 */
class IsTronGaslessSupportedUseCase(
    private val repository: TronGaslessTransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Token) return false
        if (!isTron(network.rawId)) return false
        val supported = runSuspendCatching { repository.getSupportedTokens() }.getOrDefault(emptyList())
        if (supported.none { it.contractAddress == currency.contractAddress }) return false
        return walletManagersFacade.isTronAccountActivated(userWalletId = userWalletId, network = network)
    }
}