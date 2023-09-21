package com.tangem.domain.wallets.usecase

import arrow.core.raise.catch
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
// [REDACTED_TODO_COMMENT]
class GetExploreUrlUseCase(private val walletsManagersFacade: WalletManagersFacade) {
// [REDACTED_TODO_COMMENT]
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): String {
        return catch({ walletsManagersFacade.getExploreUrl(userWalletId, network) }) {
            ""
        }
    }
}
