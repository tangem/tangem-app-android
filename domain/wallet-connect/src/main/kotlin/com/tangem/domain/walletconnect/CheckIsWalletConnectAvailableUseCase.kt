package com.tangem.domain.walletconnect

import arrow.core.Either
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.domain.models.wallet.UserWalletId

class CheckIsWalletConnectAvailableUseCase(
    private val walletConnectRepository: WalletConnectRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Boolean> {
        return Either.catch { walletConnectRepository.checkIsAvailable(userWalletId) }
    }
}