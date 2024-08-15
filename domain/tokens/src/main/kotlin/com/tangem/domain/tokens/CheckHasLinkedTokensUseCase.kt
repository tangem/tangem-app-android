package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class CheckHasLinkedTokensUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Boolean> {
        return Either.catch {
            currenciesRepository.hasTokens(userWalletId, network)
        }
    }
}
