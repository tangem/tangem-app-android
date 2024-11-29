package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampSelectedPaymentMethodUseCase(private val repository: OnrampRepository) {

    operator fun invoke(): EitherFlow<Throwable, OnrampPaymentMethod> {
        return repository.getSelectedPaymentMethod()
            .map<OnrampPaymentMethod, Either<Throwable, OnrampPaymentMethod>> { it.right() }
            .catch { emit(it.left()) }
    }
}