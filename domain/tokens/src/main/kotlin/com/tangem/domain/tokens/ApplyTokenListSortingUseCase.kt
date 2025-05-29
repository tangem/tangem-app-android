package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.collections.set

class ApplyTokenListSortingUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sortedTokensIds: List<CryptoCurrency.ID>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokenListSortingError, Unit> {
        return either {
            val storedCurrencies = getCurrencies(userWalletId)
            val isSortingTypeChanged = checkIsCurrenciesSortedByBalance(userWalletId) != isSortedByBalance
            val isGroupingTypeChanged = checkIsCurrenciesGroupedByNetwork(userWalletId) != isGroupedByNetwork

            val sortedCurrencies = sortTokens(sortedTokensIds, storedCurrencies)

            if (storedCurrencies != sortedCurrencies || isSortingTypeChanged || isGroupingTypeChanged) {
                applySorting(
                    userWalletId = userWalletId,
                    currencies = sortedCurrencies,
                    isGrouped = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )
            }
        }
    }

    private suspend fun Raise<TokenListSortingError>.checkIsCurrenciesSortedByBalance(userWalletId: UserWalletId) =
        catch(
            block = { currenciesRepository.isTokensSortedByBalance(userWalletId).firstOrNull() ?: false },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )

    private suspend fun Raise<TokenListSortingError>.checkIsCurrenciesGroupedByNetwork(userWalletId: UserWalletId) =
        catch(
            block = { currenciesRepository.isTokensGrouped(userWalletId).firstOrNull() ?: false },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )

    private suspend fun Raise<TokenListSortingError>.sortTokens(
        sortedCurrenciesIds: List<CryptoCurrency.ID>,
        unsortedCurrencies: List<CryptoCurrency>,
    ): List<CryptoCurrency> = withContext(dispatchers.default) {
        val nonEmptySortedTokensIds = ensureNotNull(sortedCurrenciesIds.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }

        val sortedTokens = sortedMapOf<Int, CryptoCurrency>()

        unsortedCurrencies.distinct().forEach { currency ->
            val index = nonEmptySortedTokensIds.indexOfFirst { currencyId ->
                currencyId == currency.id
            }

            if (index >= 0) {
                sortedTokens[index] = currency
            } else {
                raise(TokenListSortingError.UnableToSortTokenList)
            }
        }

        ensureNotNull(sortedTokens.values.toNonEmptyListOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.getCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        val tokens = catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId, refresh = false)
            },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )

        return ensureNotNull(tokens.toNonEmptyListOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.applySorting(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ) = withContext(dispatchers.io) {
        catch(
            block = { currenciesRepository.saveTokens(userWalletId, currencies, isGrouped, isSortedByBalance) },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )
    }
}