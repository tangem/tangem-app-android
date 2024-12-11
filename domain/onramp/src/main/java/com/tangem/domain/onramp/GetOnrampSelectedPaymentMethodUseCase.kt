package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

class GetOnrampSelectedPaymentMethodUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): EitherFlow<OnrampError, OnrampPaymentMethod> {
        return repository.getSelectedPaymentMethod()
            .map<OnrampPaymentMethod, Either<OnrampError, OnrampPaymentMethod>> { it.right() }
            .retryWhen { cause, _ ->
                emit(errorResolver.resolve(cause).left())
                true
            }
    }
}