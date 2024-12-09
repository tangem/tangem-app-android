package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampCurrencyUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): Flow<Either<OnrampError, OnrampCurrency?>> {
        return repository.getDefaultCurrency()
            .map<OnrampCurrency?, Either<OnrampError, OnrampCurrency?>> { it.right() }
            .catch {
                emit(errorResolver.resolve(it).left())
            }
    }
}
