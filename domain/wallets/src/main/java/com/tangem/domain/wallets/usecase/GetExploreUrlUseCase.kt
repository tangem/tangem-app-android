package com.tangem.domain.wallets.usecase

import arrow.core.raise.catch
import com.tangem.domain.tokens.model.Network
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

// TODO: Add tests
class GetExploreUrlUseCase(private val walletsManagersFacade: WalletManagersFacade) {

    // FIXME: Handle error
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        addressType: AddressType = AddressType.Default,
    ): String {
        return catch({ walletsManagersFacade.getExploreUrl(userWalletId, network, addressType) }) {
            ""
        }
    }
}