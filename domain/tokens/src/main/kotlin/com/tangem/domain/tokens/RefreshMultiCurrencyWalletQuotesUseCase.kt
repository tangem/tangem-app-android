package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.tokens.error.QuotesError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class RefreshMultiCurrencyWalletQuotesUseCase(
    private val quotesRepository: QuotesRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<QuotesError, Unit> {
        return either {
            val currencies = getCurrencies(userWalletId = userWalletId)
                .getOrElse { raise(QuotesError.DataError(it)) }

            coroutineScope {
                val fetchQuotes = async {
                    fetchQuotes(
                        currenciesIds = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::id),
                    )
                }

                awaitAll(fetchQuotes)
            }
        }
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId): Either<Throwable, List<CryptoCurrency>> {
        return either {
            catch(
                block = { currenciesRepository.getMultiCurrencyWalletCachedCurrenciesSync(userWalletId) },
                catch = { raise(it) },
            )
        }
    }

    private suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>) {
        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            multiQuoteFetcher(
                params = MultiQuoteFetcher.Params(
                    currenciesIds = currenciesIds.mapNotNullTo(hashSetOf(), CryptoCurrency.ID::rawCurrencyId),
                    appCurrencyId = null,
                ),
            )
        } else {
            catch(
                block = {
                    quotesRepository.fetchQuotes(
                        currenciesIds = currenciesIds.mapNotNullTo(hashSetOf(), CryptoCurrency.ID::rawCurrencyId),
                        refresh = true,
                    )
                },
                catch = { /* Ignore error */ },
            )
        }
    }
}