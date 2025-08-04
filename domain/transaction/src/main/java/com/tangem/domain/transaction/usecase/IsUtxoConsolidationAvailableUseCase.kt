package com.tangem.domain.transaction.usecase

import com.tangem.domain.models.network.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Gets UTXO consolidation availability
 */
class IsUtxoConsolidationAvailableUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend fun invokeSync(userWalletId: UserWalletId, network: Network) =
        walletManagersFacade.checkUtxoConsolidationAvailability(
            userWalletId = userWalletId,
            network = network,
        )
}