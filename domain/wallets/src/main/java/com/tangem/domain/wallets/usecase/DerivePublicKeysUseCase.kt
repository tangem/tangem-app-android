
package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository

class DerivePublicKeysUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): Either<Throwable, Unit> {
        return Either.Companion.catch {
            derivationsRepository.derivePublicKeys(userWalletId, currencies)
        }
    }
}