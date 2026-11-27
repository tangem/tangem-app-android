package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampCountryByIpUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<OnrampError, OnrampCountry> {
        return Either.catch {
            Either.catch { repository.getCountryByIp(userWallet) }
                .getOrElse { error ->
                    repository.getCachedCountryByIp()?.takeUnless { it.isOnrampAvailable } ?: throw error
                }
        }.mapLeft(errorResolver::resolve)
    }
}