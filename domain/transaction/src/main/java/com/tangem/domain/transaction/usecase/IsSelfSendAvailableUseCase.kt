package com.tangem.domain.transaction.usecase

import com.tangem.domain.models.network.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Gets self send availability
 */
class IsSelfSendAvailableUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend fun invokeSync(userWalletId: UserWalletId, network: Network) =
        walletManagersFacade.checkSelfSendAvailability(
            userWalletId = userWalletId,
            network = network,
        )
}