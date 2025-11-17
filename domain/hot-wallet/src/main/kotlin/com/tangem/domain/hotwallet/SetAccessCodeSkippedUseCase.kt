package com.tangem.domain.hotwallet

import arrow.core.Either
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId

class SetAccessCodeSkippedUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, skipped: Boolean): Either<Throwable, Unit> = Either.catch {
        hotWalletRepository.setAccessCodeSkipped(userWalletId, skipped)
    }
}