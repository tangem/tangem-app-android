package com.tangem.domain.visa

import arrow.core.Either
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.models.wallet.UserWalletId

class GetVisaTxDetailsUseCase(
    private val visaRepository: VisaRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, transactionId: String): Either<Throwable, VisaTxDetails> {
        return Either.catch { visaRepository.getTxDetails(userWalletId, transactionId) }
    }
}