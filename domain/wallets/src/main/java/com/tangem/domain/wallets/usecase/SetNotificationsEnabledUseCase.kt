package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class SetNotificationsEnabledUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, isEnabled: Boolean): Either<Throwable, Unit> =
        Either.catch {
            walletsRepository.setNotificationsEnabled(
                userWalletId = userWalletId,
                isEnabled = isEnabled,
            )
        }
}