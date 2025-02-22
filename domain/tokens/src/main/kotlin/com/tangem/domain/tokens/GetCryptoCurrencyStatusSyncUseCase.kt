package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.wallets.models.UserWalletId

class GetCryptoCurrencyStatusSyncUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    // multi-currency
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean = false,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return currencyStatusOperations.getCurrencyStatusSync(userWalletId, cryptoCurrencyId, isSingleWalletWithTokens)
            .mapLeft { error -> error.mapToCurrencyError() }
    }

    // single-currency
    suspend operator fun invoke(userWalletId: UserWalletId): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return currencyStatusOperations.getPrimaryCurrencyStatusSync(userWalletId)
            .mapLeft { error -> error.mapToCurrencyError() }
    }
}