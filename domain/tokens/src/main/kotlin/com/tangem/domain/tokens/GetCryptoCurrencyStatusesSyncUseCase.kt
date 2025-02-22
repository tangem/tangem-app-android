package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.mapper.mapToTokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.wallets.models.UserWalletId

class GetCryptoCurrencyStatusesSyncUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<TokenListError, List<CryptoCurrencyStatus>> {
        return currencyStatusOperations.getCurrenciesStatusesSync(userWalletId)
            .mapLeft { error -> error.mapToTokenListError() }
    }
}