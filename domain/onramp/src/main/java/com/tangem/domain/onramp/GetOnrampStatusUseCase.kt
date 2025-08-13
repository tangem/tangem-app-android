package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.models.wallet.UserWallet

class GetOnrampStatusUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet, txId: String): Either<OnrampError, OnrampStatus> {
        return Either.catch {
            onrampRepository.getStatus(userWallet = userWallet, txId = txId)
        }.mapLeft(errorResolver::resolve)
    }
}