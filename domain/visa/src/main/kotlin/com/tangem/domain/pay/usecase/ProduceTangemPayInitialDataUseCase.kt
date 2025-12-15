package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository

class ProduceTangemPayInitialDataUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return catch {
            repository.produceInitialData(userWalletId)
        }
    }
}