package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveSelectedPaymentMethod(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(paymentMethod: OnrampPaymentMethod): Either<OnrampError, Unit> {
        return Either.catch { repository.saveSelectedPaymentMethod(paymentMethod) }
            .mapLeft(errorResolver::resolve)
    }
}