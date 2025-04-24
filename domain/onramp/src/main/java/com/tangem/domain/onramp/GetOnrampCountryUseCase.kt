package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampCountryUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): Flow<Either<OnrampError, OnrampCountry?>> {
        return repository.getDefaultCountry()
            .map<OnrampCountry?, Either<OnrampError, OnrampCountry?>> { it.right() }
            .catch {
                emit(errorResolver.resolve(it).left())
            }
    }

    suspend fun invokeSync(userWallet: UserWallet): Either<OnrampError, OnrampCountry> {
        return Either.catch { repository.getDefaultCountrySync() ?: repository.getCountryByIp(userWallet) }
            .mapLeft(errorResolver::resolve)
    }
}