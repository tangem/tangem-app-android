package com.tangem.domain.visa

import arrow.core.Either
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.models.wallet.UserWalletId

class GetVisaCurrencyUseCase(
    private val repository: VisaRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        isRefresh: Boolean = false,
    ): Either<Throwable, VisaCurrency> {
        return Either.catch { repository.getVisaCurrency(userWalletId, isRefresh) }
    }
}