package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampCountryUseCase(private val repository: OnrampRepository) {

    operator fun invoke(): Flow<Either<Throwable, OnrampCountry?>> {
        return repository.getDefaultCountry()
            .map<OnrampCountry?, Either<Throwable, OnrampCountry?>> { it.right() }
            .catch { emit(it.left()) }
    }

    suspend fun invokeSync(): Either<Throwable, OnrampCountry> {
        return Either.catch { repository.getDefaultCountrySync() ?: repository.getCountryByIp() }
    }
}