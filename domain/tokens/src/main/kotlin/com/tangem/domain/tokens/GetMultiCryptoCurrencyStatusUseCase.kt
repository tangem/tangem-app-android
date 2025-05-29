package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.mapper.mapToTokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetMultiCryptoCurrencyStatusUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    /**
     * Returns synchronously list of cryptocurrency statuses for Multi-Currency wallet
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    suspend fun invokeMultiWalletSync(userWalletId: UserWalletId): Either<TokenListError, List<CryptoCurrencyStatus>> {
        return currencyStatusOperations.getCurrenciesStatusesSync(userWalletId)
            .mapLeft { error -> error.mapToTokenListError() }
    }
}