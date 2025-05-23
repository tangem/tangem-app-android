package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class GetIsNotificationsEnabledUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Boolean> = Either.catch {
        walletsRepository.isNotificationsEnabled(
            userWalletId = userWalletId,
        )
    }
}