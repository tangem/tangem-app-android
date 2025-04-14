package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

class CheckAccountInitializedUseCase(private val walletManagersFacade: WalletManagersFacade) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Boolean> {
        return Either.catch { walletManagersFacade.isAccountInitialized(userWalletId, network) }
    }
}