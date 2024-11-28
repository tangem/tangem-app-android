package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveSelectedPaymentMethod(private val repository: OnrampRepository) {

    suspend operator fun invoke(paymentMethod: OnrampPaymentMethod): Either<Throwable, Unit> {
        return Either.catch { repository.saveSelectedPaymentMethod(paymentMethod) }
    }
}