package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

class DerivePublicKeysUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): Either<Throwable, Unit> {
        return Either.catch {
            derivationsRepository.derivePublicKeys(userWalletId, currencies)
        }
    }
}