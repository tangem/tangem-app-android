package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.settings.repositories.SettingsRepository

class GetOnrampPaymentMethodsUseCase(
    private val repository: OnrampRepository,
    private val settingsRepository: SettingsRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(): Either<OnrampError, Set<OnrampPaymentMethod>> {
        return Either.catch {
            val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()

            repository.getAvailablePaymentMethods()
                .toList()
                .sortedBy { it.type.getPriority(isGooglePayAvailable) }
                .toSet()
        }.mapLeft(errorResolver::resolve)
    }
}