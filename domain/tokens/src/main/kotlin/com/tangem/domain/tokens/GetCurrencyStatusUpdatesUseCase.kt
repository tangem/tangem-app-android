package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

/**
 * Use case for fetching the status of a cryptocurrency associated with a user wallet.
 *
 */
class GetCurrencyStatusUpdatesUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Invokes the use case.
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param currencyId The unique identifier of the cryptocurrency.
     * @param isSingleWalletWithTokens Indicates whether the user wallet contains only one token on card (old cards)
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(
                getCurrencyStatus(
                    userWalletId = userWalletId,
                    currencyId = currencyId,
                    isSingleWalletWithTokens = isSingleWalletWithTokens,
                ),
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getCurrencyStatus(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        val currencyFlow = currencyStatusOperations.getCurrencyStatusFlow(
            userWalletId = userWalletId,
            currencyId = currencyId,
            isSingleWalletWithTokens = isSingleWalletWithTokens,
        )

        return currencyFlow.map { maybeCurrency ->
            maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
        }
    }
}