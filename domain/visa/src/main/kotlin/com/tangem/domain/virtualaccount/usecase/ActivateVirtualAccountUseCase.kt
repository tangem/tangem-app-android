package com.tangem.domain.virtualaccount.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.virtualaccount.repository.VirtualAccountActivationRepository

class ActivateVirtualAccountUseCase(
    private val repository: VirtualAccountActivationRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return catch {
            repository.activateVirtualAccount(userWalletId)
        }
    }
}