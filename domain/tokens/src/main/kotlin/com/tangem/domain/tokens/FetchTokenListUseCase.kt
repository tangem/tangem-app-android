package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Use case responsible for fetching token list information, including currency data,
 * network statuses, and quotes for tokens associated with a user's wallet.
 *
 * @param currenciesRepository The repository for retrieving currency-related data.
 */
class FetchTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
) {

    /**
     * Fetches the token list information for a user's wallet, including currency data,
     * network statuses, and quotes for associated tokens.
     *
     * @param userWalletId The ID of the user's wallet.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the token list.
     */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TokenListError, Unit> = either {
        val currencies = fetchCurrencies(userWalletId)

        invoke(userWalletId = userWalletId, currencies = currencies)
    }

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<TokenListError, Unit> = either {
        coroutineScope {
            val fetchStatuses = async {
                fetchNetworksStatuses(
                    userWalletId = userWalletId,
                    networks = currencies.mapTo(hashSetOf()) { it.network },
                )
            }
            val fetchQuotes = async {
                fetchQuotes(
                    currenciesIds = currencies.mapTo(hashSetOf()) { it.id },
                )
            }

            val yieldBalances = async {
                fetchYieldBalances(userWalletId = userWalletId, currencies = currencies)
            }

            awaitAll(fetchStatuses, fetchQuotes, yieldBalances)
        }
    }

    private suspend fun Raise<TokenListError>.fetchCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        val currencies = catch(
            block = { currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId, true) },
        ) {
            raise(TokenListError.DataError(it))
        }

        return ensureNotNull(currencies.toNonEmptyListOrNull()) {
            TokenListError.EmptyTokens
        }
    }

    private suspend fun Raise<TokenListError>.fetchNetworksStatuses(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ) {
        multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks),
        )
            .mapLeft(TokenListError::DataError)
            .bind()
    }

    private suspend fun Raise<TokenListError>.fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>) {
        multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet(),
                appCurrencyId = null,
            ),
        )
            .mapLeft(TokenListError::DataError)
            .bind()
    }

    private suspend fun fetchYieldBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        multiYieldBalanceFetcher(
            params = MultiYieldBalanceFetcher.Params(
                userWalletId = userWalletId,
                currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
            ),
        )
    }
}