package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.mapper.mapToCurrencyError
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

/**
 * Use case for fetching the status of a cryptocurrency associated with a user wallet.
 *
 */
class GetSingleCryptoCurrencyStatusUseCase(
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Returns cryptocurrency status flow for Multi-Currency wallet
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param currencyId The unique identifier of the cryptocurrency.
     * @param isSingleWalletWithTokens Indicates whether the user wallet contains only one token on card (old cards)
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    fun invokeMultiWallet(
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

    /**
     * Returns cryptocurrency status flow for primary currency for Single-Currency wallet
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    fun invokeSingleWallet(userWalletId: UserWalletId): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return flow {
            emitAll(
                currencyStatusOperations.getPrimaryCurrencyStatusFlow(userWalletId).map { maybeCurrency ->
                    maybeCurrency.mapLeft(CurrenciesStatusesOperations.Error::mapToCurrencyError)
                },
            )
        }.flowOn(dispatchers.io)
    }

    /**
     * Returns synchronously cryptocurrency status for Multi-Currency wallet
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param cryptoCurrencyId The unique identifier of the cryptocurrency.
     * @param isSingleWalletWithTokens Indicates whether the user wallet contains only one token on card (old cards)
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    suspend fun invokeMultiWalletSync(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean = false,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return currencyStatusOperations.getCurrencyStatusSync(userWalletId, cryptoCurrencyId, isSingleWalletWithTokens)
            .mapLeft { error -> error.mapToCurrencyError() }
    }

    /**
     * Returns synchronously cryptocurrency status for primary currency for Single-Currency wallet
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @return A [Flow] emitting either a [CurrencyStatusError] or a [CryptoCurrencyStatus], indicating the result of the fetch operation.
     */
    suspend fun invokeSingleWalletSync(userWalletId: UserWalletId): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return currencyStatusOperations.getPrimaryCurrencyStatusSync(userWalletId)
            .mapLeft { error -> error.mapToCurrencyError() }
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