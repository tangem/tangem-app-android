package com.tangem.domain.wallets.usecase

import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

class GetExploreUrlUseCase(private val walletsManagersFacade: WalletManagersFacade) {

    suspend operator fun invoke(userWalletId: UserWalletId, networkId: Network.ID): String {
        return walletsManagersFacade.getExploreUrl(userWalletId, networkId)
    }
}