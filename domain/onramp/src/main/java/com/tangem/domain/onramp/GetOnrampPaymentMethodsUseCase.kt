package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampPaymentMethodsUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(): Either<Throwable, List<OnrampPaymentMethod>> {
        return Either.catch { repository.getPaymentMethods() }
    }
}